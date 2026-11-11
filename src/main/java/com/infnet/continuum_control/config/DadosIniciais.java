package com.infnet.continuum_control.config;

import com.infnet.continuum_control.dto.agente.AgenteRequest;
import com.infnet.continuum_control.dto.agente.AgenteResponse;
import com.infnet.continuum_control.dto.anomalia.AnomaliaResponse;
import com.infnet.continuum_control.dto.anomalia.CriarAnomaliaRequest;
import com.infnet.continuum_control.dto.evento_historico.CriarEventoRequest;
import com.infnet.continuum_control.dto.evento_historico.EventoResponse;
import com.infnet.continuum_control.dto.intervencao.CriarIntervencaoRequest;
import com.infnet.continuum_control.dto.missao.CriarMissaoRequest;
import com.infnet.continuum_control.dto.missao.EncerrarMissaoRequest;
import com.infnet.continuum_control.dto.missao.MissaoResponse;
import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.model.enums.*;
import com.infnet.continuum_control.model.ocorrencias.EvidenciaTemporal;
import com.infnet.continuum_control.repository.AgenteRepository;
import com.infnet.continuum_control.repository.UsuarioRepository;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.*;
import com.infnet.continuum_control.service.ocorrencias.OcorrenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Dados iniciais do ambiente de desenvolvimento e avaliação (§25, doc 06 seção 9). Só roda no
 * perfil dev e com a tabela de usuários vazia. Tudo passa pelos próprios serviços, então os
 * históricos ficam coerentes, numa única transação.
 */
@Slf4j
@Component
@Profile("dev")
@Order(10)
@RequiredArgsConstructor
public class DadosIniciais implements ApplicationRunner {

    public static final String SENHA = "continuum-dev";

    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AgenteService agenteService;
    private final EventoHistoricoService eventoService;
    private final AnomaliaService anomaliaService;
    private final MissaoService missaoService;
    private final IntervencaoService intervencaoService;
    private final OcorrenciaService ocorrenciaService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        log.info("Carregando os dados iniciais do perfil dev");

        UsuarioAutenticado admin = UsuarioAutenticado.de(
                usuarioRepository.save(new Usuario("admin", passwordEncoder.encode(SENHA), Perfil.ADMINISTRADOR, null)));

        AgenteResponse ana = agente("AG-001", "Ana Lima", "Roma Antiga", admin);
        AgenteResponse bruno = agente("AG-002", "Bruno Reis", "Corrida Espacial", admin);
        AgenteResponse carla = agente("AG-003", "Carla Mendes", "Corrida Espacial", admin);
        AgenteResponse diego = agente("AG-004", "Diego Souza", "Guerra Fria", admin);
        agente("AG-005", "Elisa Prado", "Grandes Navegações", admin);

        usuario("supervisor", Perfil.SUPERVISOR, null);
        UsuarioAutenticado operador = UsuarioAutenticado.de(usuario("operador", Perfil.OPERADOR, null));
        usuario("ana.lima", Perfil.AGENTE, ana.id());
        UsuarioAutenticado brunoUsuario = UsuarioAutenticado.de(usuario("bruno.reis", Perfil.AGENTE, bruno.id()));

        EventoResponse roma = evento("Fundação de Roma", "Rômulo funda a cidade no Palatino",
                LocalDate.of(-752, 4, 21), "Roma", Importancia.CRITICA);
        EventoResponse apollo = evento("Pouso da Apollo 11", "Primeiro pouso tripulado na Lua",
                LocalDate.of(1969, 7, 20), "Mar da Tranquilidade, Lua", Importancia.CRITICA);
        EventoResponse muro = evento("Queda do Muro de Berlim", "Abertura da fronteira entre Berlim Oriental e Ocidental",
                LocalDate.of(1989, 11, 9), "Berlim", Importancia.ALTA);
        EventoResponse magna = evento("Assinatura da Magna Carta", "O rei João aceita limites ao poder real",
                LocalDate.of(1215, 6, 15), "Runnymede", Importancia.ALTA);
        evento("Publicação de A Origem das Espécies", "Darwin publica a teoria da seleção natural",
                LocalDate.of(1859, 11, 24), "Londres", Importancia.MODERADA);

        AnomaliaResponse anomaliaRoma = anomalia(roma, "Registros indicam a fundação em 750 a.C.", NivelRisco.CRITICO,
                OrigemAnomalia.INTERFERENCIA_NAO_AUTORIZADA);
        AnomaliaResponse anomaliaApollo = anomalia(apollo, "A transmissão do pouso não consta nos arquivos",
                NivelRisco.ALTO, OrigemAnomalia.DESCONHECIDA);
        AnomaliaResponse anomaliaMuro = anomalia(muro, "A queda aparece registrada em 1990", NivelRisco.MODERADO,
                OrigemAnomalia.CONSEQUENCIA_MISSAO);
        anomalia(magna, "A cláusula 39 não aparece nas cópias consultadas", NivelRisco.BAIXO, null);

        // MI-001: correção de emergência sobre anomalia CRITICA, planejada; precisa de autorização para iniciar
        MissaoResponse mi001 = missao("MI-001", "Restaurar a data da fundação de Roma", PropositoMissao.CORRECAO,
                anomaliaRoma, LocalDate.of(-752, 4, 20), Prioridade.EMERGENCIA, operador);
        missaoService.designar(mi001.id(), ana.id(), operador);

        // MI-002: investigação em execução, com uma intervenção e uma ocorrência do agente Bruno
        MissaoResponse mi002 = missao("MI-002", "Investigar o desaparecimento da transmissão", PropositoMissao.INVESTIGACAO,
                anomaliaApollo, LocalDate.of(1969, 7, 20), Prioridade.ALTA, operador);
        missaoService.designar(mi002.id(), bruno.id(), operador);
        missaoService.designar(mi002.id(), carla.id(), operador);
        missaoService.iniciar(mi002.id(), operador);
        intervencaoService.registrar(mi002.id(), new CriarIntervencaoRequest(
                "Copiar as fitas originais antes do arquivamento", "As cópias seriam perdidas na mudança do arquivo",
                Impacto.CONTROLADO, null), brunoUsuario);
        EvidenciaTemporal evidencia = new EvidenciaTemporal();
        evidencia.setObservacao("Fita de vídeo com etiqueta de 1971 no arquivo de 1969");
        evidencia.setObjeto("Fita de vídeo");
        evidencia.setPeriodoEstimado("Década de 1970");
        evidencia.setGrauConfiabilidade(0.8);
        ocorrenciaService.registrar(mi002.id(), evidencia, brunoUsuario);

        // MI-003: investigação já concluída, que confirmou a anomalia
        MissaoResponse mi003 = missao("MI-003", "Confirmar a data da queda do Muro", PropositoMissao.INVESTIGACAO,
                anomaliaMuro, LocalDate.of(1989, 11, 9), Prioridade.NORMAL, operador);
        missaoService.designar(mi003.id(), diego.id(), operador);
        missaoService.iniciar(mi003.id(), operador);
        missaoService.concluir(mi003.id(), new EncerrarMissaoRequest("Divergência confirmada",
                "Os jornais de 10/11/1989 foram alterados", Impacto.MINIMO, SituacaoAnomalia.CONFIRMADA, null), operador);

        log.info("Dados iniciais carregados: 5 usuários (senha '{}'), 5 agentes, 5 eventos, 4 anomalias, 3 missões", SENHA);
    }

    private AgenteResponse agente(String codigo, String nome, String especialidade, UsuarioAutenticado admin) {
        return agenteService.cadastrar(new AgenteRequest(codigo, nome, especialidade), admin);
    }

    private Usuario usuario(String login, Perfil perfil, Long agenteId) {
        Agente agente = agenteId == null ? null : agenteRepository.getReferenceById(agenteId);
        return usuarioRepository.save(new Usuario(login, passwordEncoder.encode(SENHA), perfil, agente));
    }

    private EventoResponse evento(String acontecimento, String descricao, LocalDate data, String local, Importancia importancia) {
        return eventoService.criar(new CriarEventoRequest(acontecimento, descricao, data, local, importancia));
    }

    private AnomaliaResponse anomalia(EventoResponse evento, String divergencia, NivelRisco risco, OrigemAnomalia origem) {
        return anomaliaService.criar(new CriarAnomaliaRequest(evento.id(), divergencia, risco, origem, null, null));
    }

    private MissaoResponse missao(String codigo, String objetivo, PropositoMissao proposito, AnomaliaResponse anomalia,
                                  LocalDate destino, Prioridade prioridade, UsuarioAutenticado usuario) {
        return missaoService.criar(new CriarMissaoRequest(codigo, objetivo, proposito, anomalia.id(), destino, prioridade),
                usuario);
    }
}
