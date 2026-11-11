package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.usuario.AtualizarUsuarioRequest;
import com.infnet.continuum_control.dto.usuario.CriarUsuarioRequest;
import com.infnet.continuum_control.dto.usuario.UsuarioResponse;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.model.enums.Perfil;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import com.infnet.continuum_control.repository.AgenteRepository;
import com.infnet.continuum_control.repository.Filtros;
import com.infnet.continuum_control.repository.UsuarioRepository;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão de usuários pelo ADMINISTRADOR (doc 05, seção 4; doc 06, S5 e S6). */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest req) {
        if (usuarioRepository.existsByLogin(req.login())) {
            throw new RegraDeNegocioException(null, "Este login já está em uso");
        }
        Agente agente = agenteDoPerfil(req.agenteId());
        Usuario usuario = new Usuario(req.login(), passwordEncoder.encode(req.senha()), req.perfil(), agente);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Perfil perfil, Boolean ativo, Pageable pageable) {
        var filtros = Filtros.de(Usuario.class).igual("perfil", perfil).igual("ativo", ativo).montar();
        return usuarioRepository.findAll(filtros, pageable).map(UsuarioResponse::de);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    /** S5: o último ADMINISTRADOR ativo não perde o perfil. */
    @Transactional
    public UsuarioResponse atualizar(Long id, AtualizarUsuarioRequest req) {
        Usuario usuario = buscar(id);
        Perfil novoPerfil = req.perfil() != null ? req.perfil() : usuario.getPerfil();
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR && novoPerfil != Perfil.ADMINISTRADOR) {
            exigirOutroAdministradorAtivo(usuario);
        }
        Agente agente = req.agenteId() != null ? agenteDoPerfil(req.agenteId()) : usuario.getAgente();
        usuario.alterarPerfil(novoPerfil, agente);
        return UsuarioResponse.de(usuario);
    }

    /** S5: ninguém desativa a si mesmo, e sempre resta um ADMINISTRADOR ativo. */
    @Transactional
    public UsuarioResponse desativar(Long id, UsuarioAutenticado quemPede) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(quemPede.id())) {
            throw new RegraDeNegocioException("S5", "Ninguém pode desativar a si mesmo");
        }
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR && usuario.isAtivo()) {
            exigirOutroAdministradorAtivo(usuario);
        }
        usuario.desativar();
        return UsuarioResponse.de(usuario);
    }

    /** S6: um usuário ligado a um agente INATIVO não volta. */
    @Transactional
    public UsuarioResponse reativar(Long id) {
        Usuario usuario = buscar(id);
        if (usuario.getAgente() != null && usuario.getAgente().getSituacao() == SituacaoAgente.INATIVO) {
            throw new RegraDeNegocioException("S6", "O usuário está ligado a um agente INATIVO e não pode ser reativado");
        }
        usuario.reativar();
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        buscar(id).alterarSenha(passwordEncoder.encode(novaSenha));
    }

    private void exigirOutroAdministradorAtivo(Usuario usuario) {
        if (usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR) <= 1) {
            throw new RegraDeNegocioException("S5", "Precisa existir pelo menos um ADMINISTRADOR ativo");
        }
    }

    private Agente agenteDoPerfil(Long agenteId) {
        if (agenteId == null) return null;
        return agenteRepository.findById(agenteId).orElseThrow(() -> new RecursoNaoEncontradoException("Agente", agenteId));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }
}
