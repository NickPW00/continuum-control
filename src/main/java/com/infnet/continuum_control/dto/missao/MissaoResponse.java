package com.infnet.continuum_control.dto.missao;

import com.infnet.continuum_control.model.entities.Autorizacao;
import com.infnet.continuum_control.model.entities.Encerramento;
import com.infnet.continuum_control.model.entities.Missao;
import com.infnet.continuum_control.model.entities.MissaoAgente;
import com.infnet.continuum_control.model.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A missão já traz participações, autorização e encerramento, porque eles só existem dentro dela
 * (composições do doc 01, D49). {@code exigeAutorizacao} é calculado na consulta (doc 03, 3.2).
 */
public record MissaoResponse(
        Long id,
        String codigo,
        String objetivo,
        PropositoMissao proposito,
        Long anomaliaId,
        LocalDate destinoTemporal,
        Prioridade prioridade,
        SituacaoMissao situacao,
        boolean exigeAutorizacao,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        List<Participacao> participacoes,
        AutorizacaoResumo autorizacao,
        EncerramentoResumo encerramento
) {

    public record Participacao(Long agenteId, LocalDateTime dataDesignacao, LocalDateTime dataSaida) {
        static Participacao de(MissaoAgente p) {
            return new Participacao(p.getAgente().getId(), p.getDataDesignacao(), p.getDataSaida());
        }
    }

    public record AutorizacaoResumo(Long responsavelId, LocalDateTime dataHora, String observacao) {
        static AutorizacaoResumo de(Autorizacao a) {
            return a == null ? null : new AutorizacaoResumo(a.getResponsavel().getId(), a.getDataHora(), a.getObservacao());
        }
    }

    public record EncerramentoResumo(String resultado, String resumo, Impacto impactoObservado,
                                     SituacaoAnomalia situacaoFinalAnomalia, LocalDateTime dataHora,
                                     String observacoes) {
        static EncerramentoResumo de(Encerramento e) {
            return e == null ? null : new EncerramentoResumo(e.getResultado(), e.getResumo(), e.getImpactoObservado(),
                    e.getSituacaoFinalAnomalia(), e.getDataHora(), e.getObservacoes());
        }
    }

    public static MissaoResponse de(Missao m) {
        return new MissaoResponse(m.getId(), m.getCodigo(), m.getObjetivo(), m.getProposito(),
                m.getAnomalia().getId(), m.getDestinoTemporal(), m.getPrioridade(), m.getSituacao(),
                m.exigeAutorizacao(), m.getDataInicio(), m.getDataFim(),
                m.getParticipacoes().stream().map(Participacao::de).toList(),
                AutorizacaoResumo.de(m.getAutorizacao()),
                EncerramentoResumo.de(m.getEncerramento()));
    }
}
