package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.NivelRisco;
import com.infnet.continuum_control.model.enums.PropositoMissao;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;
import com.infnet.continuum_control.model.enums.SituacaoMissao;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.infnet.continuum_control.model.enums.SituacaoAnomalia.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * D35: a conferência da situação final fica dividida entre Anomalia e Missao, e a interseção das
 * duas precisa ser exatamente a tabela 5.2 do doc 03. Percorre todas as combinações de propósito,
 * desfecho e situação final, e compara com a tabela escrita aqui à parte.
 */
class Tabela52Test {

    /** Tabela 5.2, transcrita do documento. */
    private static Set<SituacaoAnomalia> esperadas(PropositoMissao proposito, SituacaoMissao desfecho, boolean irreversivel) {
        if (irreversivel) return EnumSet.of(IRREVERSIVEL);
        if (proposito == PropositoMissao.INVESTIGACAO && desfecho == SituacaoMissao.CONCLUIDA)
            return EnumSet.of(CONFIRMADA, ESTABILIZADA, IRREVERSIVEL);
        if (proposito == PropositoMissao.INVESTIGACAO) return EnumSet.of(DETECTADA, CONFIRMADA, IRREVERSIVEL);
        if (desfecho == SituacaoMissao.CONCLUIDA) return EnumSet.of(ESTABILIZADA, CONFIRMADA);
        return EnumSet.of(CONFIRMADA, IRREVERSIVEL);
    }

    static List<Arguments> combinacoes() {
        List<Arguments> casos = new ArrayList<>();
        for (PropositoMissao proposito : PropositoMissao.values()) {
            for (SituacaoMissao desfecho : List.of(SituacaoMissao.CONCLUIDA, SituacaoMissao.FALHOU)) {
                for (boolean irreversivel : List.of(false, true)) {
                    if (irreversivel && proposito == PropositoMissao.CORRECAO) continue;   // §5: não existe
                    for (SituacaoAnomalia situacaoFinal : SituacaoAnomalia.values()) {
                        casos.add(Arguments.of(proposito, desfecho, irreversivel, situacaoFinal));
                    }
                }
            }
        }
        return casos;
    }

    @ParameterizedTest(name = "{0} {1} irreversível={2} → {3}")
    @MethodSource("combinacoes")
    void situacaoFinalPermitidaSoNaTabela(PropositoMissao proposito, SituacaoMissao desfecho,
                                          boolean irreversivel, SituacaoAnomalia situacaoFinal) {
        Anomalia anomalia = Cenario.anomalia(NivelRisco.ALTO);
        if (irreversivel) anomalia.declararIrreversivel("x");
        Missao missao = Cenario.emExecucao(anomalia, proposito, Cenario.agente("AG-1"));
        DadosEncerramento dados = Cenario.encerramento(situacaoFinal);

        boolean permitida = esperadas(proposito, desfecho, irreversivel).contains(situacaoFinal);
        Runnable encerrar = desfecho == SituacaoMissao.CONCLUIDA
                ? () -> missao.concluir(dados, Cenario.AGORA)
                : () -> missao.registrarFalha(dados, Cenario.AGORA);

        if (permitida) {
            encerrar.run();
            assertThat(missao.getSituacao()).isEqualTo(desfecho);
            assertThat(anomalia.getSituacao()).isEqualTo(situacaoFinal);
        } else {
            assertThatThrownBy(encerrar::run).isInstanceOf(RegraDeNegocioException.class);
            assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.EM_EXECUCAO);
        }
    }
}
