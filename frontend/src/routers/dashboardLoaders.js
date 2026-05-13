import { lazy } from "react";

const lazyRoute = (loader) => {
  const Component = lazy(loader);
  Component.preload = loader;
  return Component;
};

export const DashboardLayoutRoute = lazyRoute(() => import("../components/dashboard/DashboardLayout.jsx"));
export const DashboardRoute = lazyRoute(() => import("../pages/dashboard/Dashboard.jsx"));
export const ProdutosRoute = lazyRoute(() => import("../pages/dashboard/Produtos.jsx"));
export const PedidosRoute = lazyRoute(() => import("../pages/dashboard/Pedidos.jsx"));
export const ConexoesRoute = lazyRoute(() => import("../pages/dashboard/Conexoes.jsx"));
export const RelatoriosRoute = lazyRoute(() => import("../pages/dashboard/Relatorios.jsx"));
export const ConfiguracoesRoute = lazyRoute(() => import("../pages/dashboard/Configuracoes.jsx"));
export const AssinaturaRoute = lazyRoute(() => import("../pages/dashboard/Assinatura.jsx"));