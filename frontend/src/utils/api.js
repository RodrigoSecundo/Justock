import { getAuthToken } from "./auth";

const MOCK_API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:3001";
const BACKEND_API_BASE_URL = import.meta.env.VITE_BACKEND_API_BASE_URL || "http://localhost:8080";

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

async function fetchBackend(path, { method = "GET", body } = {}) {
  const token = getAuthToken();
  const headers = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${BACKEND_API_BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const payload = await handleResponse(res);
  return normalizeApiEnvelope(payload);
}

function formatBRL(value) {
  const number = Number(value || 0);
  if (Number.isNaN(number)) {
    return "R$ 0,00";
  }
  return number.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatISOToBR(isoDate) {
  if (!isoDate) return "";
  const [year, month, day] = String(isoDate).split("-");
  if (!year || !month || !day) return "";
  return `${day}/${month}/${year}`;
}

function mapBackendProduct(product) {
  const id = product?.idProduto ?? product?.id ?? 0;
  const categoria = product?.categoria ?? product?.category ?? "-";
  const marca = product?.marca ?? product?.brand ?? "-";
  const nome = product?.nomeDoProduto ?? product?.nome ?? product?.nomeProduto ?? "-";
  const estoque = product?.quantidade ?? product?.estoque ?? product?.stock ?? 0;
  const preco = product?.preco ?? product?.price ?? 0;
  const codigoBarras = product?.codigoDeBarras ?? product?.codigoBarras ?? product?.barcode ?? "-";

  return {
    id,
    categoria,
    marca,
    nome,
    estoque,
    preco: typeof preco === "string" && preco.includes("R$") ? preco : formatBRL(preco),
    codigoBarras,
  };
}

function mapBackendOrder(order) {
  return {
    id: order?.idPedido ?? 0,
    dataEmissao: formatISOToBR(order?.dataEmissao),
    dataEntrega: formatISOToBR(order?.dataEntrega),
    marketplace: order?.usuarioMarketplaceId != null ? `Marketplace #${order.usuarioMarketplaceId}` : "-",
    pagamento: order?.statusPagamento ?? "-",
    status: order?.statusPedido ?? "-",
  };
}

export async function getDashboardResumo() {
  const res = await fetch(`${MOCK_API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return {
    total: data.totalProducts,
    lowStock: data.lowStockProducts,
    connectedMarketplaces: data.connectedMarketplaces,
    syncStatus: data.syncStatus,
  };
}

export async function getDashboardInventoryOverview() {
  const res = await fetch(`${MOCK_API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return data.inventoryOverview;
}

export async function getDashboardRecentActivity() {
  const res = await fetch(`${MOCK_API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return { activities: data.recentActivity };
}

export async function getDashboardAlerts() {
  const res = await fetch(`${MOCK_API_BASE_URL}/dashboard`);
  const data = await handleResponse(res);
  return { alerts: data.alerts };
}

export async function getConexoes() {
  const res = await fetch(`${MOCK_API_BASE_URL}/conexoes`);
  const data = await handleResponse(res);
  return { marketplaces: data };
}

export async function getProdutos() {
  const data = await fetchBackend(`/api/products/`);
  const products = Array.isArray(data) ? data.map(mapBackendProduct) : [];
  return { products };
}

export async function getPedidos() {
  const data = await fetchBackend(`/api/Order/`);
  const orders = Array.isArray(data) ? data.map(mapBackendOrder) : [];
  return { orders };
}

export async function getRelatoriosPorAno(ano) {
  const res = await fetch(`${MOCK_API_BASE_URL}/relatorios?ano=${ano}`);
  const data = await handleResponse(res);
  const relatorio = Array.isArray(data) ? data[0] : data;
  if (!relatorio) {
    throw new Error("Relatório não encontrado");
  }
  return relatorio;
}

export async function getAssinatura() {
  const res = await fetch(`${MOCK_API_BASE_URL}/assinatura`);
  const data = await handleResponse(res);
  return data;
}

export async function getUsuario(id) {
  const res = await fetch(`${MOCK_API_BASE_URL}/usuarios/${id}`);
  return handleResponse(res);
}

export async function getUsuarios(params = {}) {
  const query = new URLSearchParams(params).toString();
  const url = query ? `${MOCK_API_BASE_URL}/usuarios?${query}` : `${MOCK_API_BASE_URL}/usuarios`;
  const res = await fetch(url);
  return handleResponse(res);
}
