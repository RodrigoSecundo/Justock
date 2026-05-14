import { lazy } from "react";

const lazyRoute = (loader) => {
  const Component = lazy(loader);
  Component.preload = loader;
  return Component;
};

export const DashboardLayoutRoute = lazyRoute(() => import("../components/dashboard/DashboardLayout.jsx?v=20260514-6"));
export const DashboardRoute = lazyRoute(() => import("../pages/dashboard/Dashboard.jsx?v=20260514-6"));
export const ProdutosRoute = lazyRoute(() => import("../pages/dashboard/Produtos.jsx?v=20260514-6"));
export const PedidosRoute = lazyRoute(() => import("../pages/dashboard/Pedidos.jsx?v=20260514-6"));
export const ConexoesRoute = lazyRoute(() => import("../pages/dashboard/Conexoes.jsx?v=20260514-6"));
export const RelatoriosRoute = lazyRoute(() => import("../pages/dashboard/Relatorios.jsx?v=20260514-6"));
export const ConfiguracoesRoute = lazyRoute(() => import("../pages/dashboard/Configuracoes.jsx?v=20260514-6"));
export const AssinaturaRoute = lazyRoute(() => import("../pages/dashboard/Assinatura.jsx?v=20260514-6"));