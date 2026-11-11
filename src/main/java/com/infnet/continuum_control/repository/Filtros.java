package com.infnet.continuum_control.repository;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Monta uma consulta com os filtros opcionais de uma listagem (§16): cada filtro informado vira
 * uma condição, e as condições se combinam com E.
 */
public final class Filtros<T> {

    private final List<Specification<T>> condicoes = new ArrayList<>();

    public static <T> Filtros<T> de(Class<T> tipo) {
        return new Filtros<>();
    }

    public Filtros<T> igual(String atributo, Object valor) {
        if (valor != null) {
            condicoes.add((root, query, cb) -> cb.equal(root.get(atributo), valor));
        }
        return this;
    }

    public Filtros<T> igualNoCaminho(String associacao, String atributo, Object valor) {
        if (valor != null) {
            condicoes.add((root, query, cb) -> cb.equal(root.get(associacao).get(atributo), valor));
        }
        return this;
    }

    public Filtros<T> em(String atributo, Collection<?> valores, boolean aplicar) {
        if (aplicar) {
            condicoes.add((root, query, cb) -> root.get(atributo).in(valores));
        }
        return this;
    }

    public <C extends Comparable<? super C>> Filtros<T> aPartirDe(String atributo, C valor) {
        if (valor != null) {
            condicoes.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.<C>get(atributo), valor));
        }
        return this;
    }

    public <C extends Comparable<? super C>> Filtros<T> ate(String atributo, C valor) {
        if (valor != null) {
            condicoes.add((root, query, cb) -> cb.lessThanOrEqualTo(root.<C>get(atributo), valor));
        }
        return this;
    }

    public Filtros<T> condicao(Specification<T> especificacao, boolean aplicar) {
        if (aplicar) {
            condicoes.add(especificacao);
        }
        return this;
    }

    public Specification<T> montar() {
        return Specification.allOf(condicoes);
    }
}
