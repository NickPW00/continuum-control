DO $$
BEGIN
    IF '${appUser}' <> current_user THEN
        EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', '${appUser}');
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON agente, evento_historico, anomalia, usuario, missao, '
                       'missao_agente, autorizacao, intervencao, encerramento, outbox TO %I', '${appUser}');
        EXECUTE format('GRANT SELECT, INSERT ON registro_historico, registro_agente TO %I', '${appUser}');
    END IF;
END
$$;
