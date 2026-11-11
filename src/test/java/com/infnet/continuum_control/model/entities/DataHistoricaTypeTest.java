package com.infnet.continuum_control.model.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** D50: o ano astronômico do LocalDate e o "BC" do PostgreSQL. */
class DataHistoricaTypeTest {

    @Test
    @DisplayName("753 a.C. é o ano -752; 1 a.C. é o ano 0; datas d.C. não mudam")
    void converteAntesDeCristo() {
        assertThat(DataHistoricaType.converter("0753-04-21 BC")).isEqualTo(LocalDate.of(-752, 4, 21));
        assertThat(DataHistoricaType.converter("0001-12-31 BC")).isEqualTo(LocalDate.of(0, 12, 31));
        assertThat(DataHistoricaType.converter("1969-07-20")).isEqualTo(LocalDate.of(1969, 7, 20));
        assertThat(DataHistoricaType.converter(null)).isNull();
    }
}
