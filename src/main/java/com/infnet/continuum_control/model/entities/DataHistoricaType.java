package com.infnet.continuum_control.model.entities;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Data histórica antes de Cristo (doc 05, D50). O PostgreSQL guarda {@code 0753-04-21 BC} e o
 * driver JDBC grava um {@code LocalDate} de ano negativo corretamente, mas ao ler devolve o ano
 * 753 d.C. Este tipo lê a data como texto e converte o "BC" para o ano astronômico
 * (1 a.C. = ano 0, 753 a.C. = ano -752), que é o que {@code LocalDate} e o ISO 8601 usam.
 */
public class DataHistoricaType implements UserType<LocalDate> {

    private static final String ANTES_DE_CRISTO = " BC";

    @Override
    public int getSqlType() {
        return Types.DATE;
    }

    @Override
    public Class<LocalDate> returnedClass() {
        return LocalDate.class;
    }

    @Override
    public LocalDate nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
        return converter(rs.getString(position));
    }

    @Override
    public void nullSafeSet(PreparedStatement st, LocalDate value, int index, WrapperOptions options) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.DATE);
        } else {
            st.setObject(index, value);
        }
    }

    static LocalDate converter(String texto) {
        if (texto == null) {
            return null;
        }
        if (!texto.endsWith(ANTES_DE_CRISTO)) {
            return LocalDate.parse(texto);
        }
        LocalDate comoDepoisDeCristo = LocalDate.parse(texto.substring(0, texto.length() - ANTES_DE_CRISTO.length()));
        return comoDepoisDeCristo.withYear(1 - comoDepoisDeCristo.getYear());
    }

    @Override
    public LocalDate deepCopy(LocalDate value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
