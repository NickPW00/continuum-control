package com.infnet.continuum_control.dto.anomalia;

import com.infnet.continuum_control.model.entities.RegistroHistorico;
import com.infnet.continuum_control.model.enums.TipoRegistro;

import java.time.LocalDateTime;

public record RegistroHistoricoResponse(
        Long id,
        TipoRegistro tipo,
        LocalDateTime dataHora,
        String descricao,
        String valorAnterior,
        String valorNovo,
        Long missaoId,
        Long usuarioId
) {
    public static RegistroHistoricoResponse de(RegistroHistorico r) {
        return new RegistroHistoricoResponse(r.getId(), r.getTipo(), r.getDataHora(), r.getDescricao(),
                r.getValorAnterior(), r.getValorNovo(),
                r.getMissao() == null ? null : r.getMissao().getId(),
                r.getUsuario() == null ? null : r.getUsuario().getId());
    }
}
