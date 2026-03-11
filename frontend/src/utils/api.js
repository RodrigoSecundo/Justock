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

function parseCurrencyToNumber(value) {
  if (value == null || value === "") return 0;
  if (typeof value === "number") return Number.isNaN(value) ? 0 : value;

  const text = String(value).trim();
  let normalized = text;

  if (text.includes(",")) {
    normalized = text.replace(/[^\d,.-]/g, "").replace(/\./g, "").replace(",", ".");
  } else {
    normalized = text.replace(/[^\d.-]/g, "");
  }

  const parsed = Number(normalized);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function formatISOToBR(isoDate) {
  if (!isoDate) return "";
  const [year, month, day] = String(isoDate).split("-");
  if (!year || !month || !day) return "";
  return `${day}/${month}/${year}`;
}

function formatDateToISO(dateValue) {
  if (!dateValue) return null;

  if (typeof dateValue === "string") {
    const brDateMatch = dateValue.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (brDateMatch) {
      const [, day, month, year] = brDateMatch;
      return `${year}-${month}-${day}`;
    }

    const isoDateMatch = dateValue.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (isoDateMatch) {
      return `${isoDateMatch[1]}-${isoDateMatch[2]}-${isoDateMatch[3]}`;
    }
  }

  const date = dateValue instanceof Date ? dateValue : new Date(dateValue);
  if (Number.isNaN(date.getTime())) return null;

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function mapOrderMarketplace(idPedidoMarketplace) {
  if (idPedidoMarketplace == null || idPedidoMarketplace === "") return "-";

  const code = Number(idPedidoMarketplace);
  if (Number.isNaN(code)) return String(idPedidoMarketplace);

  if (code === 1) return "Mercado Livre";
  if (code === 2) return "Amazon";
  if (code === 3) return "Shopee";
  if (code === 4) return "Manual";
  return `Marketplace #${code}`;
}

function mapMarketplaceToCode(marketplaceValue) {
  if (marketplaceValue == null || marketplaceValue === "") return null;

  if (typeof marketplaceValue === "number" && !Number.isNaN(marketplaceValue)) {
    return marketplaceValue;
  }

  const normalized = String(marketplaceValue).trim().toLowerCase();
  if (normalized === "1" || normalized === "mercado livre") return 1;
  if (normalized === "2" || normalized === "amazon") return 2;
  if (normalized === "3" || normalized === "shopee") return 3;
  if (normalized === "4" || normalized === "manual") return 4;
  return null;
}

function mapBackendProduct(product) {
  const id = product?.idProduto ?? product?.id ?? 0;
  const categoria = product?.categoria ?? product?.category ?? "-";
  const marca = product?.marca ?? product?.brand ?? "-";
  const nome = product?.nomeDoProduto ?? product?.nome ?? product?.nomeProduto ?? "-";
  const estoque = product?.quantidade ?? product?.estoque ?? product?.stock ?? 0;
  const preco = product?.preco ?? product?.price ?? 0;
  const codigoBarras = product?.codigoDeBarras ?? product?.codigoBarras ?? product?.barcode ?? "-";
  const precoValor = parseCurrencyToNumber(preco);

  return {
    id,
    categoria,
    marca,
    nome,
    estoque,
    preco: typeof preco === "string" && preco.includes("R$") ? preco : formatBRL(precoValor),
    precoValor,
    codigoBarras,
    estado: product?.estado ?? "ATIVO",
    quantidadeReservada: Number(product?.quantidadeReservada ?? 0) || 0,
    marcador: product?.marcador ?? "MANUAL",
    usuario: Number(product?.usuario ?? 1) || 1,
  };
}

function mapBackendOrder(order) {
  return {
    id: order?.idPedido ?? 0,
    idPedidoMarketplace: Number(order?.idPedidoMarketplace ?? 0) || null,
    dataEmissao: formatISOToBR(order?.dataEmissao),
    dataEntrega: formatISOToBR(order?.dataEntrega),
    marketplace: mapOrderMarketplace(order?.idPedidoMarketplace),
    pagamento: order?.statusPagamento ?? "-",
    status: order?.statusPedido ?? "-",
  };
}

export async function getDashboardResumo() {
  const [dashboardRes, productsData] = await Promise.all([
    fetch(`${MOCK_API_BASE_URL}/dashboard`).then(handleResponse),
    fetchBackend(`/api/products/`).catch(() => []),
  ]);

  const products = Array.isArray(productsData) ? productsData : [];
  const totalFromDatabase = products.reduce((acc, product) => {
    const quantity = Number(product?.quantidade ?? product?.estoque ?? 0);
    return acc + (Number.isNaN(quantity) ? 0 : quantity);
  }, 0);

  return {
    total: totalFromDatabase,
    lowStock: dashboardRes.lowStockProducts,
    connectedMarketplaces: dashboardRes.connectedMarketplaces,
    syncStatus: dashboardRes.syncStatus,
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

export async function createProduto(productInput) {
  const payload = {
    categoria: productInput?.categoria ?? "",
    marca: productInput?.marca ?? "",
    nomeDoProduto: productInput?.nome ?? productInput?.nomeDoProduto ?? "",
    estado: productInput?.estado ?? "ATIVO",
    preco: parseCurrencyToNumber(productInput?.preco ?? productInput?.precoValor ?? 0),
    codigoDeBarras: productInput?.codigoBarras ?? productInput?.codigoDeBarras ?? "",
    quantidade: Number(productInput?.estoque ?? productInput?.quantidade ?? 0),
    quantidadeReservada: Number(productInput?.quantidadeReservada ?? 0),
    marcador: productInput?.marcador ?? "MANUAL",
    usuario: Number(productInput?.usuario ?? 1),
  };

  const created = await fetchBackend(`/api/products/cadastrar`, {
    method: "POST",
    body: payload,
  });

  return mapBackendProduct(created);
}

export async function updateProduto(productId, productInput) {
  const payload = {
    categoria: productInput?.categoria ?? "",
    marca: productInput?.marca ?? "",
    nomeDoProduto: productInput?.nome ?? productInput?.nomeDoProduto ?? "",
    estado: productInput?.estado ?? "ATIVO",
    preco: parseCurrencyToNumber(productInput?.preco ?? productInput?.precoValor ?? 0),
    codigoDeBarras: productInput?.codigoBarras ?? productInput?.codigoDeBarras ?? "",
    quantidade: Number(productInput?.estoque ?? productInput?.quantidade ?? 0),
    quantidadeReservada: Number(productInput?.quantidadeReservada ?? 0),
    marcador: productInput?.marcador ?? "MANUAL",
    usuario: Number(productInput?.usuario ?? 1),
  };

  const updated = await fetchBackend(`/api/products/atualizar/${productId}`, {
    method: "PUT",
    body: payload,
  });

  return mapBackendProduct(updated);
}

export async function deleteProduto(productId) {
  await fetchBackend(`/api/products/deletar/${productId}`, {
    method: "DELETE",
  });

  return true;
}

export async function getPedidos() {
  const data = await fetchBackend(`/api/Order/`);
  const orders = Array.isArray(data) ? data.map(mapBackendOrder) : [];
  return { orders };
}

export async function createPedido(orderInput) {
  const marketplaceCode = mapMarketplaceToCode(orderInput?.idPedidoMarketplace ?? orderInput?.marketplace ?? 4);
  if (marketplaceCode == null) {
    throw new Error("Marketplace inválido para criação do pedido.");
  }

  const payload = {
    idPedidoMarketplace: marketplaceCode,
    usuarioMarketplaceId: orderInput?.usuarioMarketplaceId ?? null,
    dataEmissao: formatDateToISO(orderInput?.dataEmissao),
    dataEntrega: formatDateToISO(orderInput?.dataEntrega),
    statusPagamento: orderInput?.pagamento ?? "-",
    statusPedido: orderInput?.status ?? "Pendente",
  };

  const created = await fetchBackend(`/api/Order/cadastrar`, {
    method: "POST",
    body: payload,
  });

  return mapBackendOrder(created);
}

export async function updatePedido(orderId, orderInput) {
  const marketplaceCode = mapMarketplaceToCode(orderInput?.idPedidoMarketplace ?? orderInput?.marketplace);
  if (marketplaceCode == null) {
    throw new Error("Marketplace inválido para atualização do pedido.");
  }

  const payload = {
    idPedidoMarketplace: marketplaceCode,
    usuarioMarketplaceId: orderInput?.usuarioMarketplaceId ?? null,
    dataEmissao: formatDateToISO(orderInput?.dataEmissao),
    dataEntrega: formatDateToISO(orderInput?.dataEntrega),
    statusPagamento: orderInput?.pagamento ?? "-",
    statusPedido: orderInput?.status ?? "Pendente",
  };

  const updated = await fetchBackend(`/api/Order/atualizar/${orderId}`, {
    method: "PUT",
    body: payload,
  });

  return mapBackendOrder(updated);
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
