package com.infnet.continuum_control.config;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.validation.Validator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** MongoDB das ocorrências (doc 04, seção 4). */
@Slf4j
@Configuration
public class MongoConfig {

    static final String COLECAO = "ocorrencias";

    /** LocalDateTime gravado como date em UTC, pelos codecs do próprio driver (D22). */
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(c -> c.useNativeDriverJavaTimeCodecs());
    }

    /**
     * 4.3: o campo {@code tipo} é a chave do tipo, no lugar de {@code _class}. O campo que o validador
     * confere é o mesmo que o Spring usa para escolher a subclasse, e o documento não leva nome de classe Java.
     * 4.5 (D44): a aplicação prepara a coleção ao subir, com o validador e os índices; é idempotente.
     */
    @Bean
    @Order(1)
    ApplicationRunner prepararColecaoDeOcorrencias(MongoTemplate mongoTemplate, MappingMongoConverter conversor) {
        conversor.setTypeMapper(new DefaultMongoTypeMapper("tipo", conversor.getMappingContext()));
        return (ApplicationArguments args) -> {
            Document esquema = lerEsquema();
            if (!mongoTemplate.collectionExists(COLECAO)) {
                mongoTemplate.createCollection(COLECAO, CollectionOptions.empty()
                        .validator(Validator.document(esquema))
                        .strictValidation()
                        .failOnValidationError());
                log.info("Coleção {} criada com o validador", COLECAO);
            } else {
                try {
                    mongoTemplate.executeCommand(new Document("collMod", COLECAO)
                            .append("validator", esquema)
                            .append("validationLevel", "strict")
                            .append("validationAction", "error"));
                } catch (RuntimeException e) {
                    log.warn("Não foi possível atualizar o validador da coleção {}: {}", COLECAO, e.getMessage());
                }
            }
            IndexOperations indices = mongoTemplate.indexOps(COLECAO);
            indices.createIndex(new Index().on("missaoId", Sort.Direction.ASC).on("dataHora", Sort.Direction.ASC)
                    .named("ix_ocorrencias_missao"));
            indices.createIndex(new Index().on("agenteId", Sort.Direction.ASC).on("dataHora", Sort.Direction.ASC)
                    .named("ix_ocorrencias_agente"));
            indices.createIndex(new Index().on("tipo", Sort.Direction.ASC).named("ix_ocorrencias_tipo"));
        };
    }

    private static Document lerEsquema() {
        try {
            String json = new ClassPathResource("mongo/ocorrencias-schema.json").getContentAsString(StandardCharsets.UTF_8);
            return Document.parse(json);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler mongo/ocorrencias-schema.json", e);
        }
    }
}
