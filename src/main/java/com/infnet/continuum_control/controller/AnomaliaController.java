package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.dto.anomalia.*;
import com.infnet.continuum_control.model.enums.NivelRisco;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;
import com.infnet.continuum_control.model.enums.TipoRegistro;
import com.infnet.continuum_control.security.Acesso;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.AnomaliaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/** Anomalias (doc 05, seção 7). Não há DELETE: mesmo estabilizada, continua consultável (§5). */
@RestController
@RequestMapping("/anomalias")
@RequiredArgsConstructor
public class AnomaliaController {

    private final AnomaliaService anomaliaService;

    @PostMapping
    @PreAuthorize(Acesso.OPERACAO)
    public ResponseEntity<AnomaliaResponse> criar(@RequestBody @Valid CriarAnomaliaRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        AnomaliaResponse anomalia = anomaliaService.criar(request);
        return ResponseEntity.created(uriBuilder.path("/anomalias/{id}").buildAndExpand(anomalia.id()).toUri())
                .body(anomalia);
    }

    @GetMapping
    public Page<AnomaliaResponse> listar(@RequestParam(required = false) SituacaoAnomalia situacao,
                                         @RequestParam(required = false) NivelRisco risco,
                                         @RequestParam(required = false) Boolean ativas,
                                         @RequestParam(required = false) Boolean criticas,
                                         @RequestParam(required = false) Long eventoId,
                                         @PageableDefault(size = 20, sort = "dataHoraDeteccao", direction = Sort.Direction.DESC) Pageable pageable) {
        return anomaliaService.listar(situacao, risco, ativas, criticas, eventoId, pageable);
    }

    @GetMapping("/{id}")
    public AnomaliaResponse buscarPorId(@PathVariable Long id) {
        return anomaliaService.buscarPorId(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(Acesso.OPERACAO)
    public AnomaliaResponse atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarAnomaliaRequest request) {
        return anomaliaService.atualizar(id, request);
    }

    @PostMapping("/{id}/alterar-risco")
    @PreAuthorize(Acesso.OPERACAO)
    public AnomaliaResponse alterarRisco(@PathVariable Long id, @RequestBody @Valid AlterarRiscoRequest request,
                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return anomaliaService.alterarRisco(id, request, usuario);
    }

    @PostMapping("/{id}/declarar-irreversivel")
    @PreAuthorize(Acesso.SUPERVISAO)
    public AnomaliaResponse declararIrreversivel(@PathVariable Long id, @RequestBody @Valid JustificativaRequest request,
                                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return anomaliaService.declararIrreversivel(id, request.justificativa(), usuario);
    }

    @GetMapping("/{id}/historico")
    public Page<RegistroHistoricoResponse> historico(@PathVariable Long id,
                                                     @RequestParam(required = false) Long missaoId,
                                                     @RequestParam(required = false) TipoRegistro tipo,
                                                     @PageableDefault(size = 50, sort = {"dataHora", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return anomaliaService.historico(id, missaoId, tipo, pageable);
    }
}
