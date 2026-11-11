# Continuum Control

Sistema de gestão e controle da integridade temporal da Continuum Authority: agentes, eventos
históricos, anomalias, missões, intervenções, ocorrências, histórico e indicadores de
estabilidade, expostos por uma API HTTP com autenticação JWT.

A especificação completa está em [`docs/`](docs):

| Documento | Conteúdo |
|---|---|
| [00 — Enunciado](docs/00-enunciado.pdf) | Requisitos originais (§1 a §27) |
| [01 — Modelo de domínio](docs/01-modelo-de-dominio.pdf) | Classes, regras de negócio e decisões D1–D13, D31–D36 |
| [02 — Modelo de dados](docs/02-modelo-de-dados.pdf) | DER, dicionário, restrições e índices (D14–D22, D37–D39) |
| [03 — Estados](docs/03-estados.pdf) | Máquinas de estado de Missão, Agente e Anomalia e operações compostas (D23–D30) |
| [04 — Persistência](docs/04-persistencia.pdf) | PostgreSQL, MongoDB, Redis e o *outbox* (D40–D44) |
| [05 — API](docs/05-api.pdf) | Rotas, corpos, paginação e erros (D45–D52) |
| [06 — Segurança](docs/06-seguranca.pdf) | JWT, perfis, matriz de permissões e regras S1–S7 (D53–D57) |

## Serviços necessários

| Serviço | Versão | Papel |
|---|---|---|
| PostgreSQL | 16 | Fonte da verdade: tudo que tem integridade referencial ou transação |
| MongoDB | 7 | Ocorrências, com estrutura diferente por tipo |
| Redis | 7 | Cache do Estado Operacional e do Índice de Estabilidade |
| Java | 21 | Só para rodar fora do Docker |
| Docker e Docker Compose | — | Ambiente e testes de integração (Testcontainers) |

O porquê de cada banco está no doc 04, seção 2.

## Subir tudo com Docker (forma recomendada)

```bash
docker compose up -d --build
```

Sobe os três bancos e a aplicação na porta **8080**. Na primeira subida:

1. os scripts de `docker/` criam os usuários dos bancos;
2. o Flyway cria o esquema (`V1`) e dá as permissões do usuário da aplicação (`V2`);
3. a aplicação cria a coleção `ocorrencias` no MongoDB, com o validador e os índices;
4. o perfil `dev` carrega os dados iniciais.

Para recomeçar do zero (apaga os dados):

```bash
docker compose down -v
```

## Configuração

Nada que dependa do ambiente fica no código (§24). Tudo vem de variáveis de ambiente; o compose
usa valores padrão de desenvolvimento. Para outro ambiente, copie `.env.example` para `.env` e
troque as senhas e o `JWT_SECRET`.

| Variável | Uso |
|---|---|
| `DB_URL`, `DB_APP_USER`, `DB_APP_PASSWORD` | PostgreSQL, usuário da aplicação (sem DDL) |
| `DB_MIGRACAO_USER`, `DB_MIGRACAO_PASSWORD` | PostgreSQL, dono do esquema, usado só pelo Flyway |
| `MONGO_URI` | MongoDB, com usuário e banco |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis |
| `JWT_SECRET` | Segredo dos tokens, 32 bytes ou mais |
| `SPRING_PROFILES_ACTIVE` | `dev` carrega os dados iniciais |

Pesos do índice, validade do cache e parâmetros do *outbox* ficam em `continuum.*` no
`application.yaml` e também podem ser sobrescritos por variável de ambiente.

## Rodar a aplicação na IDE

```bash
docker compose up -d postgres mongo redis
```

Depois rode `ContinuumControlApplication` (perfil `dev` por padrão; os valores locais estão em
`application-dev.yaml`), ou:

```bash
./mvnw spring-boot:run
```

## Construir

```bash
./mvnw package -DskipTests
```

Gera o executável `target/continuum_control-0.0.1-SNAPSHOT.jar` (`java -jar ...`).

## Testes

```bash
./mvnw test
```

Precisa do Docker ligado: os testes de integração sobem PostgreSQL, MongoDB e Redis reais com
Testcontainers.

| Pacote | O que verifica |
|---|---|
| `model.entities` | Cada transição das tabelas 3.1, 4.1 e 5.1 do doc 03 (permitida e guarda violada) e todas as combinações da tabela 5.2 (D35) |
| `service.indicadores` | Fórmula do índice: exemplo do D36, limites de 0 a 100 e "um problema a mais nunca aumenta o índice" |
| `integracao.MissaoIntegracaoTest` | Operações compostas pela API, caminhos da tabela 8.1 (D23, D24, D25), data a.C. (D50) e erros (§18) |
| `integracao.SegurancaIntegracaoTest` | 401, 403 por perfil e por posse (S3), S2, S4, S5, S6, S7 e D55 |
| `integracao.PersistenciaIntegracaoTest` | *Outbox* até o MongoDB, os seis tipos de ocorrência, validador do MongoDB, falha sem resíduo e cache no Redis |
| `integracao.AtomicidadeIntegracaoTest` | Falha no fim de "iniciar missão" desfaz missão, agentes, anomalia e histórico (§9) |

## Verificar se está no ar

```bash
curl localhost:8080/actuator/health
```

Mostra o estado da aplicação, dos três bancos e do *outbox* (pendências de entrega ao MongoDB).

```bash
curl -s -X POST localhost:8080/auth/login -H "Content-Type: application/json" -d '{"login":"operador","senha":"continuum-dev"}'
```

Use o `token` da resposta nas outras chamadas, no cabeçalho `Authorization: Bearer <token>`.

## Usuários de desenvolvimento

Carregados pelo perfil `dev` (doc 06, seção 9). Senha de todos: `continuum-dev`.

| Login | Perfil | Agente |
|---|---|---|
| `admin` | ADMINISTRADOR | — |
| `supervisor` | SUPERVISOR | — |
| `operador` | OPERADOR | — |
| `ana.lima` | AGENTE | AG-001, designada na MI-001 |
| `bruno.reis` | AGENTE | AG-002, em execução na MI-002 |

Os dados iniciais também trazem cinco eventos (inclusive a Fundação de Roma, em 753 a.C.), quatro
anomalias e três missões: MI-001 planejada e exigindo autorização, MI-002 em execução com uma
intervenção e uma ocorrência, e MI-003 concluída.

## Organização do código

```
src/main/java/com/infnet/continuum_control
├── model/entities       entidades JPA e as regras de estado (doc 01 e 03)
├── model/ocorrencias    documentos do MongoDB, um subtipo por tipo de ocorrência
├── repository           Spring Data JPA
├── service              casos de uso; cada método público é uma transação
│   ├── ocorrencias      registro com outbox e o entregador ao MongoDB
│   └── indicadores      índice de estabilidade e cache no Redis
├── controller           rotas HTTP (doc 05)
├── security             JWT, usuário autenticado e regra de posse (doc 06)
├── config               propriedades, MongoDB, relógio UTC e dados iniciais
└── exception            erros no formato ProblemDetail
src/main/resources
├── db/migration         V1 (esquema) e V2 (permissões), aplicados pelo Flyway
└── mongo                validador $jsonSchema da coleção ocorrencias
docker/                  scripts de criação dos usuários dos bancos
```

## Decisões de implementação

Pontos em que a implementação detalha ou ajusta o que os documentos propõem:

- **Datas antes de Cristo (D50).** O driver JDBC grava `0753-04-21 BC` corretamente, mas lê de
  volta como 753 d.C. O tipo `DataHistoricaType` lê a data como texto e converte para o ano
  astronômico (753 a.C. = `-0752`). O teste `dataAntesDeCristo` confere a ida e a volta.
- **Duração da distorção temporal.** Guardada como número inteiro de segundos (`Long duracao`),
  que é o formato da API (doc 05, 1.2) e do validador do MongoDB, sem conversor intermediário.
- **Cache dos indicadores.** O *cache-aside* do doc 04 (seção 5) está escrito diretamente no
  `IndicadoresService` com `StringRedisTemplate`: leitura, gravação com validade de 60 s,
  invalidação depois do commit (`TransactionSynchronization.afterCommit`) e falha aberta.
- **Usuário da aplicação no MongoDB.** Tem `readWrite` e `dbAdmin` no banco `continuum`: o
  `dbAdmin` é o que permite atualizar o validador com `collMod` quando a aplicação sobe de novo.
