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

CREATE INDEX ix_agente_situacao ON agente (situacao);
CREATE INDEX ix_agente_especialidade ON agente (especialidade);

CREATE TABLE evento_historico (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    acontecimento VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    data DATE NOT NULL,
    localizacao VARCHAR(150) NOT NULL,
    importancia VARCHAR(10) NOT NULL,

    CONSTRAINT pk_evento_historico PRIMARY KEY (id),
    CONSTRAINT ck_evento_historico_importancia CHECK ( importancia IN ('BAIXA', 'MODERADA', 'ALTA', 'CRITICA') )
);

CREATE INDEX ix_evento_importancia ON evento_historico (importancia);
CREATE INDEX ix_evento_data ON evento_historico (data);

CREATE TABLE anomalia (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    evento_id BIGINT NOT NULL,
    divergencia_observada TEXT NOT NULL,
    data_hora_deteccao TIMESTAMP NOT NULL,
    risco VARCHAR(10) NOT NULL,
    situacao VARCHAR(15) NOT NULL DEFAULT 'DETECTADA',
    origem VARCHAR(30),
    observacoes TEXT,

    CONSTRAINT pk_anomalia PRIMARY KEY (id),
    CONSTRAINT fk_evento_historico FOREIGN KEY (evento_id) REFERENCES evento_historico (id) ON DELETE RESTRICT,
    CONSTRAINT ck_nivel_risco CHECK ( risco IN ('BAIXO', 'MODERADO', 'ALTO', 'CRITICO') ),
    CONSTRAINT ck_situacao_anomalia CHECK ( situacao IN ('DETECTADA', 'EM_ANALISE', 'CONFIRMADA', 'EM_CORRECAO', 'ESTABILIZADA', 'IRREVERSIVEL') ),
    CONSTRAINT ck_origem_anomalia CHECK ( origem IN ('INTERFERENCIA_NAO_AUTORIZADA', 'CONSEQUENCIA_MISSAO', 'FALHA_OPERACIONAL', 'DESCONHECIDA') )
);

CREATE INDEX ix_anomalia_evento ON anomalia (evento_id);
CREATE INDEX ix_anomalia_situacao ON anomalia (situacao);
CREATE INDEX ix_anomalia_risco ON anomalia (risco);