const dashboardShellImports = [
  () => import("../components/dashboard/DashboardLayout.jsx?v=20260514-6"),
  () => import("../pages/dashboard/Dashboard.jsx?v=20260514-6"),
];

const dashboardRouteImports = [
  () => import("../pages/dashboard/Produtos.jsx?v=20260514-6"),
  () => import("../pages/dashboard/Pedidos.jsx?v=20260514-6"),
  () => import("../pages/dashboard/Conexoes.jsx?v=20260514-6"),
  () => import("../pages/dashboard/Relatorios.jsx?v=20260514-6"),
  () => import("../pages/dashboard/Configuracoes.jsx?v=20260514-6"),
  () => import("../pages/dashboard/Assinatura.jsx?v=20260514-6"),
];

export async function preloadDashboardShell() {
  await Promise.all(dashboardShellImports.map((load) => load()));
}

export function preloadDashboardRoutes() {
  return Promise.all(dashboardRouteImports.map((load) => load()));
}