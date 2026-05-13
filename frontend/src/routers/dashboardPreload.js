const dashboardShellImports = [
  () => import("../components/dashboard/DashboardLayout.jsx"),
  () => import("../pages/dashboard/Dashboard.jsx"),
];

const dashboardRouteImports = [
  () => import("../pages/dashboard/Produtos.jsx"),
  () => import("../pages/dashboard/Pedidos.jsx"),
  () => import("../pages/dashboard/Conexoes.jsx"),
  () => import("../pages/dashboard/Relatorios.jsx"),
  () => import("../pages/dashboard/Configuracoes.jsx"),
  () => import("../pages/dashboard/Assinatura.jsx"),
];

export async function preloadDashboardShell() {
  await Promise.all(dashboardShellImports.map((load) => load()));
}

export function preloadDashboardRoutes() {
  return Promise.all(dashboardRouteImports.map((load) => load()));
}