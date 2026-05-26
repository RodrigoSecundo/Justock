import { getRequiredEnv } from "./env?v=20260514-3";

const AUTH_API_BASE_URL = getRequiredEnv("VITE_BACKEND_API_BASE_URL");
const AUTH_STORAGE_KEY = "jt:auth";
const USER_STORAGE_KEY = "jt:user";
const AUTH_LAST_ACTIVITY_KEY = "jt:auth:last-activity";
const AUTH_BUILD_KEY = "jt:auth:build";
const AUTH_TIMEOUT_MS = 60 * 60 * 1000;
const AUTH_BUILD_VERSION = "20260526-1";

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

function getStorage() {
  if (typeof window === "undefined") {
    return null;
  }

  return window.sessionStorage;
}

function clearLegacyLocalAuth() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  window.localStorage.removeItem(USER_STORAGE_KEY);
  window.localStorage.removeItem(AUTH_LAST_ACTIVITY_KEY);
}

function readStoredAuth() {
  const storage = getStorage();

  if (!storage) {
    return null;
  }

  const raw = storage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    clearAuth();
    return null;
  }
}

function isExpired(lastActivityAt) {
  const timestamp = Number(lastActivityAt);
  if (!Number.isFinite(timestamp)) {
    return true;
  }

  return Date.now() - timestamp > AUTH_TIMEOUT_MS;
}

export function touchAuthActivity() {
  const storage = getStorage();

  if (!storage || !readStoredAuth()) {
    return false;
  }

  storage.setItem(AUTH_LAST_ACTIVITY_KEY, String(Date.now()));
  return true;
}

export function initializeAuthSession() {
  const storage = getStorage();

  clearLegacyLocalAuth();

  if (!storage) {
    return;
  }

  const previousBuildVersion = storage.getItem(AUTH_BUILD_KEY);
  if (previousBuildVersion && previousBuildVersion !== AUTH_BUILD_VERSION) {
    clearAuth();
  }

  storage.setItem(AUTH_BUILD_KEY, AUTH_BUILD_VERSION);
  ensureValidAuthSession();
}

export function ensureValidAuthSession() {
  const storage = getStorage();
  const authData = readStoredAuth();

  if (!storage || !authData?.token) {
    return null;
  }

  const lastActivityAt = storage.getItem(AUTH_LAST_ACTIVITY_KEY);
  if (isExpired(lastActivityAt)) {
    clearAuth();
    return null;
  }

  return authData;
}

export function isAuthenticated() {
  return Boolean(ensureValidAuthSession()?.token);
}

export function getStoredUser() {
  const storage = getStorage();

  if (!storage) {
    return null;
  }

  const raw = storage.getItem(USER_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    storage.removeItem(USER_STORAGE_KEY);
    return null;
  }
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

  const usuario = {
    id: null,
    nome: data.name || "",
    email: data.email || email,
    role: data.role || "",
  };

  const storage = getStorage();
  if (!storage) {
    throw new Error("Sessão indisponível neste ambiente.");
  }

  storage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authData));
  storage.setItem(USER_STORAGE_KEY, JSON.stringify(usuario));
  touchAuthActivity();

  return usuario;
}

export async function getCurrentUser(id) {
  const res = await fetch(`${AUTH_API_BASE_URL}/usuarios/${id}`);
  return handleResponse(res);
}

export function getAuthToken() {
  return ensureValidAuthSession()?.token || null;
}

export function clearAuth() {
  const storage = getStorage();

  if (storage) {
    storage.removeItem(AUTH_STORAGE_KEY);
    storage.removeItem(USER_STORAGE_KEY);
    storage.removeItem(AUTH_LAST_ACTIVITY_KEY);
    storage.removeItem(AUTH_BUILD_KEY);
  }

  clearLegacyLocalAuth();
}
