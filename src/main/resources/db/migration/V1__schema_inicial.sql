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

CREATE INDEX ix_evento_historico_importancia ON evento_historico (importancia);
CREATE INDEX ix_evento_historico_data ON evento_historico (data);

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
    CONSTRAINT fk_anomalia_evento FOREIGN KEY (evento_id) REFERENCES evento_historico (id) ON DELETE RESTRICT,
    CONSTRAINT ck_anomalia_risco CHECK ( risco IN ('BAIXO', 'MODERADO', 'ALTO', 'CRITICO') ),
    CONSTRAINT ck_anomalia_situacao CHECK ( situacao IN ('DETECTADA', 'EM_ANALISE', 'CONFIRMADA', 'EM_CORRECAO', 'ESTABILIZADA', 'IRREVERSIVEL') ),
    CONSTRAINT ck_anomalia_origem CHECK ( origem IN ('INTERFERENCIA_NAO_AUTORIZADA', 'CONSEQUENCIA_MISSAO', 'FALHA_OPERACIONAL', 'DESCONHECIDA') )
);

CREATE INDEX ix_anomalia_evento ON anomalia (evento_id);
CREATE INDEX ix_anomalia_situacao ON anomalia (situacao);
CREATE INDEX ix_anomalia_risco ON anomalia (risco);

CREATE TABLE usuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    login VARCHAR(50) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(15) NOT NULL,
    agente_id BIGINT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_login UNIQUE (login),
    CONSTRAINT uk_usuario_agente UNIQUE (agente_id),
    CONSTRAINT fk_usuario_agente FOREIGN KEY (agente_id) REFERENCES agente (id) ON DELETE RESTRICT,
    CONSTRAINT ck_usuario_perfil CHECK ( perfil IN ('OPERADOR', 'AGENTE', 'SUPERVISOR', 'ADMINISTRADOR') ),
    CONSTRAINT ck_usuario_agente_perfil CHECK ( perfil <> 'AGENTE' OR agente_id IS NOT NULL )
);

CREATE TABLE missao (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR(20) NOT NULL,
    objetivo TEXT NOT NULL,
    proposito VARCHAR(15) NOT NULL,
    anomalia_id BIGINT NOT NULL,
    destino_temporal DATE NOT NULL,
    prioridade VARCHAR(10) NOT NULL,
    situacao VARCHAR(12) NOT NULL DEFAULT 'PLANEJADA',
    data_inicio TIMESTAMP,
    data_fim TIMESTAMP,

    CONSTRAINT pk_missao PRIMARY KEY (id),
    CONSTRAINT uk_missao_codigo UNIQUE (codigo),
    CONSTRAINT fk_missao_anomalia FOREIGN KEY (anomalia_id) REFERENCES anomalia (id) ON DELETE RESTRICT,
    CONSTRAINT ck_missao_proposito CHECK ( proposito IN ('INVESTIGACAO', 'CORRECAO') ),
    CONSTRAINT ck_missao_prioridade CHECK ( prioridade IN ('BAIXA', 'NORMAL', 'ALTA', 'EMERGENCIA') ),
    CONSTRAINT ck_missao_situacao CHECK ( situacao IN ('PLANEJADA', 'AUTORIZADA', 'EM_EXECUCAO', 'CONCLUIDA', 'CANCELADA', 'FALHOU') ),
    CONSTRAINT ck_missao_inicio CHECK ( situacao NOT IN ('EM_EXECUCAO', 'CONCLUIDA', 'FALHOU') OR data_inicio IS NOT NULL ),
    CONSTRAINT ck_missao_periodo CHECK ( data_fim IS NULL OR data_inicio IS NULL OR data_fim >= data_inicio )
);

CREATE INDEX ix_missao_anomalia ON missao (anomalia_id);
CREATE INDEX ix_missao_situacao ON missao (situacao);
CREATE UNIQUE INDEX uk_missao_em_execucao ON missao (anomalia_id) WHERE situacao = 'EM_EXECUCAO';
CREATE INDEX ix_missao_falhou ON missao (data_fim) WHERE situacao = 'FALHOU';

CREATE TABLE missao_agente (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    missao_id BIGINT NOT NULL,
    agente_id BIGINT NOT NULL,
    data_designacao TIMESTAMP NOT NULL,
    data_saida TIMESTAMP,

    CONSTRAINT pk_missao_agente PRIMARY KEY (id),
    CONSTRAINT uk_missao_agente UNIQUE (missao_id, agente_id),
    CONSTRAINT fk_missao_agente_missao FOREIGN KEY (missao_id) REFERENCES missao (id) ON DELETE RESTRICT,
    CONSTRAINT fk_missao_agente_agente FOREIGN KEY (agente_id) REFERENCES agente (id) ON DELETE RESTRICT,
    CONSTRAINT ck_missao_agente_periodo CHECK ( data_saida IS NULL OR data_saida >= data_designacao )
);

CREATE INDEX ix_missao_agente_agente ON missao_agente (agente_id);

CREATE TABLE autorizacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    missao_id BIGINT NOT NULL,
    responsavel_id BIGINT NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    observacao TEXT NOT NULL,

    CONSTRAINT pk_autorizacao PRIMARY KEY (id),
    CONSTRAINT uk_autorizacao_missao UNIQUE (missao_id),
    CONSTRAINT fk_autorizacao_missao FOREIGN KEY (missao_id) REFERENCES missao (id) ON DELETE RESTRICT,
    CONSTRAINT fk_autorizacao_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuario (id) ON DELETE RESTRICT
);

CREATE INDEX ix_autorizacao_responsavel ON autorizacao (responsavel_id);

CREATE TABLE intervencao (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    missao_id BIGINT NOT NULL,
    agente_id BIGINT NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    acao TEXT NOT NULL,
    justificativa TEXT NOT NULL,
    impacto_estimado VARCHAR(15) NOT NULL,
    resultado_observado TEXT,

    CONSTRAINT pk_intervencao PRIMARY KEY (id),
    CONSTRAINT fk_intervencao_participacao FOREIGN KEY (missao_id, agente_id)
        REFERENCES missao_agente (missao_id, agente_id) ON DELETE RESTRICT,
    CONSTRAINT ck_intervencao_impacto CHECK ( impacto_estimado IN ('MINIMO', 'CONTROLADO', 'SIGNIFICATIVO', 'SEVERO') )
);

CREATE INDEX ix_intervencao_participacao ON intervencao (missao_id, agente_id);
CREATE INDEX ix_intervencao_severa ON intervencao (data_hora) WHERE impacto_estimado = 'SEVERO';

CREATE TABLE encerramento (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    missao_id BIGINT NOT NULL,
    resultado TEXT NOT NULL,
    resumo TEXT NOT NULL,
    impacto_observado VARCHAR(15) NOT NULL,
    situacao_final_anomalia VARCHAR(15) NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    observacoes TEXT,

    CONSTRAINT pk_encerramento PRIMARY KEY (id),
    CONSTRAINT uk_encerramento_missao UNIQUE (missao_id),
    CONSTRAINT fk_encerramento_missao FOREIGN KEY (missao_id) REFERENCES missao (id) ON DELETE RESTRICT,
    CONSTRAINT ck_encerramento_impacto CHECK ( impacto_observado IN ('MINIMO', 'CONTROLADO', 'SIGNIFICATIVO', 'SEVERO') ),
    CONSTRAINT ck_encerramento_situacao CHECK ( situacao_final_anomalia IN ('DETECTADA', 'CONFIRMADA', 'ESTABILIZADA', 'IRREVERSIVEL') )
);

CREATE TABLE registro_historico (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    anomalia_id BIGINT NOT NULL,
    missao_id BIGINT,
    tipo VARCHAR(20) NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    descricao TEXT NOT NULL,
    valor_anterior VARCHAR(255),
    valor_novo VARCHAR(255),
    usuario_id BIGINT,

    CONSTRAINT pk_registro_historico PRIMARY KEY (id),
    CONSTRAINT fk_registro_historico_anomalia FOREIGN KEY (anomalia_id) REFERENCES anomalia (id) ON DELETE RESTRICT,
    CONSTRAINT fk_registro_historico_missao FOREIGN KEY (missao_id) REFERENCES missao (id) ON DELETE RESTRICT,
    CONSTRAINT fk_registro_historico_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_registro_historico_tipo CHECK ( tipo IN ('MUDANCA_RISCO', 'MUDANCA_SITUACAO', 'MISSAO_CRIADA',
        'AGENTE_DESIGNADO', 'AGENTE_REMOVIDO', 'AUTORIZACAO', 'MISSAO_INICIADA', 'MISSAO_CANCELADA',
        'INTERVENCAO', 'OCORRENCIA', 'ENCERRAMENTO') )
);

CREATE INDEX ix_registro_historico_anomalia ON registro_historico (anomalia_id, data_hora);
CREATE INDEX ix_registro_historico_missao ON registro_historico (missao_id);
CREATE INDEX ix_registro_historico_usuario ON registro_historico (usuario_id);

CREATE TABLE registro_agente (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    agente_id BIGINT NOT NULL,
    situacao_anterior VARCHAR(10),
    situacao_nova VARCHAR(10) NOT NULL,
    motivo TEXT NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    usuario_id BIGINT,

    CONSTRAINT pk_registro_agente PRIMARY KEY (id),
    CONSTRAINT fk_registro_agente_agente FOREIGN KEY (agente_id) REFERENCES agente (id) ON DELETE RESTRICT,
    CONSTRAINT fk_registro_agente_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_registro_agente_situacao_anterior CHECK ( situacao_anterior IN ('DISPONIVEL', 'EM_MISSAO', 'SUSPENSO', 'INATIVO') ),
    CONSTRAINT ck_registro_agente_situacao_nova CHECK ( situacao_nova IN ('DISPONIVEL', 'EM_MISSAO', 'SUSPENSO', 'INATIVO') ),
    CONSTRAINT ck_registro_agente_mudanca CHECK ( situacao_anterior IS NULL OR situacao_anterior <> situacao_nova )
);

CREATE INDEX ix_registro_agente_agente ON registro_agente (agente_id, data_hora);
CREATE INDEX ix_registro_agente_usuario ON registro_agente (usuario_id);

CREATE TABLE outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    destino VARCHAR(30) NOT NULL,
    chave VARCHAR(40) NOT NULL,
    payload JSONB NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    processado_em TIMESTAMP,
    proxima_tentativa TIMESTAMP NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0,
    ultimo_erro TEXT,

    CONSTRAINT pk_outbox PRIMARY KEY (id),
    CONSTRAINT uk_outbox_chave UNIQUE (destino, chave),
    CONSTRAINT ck_outbox_destino CHECK ( destino IN ('MONGO_OCORRENCIAS') ),
    CONSTRAINT ck_outbox_tentativas CHECK ( tentativas >= 0 )
);

CREATE INDEX ix_outbox_pendente ON outbox (proxima_tentativa) WHERE processado_em IS NULL;

CREATE FUNCTION fn_append_only() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'A tabela % só aceita inserções', TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER tg_registro_agente_imutavel
    BEFORE UPDATE OR DELETE ON registro_agente
    FOR EACH ROW EXECUTE FUNCTION fn_append_only();

CREATE TRIGGER tg_registro_historico_imutavel
    BEFORE UPDATE OR DELETE ON registro_historico
    FOR EACH ROW EXECUTE FUNCTION fn_append_only();