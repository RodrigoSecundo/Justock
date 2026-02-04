const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:3001";

async function handleResponse(response) {
  if (!response.ok) {
    const errorBody = await response.json().catch(() => undefined);
    const message = errorBody?.message || errorBody?.error || `Erro HTTP ${response.status}`;
    throw new Error(message);
  }
  return response.json();
}

export async function login({ email, senha }) {
  const res = await fetch(`${API_BASE_URL}/usuarios?email=${encodeURIComponent(email)}&senha=${encodeURIComponent(senha)}`);
  const data = await handleResponse(res);
  const usuario = Array.isArray(data) ? data[0] : data;
  if (!usuario) {
    throw new Error("Usuário ou senha inválidos");
  }
  return usuario;
}

export async function getCurrentUser(id) {
  const res = await fetch(`${API_BASE_URL}/usuarios/${id}`);
  return handleResponse(res);
}
