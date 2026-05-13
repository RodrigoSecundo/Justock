export function getRequiredEnv(name) {
  const value = import.meta.env[name];

  if (!value) {
    throw new Error(`Variável de ambiente obrigatória ausente: ${name}. Configure o arquivo .env.local.`);
  }

  return value;
}