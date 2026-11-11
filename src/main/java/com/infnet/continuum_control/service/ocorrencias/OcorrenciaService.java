package com.infnet.continuum_control.service.ocorrencias;

import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.entities.MensagemOutbox;
import com.infnet.continuum_control.model.entities.Missao;
import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import com.infnet.continuum_control.model.enums.TipoRegistro;
import com.infnet.continuum_control.model.ocorrencias.Ocorrencia;
import com.infnet.continuum_control.repository.AgenteRepository;
import com.infnet.continuum_control.repository.MissaoRepository;
import com.infnet.continuum_control.repository.OutboxRepository;
import com.infnet.continuum_control.security.PosseDeMissao;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.HistoricoService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ocorrências (§11). O registro segue a operação 6.6 do doc 03: a ocorrência e o histórico entram
 * na mesma transação do PostgreSQL (tabela outbox), e o entregador a leva ao MongoDB depois do
 * commit (doc 04, D40). Assim nenhuma das duas gravações se perde sem a outra (§21).
 */
@Service
@RequiredArgsConstructor
public class OcorrenciaService {

    private final MissaoRepository missaoRepository;
    private final AgenteRepository agenteRepository;
    private final OutboxRepository outboxRepository;
    private final MongoTemplate mongoTemplate;
    private final PosseDeMissao posse;
    private final HistoricoService historico;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    @Transactional
    public Ocorrencia registrar(Long missaoId, Ocorrencia ocorrencia, UsuarioAutenticado usuario) {
        // 1. Missão em execução e agente do usuário designado nela (S3 antes de M9)
        Missao missao = buscarMissao(missaoId);
        posse.exigir(missaoId, usuario);
        Agente agente = agenteRepository.getReferenceById(usuario.agenteId());
        missao.conferirRegistroDeOcorrencia(agente);

        // 2. Identificador gerado na aplicação, para o histórico já poder citá-lo
        String id = new ObjectId().toHexString();
        ocorrencia.preencher(id, missaoId, usuario.agenteId(), LocalDateTime.now(clock));

        // 3. Na mesma transação: histórico e ocorrência completa no outbox
        historico.registrar(missao.getAnomalia(), missao, TipoRegistro.OCORRENCIA,
                "Ocorrência " + ocorrencia.getTipo() + " registrada por " + agente.getCodigo(), null, id, usuario);
        outboxRepository.save(new MensagemOutbox(MensagemOutbox.DESTINO_OCORRENCIAS, id,
                jsonMapper.writeValueAsString(ocorrencia), LocalDateTime.now(clock)));

        // 4. Depois do commit, o entregador grava no MongoDB (EntregadorOutbox)
        eventos.publishEvent(new OcorrenciaRegistrada(id));
        return ocorrencia;
    }

    /** Lê do MongoDB. Enquanto ele estiver fora do ar, as novas só aparecem depois de entregues (doc 04, 6.4). */
    @Transactional(readOnly = true)
    public Page<Ocorrencia> listar(Long missaoId, TipoOcorrencia tipo, Pageable pageable) {
        buscarMissao(missaoId);
        Criteria criterio = Criteria.where("missaoId").is(missaoId);
        if (tipo != null) {
            criterio = criterio.and("tipo").is(tipo.name());
        }
        long total = mongoTemplate.count(Query.query(criterio), Ocorrencia.class);
        List<Ocorrencia> pagina = mongoTemplate.find(Query.query(criterio).with(pageable), Ocorrencia.class);
        return new PageImpl<>(pagina, pageable, total);
    }

    @Transactional(readOnly = true)
    public Ocorrencia buscar(Long missaoId, String ocorrenciaId) {
        buscarMissao(missaoId);
        if (!ObjectId.isValid(ocorrenciaId)) {
            throw new RecursoNaoEncontradoException("Ocorrência", ocorrenciaId);
        }
        Query consulta = Query.query(Criteria.where("_id").is(new ObjectId(ocorrenciaId)).and("missaoId").is(missaoId));
        Ocorrencia ocorrencia = mongoTemplate.findOne(consulta, Ocorrencia.class);
        if (ocorrencia == null) {
            throw new RecursoNaoEncontradoException("Ocorrência", ocorrenciaId);
        }
        return ocorrencia;
    }

    private Missao buscarMissao(Long id) {
        return missaoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Missão", id));
    }
}
