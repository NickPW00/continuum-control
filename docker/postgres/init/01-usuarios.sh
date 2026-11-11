#!/bin/bash
# Roda uma vez, na primeira subida do contêiner (volume vazio). Cria os dois usuários do doc 04 (D43):
#   - DB_MIGRACAO_USER: dono do banco e do esquema; é com ele que o Flyway cria as tabelas.
#   - DB_APP_USER: usado pela aplicação; recebe só as permissões dadas pela migração V2.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE "$DB_MIGRACAO_USER" LOGIN PASSWORD '$DB_MIGRACAO_PASSWORD';
    CREATE ROLE "$DB_APP_USER" LOGIN PASSWORD '$DB_APP_PASSWORD';
    ALTER DATABASE "$POSTGRES_DB" OWNER TO "$DB_MIGRACAO_USER";
    ALTER SCHEMA public OWNER TO "$DB_MIGRACAO_USER";
    REVOKE ALL ON SCHEMA public FROM PUBLIC;
    REVOKE ALL ON DATABASE "$POSTGRES_DB" FROM PUBLIC;
    GRANT CONNECT ON DATABASE "$POSTGRES_DB" TO "$DB_MIGRACAO_USER", "$DB_APP_USER";
EOSQL
