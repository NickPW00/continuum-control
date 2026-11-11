// Roda uma vez, na primeira subida do contêiner (volume vazio). Cria o usuário da aplicação,
// com acesso só ao banco "continuum". A coleção, o validador e os índices são criados pela
// própria aplicação ao subir (doc 04, D44); dbAdmin permite atualizar o validador (collMod).
const usuario = process.env.MONGO_APP_USER || 'continuum_app';
const senha = process.env.MONGO_APP_PASSWORD || 'continuum_app';

db.getSiblingDB('continuum').createUser({
  user: usuario,
  pwd: senha,
  roles: [
    { role: 'readWrite', db: 'continuum' },
    { role: 'dbAdmin', db: 'continuum' }
  ]
});
