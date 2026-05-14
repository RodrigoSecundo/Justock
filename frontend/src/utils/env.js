function getFallbackEnv(name) {
  const hostname = typeof window !== "undefined" ? window.location.hostname : "localhost";
  const protocol = typeof window !== "undefined" ? window.location.protocol : "http:";
  const isPublicFrontendHost = hostname === "justock.com.br" || hostname === "www.justock.com.br";

  if (name === "VITE_BACKEND_API_BASE_URL") {
    return isPublicFrontendHost ? `${protocol}//api.justock.com.br` : "http://localhost:8080";
  }

  if (name === "VITE_API_BASE_URL") {
    return isPublicFrontendHost ? `${protocol}//mock.justock.com.br` : "http://localhost:3001";
  }

  return undefined;
}

export function getRequiredEnv(name) {
  const value = import.meta.env[name];

  if (value) {
    return value;
  }

  const fallbackValue = getFallbackEnv(name);
  if (fallbackValue) {
    return fallbackValue;
  }

  throw new Error(`Variável de ambiente obrigatória ausente: ${name}. Configure o arquivo .env.local.`);
}