const AUTH_API_BASE_URL = import.meta.env.VITE_BACKEND_API_BASE_URL || "http://localhost:8080";
const AUTH_STORAGE_KEY = "jt:auth";

async function handleResponse(response) {
  if (!response.ok) {
    const errorBody = await response.json().catch(() => undefined);
    const message = errorBody?.message || errorBody?.error || errorBody?.data?.message || `Erro HTTP ${response.status}`;
    throw new Error(message);
  }
  return response.json();
}

function normalizeApiEnvelope(payload) {
  if (payload && typeof payload === "object" && "data" in payload) {
    return payload.data;
  }
  return payload;
}

export async function login({ email, senha }) {
  const res = await fetch(`${AUTH_API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      email,
      password: senha,
    }),
  });

  const payload = await handleResponse(res);
  const data = normalizeApiEnvelope(payload);

  if (!data?.token) {
    throw new Error("Token não retornado pelo backend");
  }

  const authData = {
    token: data.token,
    email: data.email,
    role: data.role,
    name: data.name,
  };

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authData));

  return {
    id: null,
    nome: data.name || "",
    email: data.email || email,
    role: data.role || "",
  };
}

export async function getCurrentUser(id) {
  const res = await fetch(`${AUTH_API_BASE_URL}/usuarios/${id}`);
  return handleResponse(res);
}

export function getAuthToken() {
  try {
    const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return parsed?.token || null;
  } catch {
    return null;
  }
}

export function clearAuth() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
}
