CREATE TABLE agente (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR(20) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    especialidade VARCHAR(80) NOT NULL,
    situacao VARCHAR(10) NOT NULL DEFAULT 'DISPONIVEL',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_agente PRIMARY KEY (id),
    CONSTRAINT uk_agente_codigo UNIQUE (codigo),
    CONSTRAINT ck_agente_situacao CHECK ( situacao IN ('DISPONIVEL', 'EM_MISSAO', 'SUSPENSO', 'INATIVO') )
);

CREATE INDEX ix_agente_situacao      ON agente (situacao);
CREATE INDEX ix_agente_especialidade ON agente (especialidade);