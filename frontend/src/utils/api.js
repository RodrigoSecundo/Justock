const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:3001";

async function handleResponse(response) {
  if (!response.ok) {
    const errorBody = await response.json().catch(() => undefined);
    const message = errorBody?.message || errorBody?.error || `Erro HTTP ${response.status}`;
    throw new Error(message);
  }
  return response.json();
}

export async function getDashboardResumo() {
  const res = await fetch(`${API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return {
    total: data.totalProducts,
    lowStock: data.lowStockProducts,
    connectedMarketplaces: data.connectedMarketplaces,
    syncStatus: data.syncStatus,
  };
}

export async function getDashboardInventoryOverview() {
  const res = await fetch(`${API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return data.inventoryOverview;
}

export async function getDashboardRecentActivity() {
  const res = await fetch(`${API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return { activities: data.recentActivity };
}

export async function getDashboardAlerts() {
  const res = await fetch(`${API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return { alerts: data.alerts };
}

export async function getConexoes() {
  const res = await fetch(`${API_BASE_URL}/conexoes`);
  const data = await handleResponse(res);
  return { marketplaces: data };
}

export async function getProdutos() {
  const res = await fetch(`${API_BASE_URL}/produtos`);
  const data = await handleResponse(res);
  return { products: data };
}

export async function getPedidos() {
  const res = await fetch(`${API_BASE_URL}/pedidos`);
  const data = await handleResponse(res);
  return { orders: data };
}

export async function getRelatoriosPorAno(ano) {
  const res = await fetch(`${API_BASE_URL}/relatorios?ano=${ano}`);
  const data = await handleResponse(res);
  const relatorio = Array.isArray(data) ? data[0] : data;
  if (!relatorio) {
    throw new Error("Relatório não encontrado");
  }
  return relatorio;
}

export async function getAssinatura() {
  const res = await fetch(`${API_BASE_URL}/assinatura`);
  const data = await handleResponse(res);
  return data;
}

export async function getUsuario(id) {
  const res = await fetch(`${API_BASE_URL}/usuarios/${id}`);
  return handleResponse(res);
}

export async function getUsuarios(params = {}) {
  const query = new URLSearchParams(params).toString();
  const url = query ? `${API_BASE_URL}/usuarios?${query}` : `${API_BASE_URL}/usuarios`;
  const res = await fetch(url);
  return handleResponse(res);
}
