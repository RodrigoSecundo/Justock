import React, { Suspense, useLayoutEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, useLocation } from "react-router-dom";
import BarraNavegacao from "../pages/home/barra_navegacao.jsx";
import TopoHome from "../pages/home/topo_home.jsx";
import RecursosDisponiveis from "../pages/home/recursos_home.jsx";
import SobreNos from "../pages/home/sobre_nos.jsx";
import RodapeHome from "../pages/home/rodape_home.jsx";
import PlanosModal from "../pages/home/planos_modal.jsx";
import Login from "../pages/login/Login.jsx";
import ErrorBoundary from "../components/common/ErrorBoundary.jsx";
import { applyAppearance } from "../utils/appearance.js";
import {
  AssinaturaRoute,
  ConfiguracoesRoute,
  ConexoesRoute,
  DashboardLayoutRoute,
  DashboardRoute,
  PedidosRoute,
  ProdutosRoute,
  RelatoriosRoute,
} from "./dashboardLoaders.js";

const DashboardScope = ({ children }) => {
  const location = useLocation();
  useLayoutEffect(() => {
    const paths = ["/dashboard", "/conexoes", "/produtos", "/pedidos", "/relatorios", "/configuracoes", "/assinatura"]; 
    const isDash = paths.some(p => location.pathname.startsWith(p));
    if (typeof document !== 'undefined') {
      document.body.classList.toggle('dashboard-scope', isDash);
    }
    try { applyAppearance(); } catch { /* para aqui */ }
  }, [location]);
  return children;
};

const RouteFallback = ({ label }) => (
  <div
    style={{
      minHeight: "100vh",
      display: "grid",
      placeItems: "center",
      padding: "2rem",
      background: "#f5f9fa",
      color: "#1a3a3a",
    }}
    role="status"
    aria-live="polite"
  >
    <div>{label}</div>
  </div>
);

const DashboardPage = ({ children }) => (
  <Suspense fallback={<RouteFallback label="Carregando painel..." />}>
    <ErrorBoundary>{children}</ErrorBoundary>
  </Suspense>
);

const Routs = () => {
  const [planosOpen, setPlanosOpen] = useState(false);
  const openPlanos = () => setPlanosOpen(true);
  const closePlanos = () => setPlanosOpen(false);

  return (
    <Router>
      <DashboardScope>
      <div className="app-container">
        <Routes>
          <Route
            path="/"
            element={
              <>
                <BarraNavegacao onOpenPlanos={openPlanos} />
                <TopoHome onOpenPlanos={openPlanos} />
                <RecursosDisponiveis />
                <SobreNos />
                <RodapeHome />
                <PlanosModal open={planosOpen} onClose={closePlanos} />
              </>
            }
          />
          <Route path="/login" element={<Login />} />
          <Route
            element={
              <Suspense fallback={<RouteFallback label="Preparando painel..." />}>
                <DashboardLayoutRoute />
              </Suspense>
            }
          >
            <Route path="/dashboard" element={<DashboardPage><DashboardRoute /></DashboardPage>} />
            <Route path="/dashboard/conexoes" element={<DashboardPage><ConexoesRoute /></DashboardPage>} />
            <Route path="/conexoes" element={<DashboardPage><ConexoesRoute /></DashboardPage>} />
            <Route path="/produtos" element={<DashboardPage><ProdutosRoute /></DashboardPage>} />
            <Route path="/pedidos" element={<DashboardPage><PedidosRoute /></DashboardPage>} />
            <Route path="/relatorios" element={<DashboardPage><RelatoriosRoute /></DashboardPage>} />
            <Route path="/configuracoes" element={<DashboardPage><ConfiguracoesRoute /></DashboardPage>} />
            <Route path="/assinatura" element={<DashboardPage><AssinaturaRoute /></DashboardPage>} />
          </Route>
        </Routes>
      </div>
      </DashboardScope>
    </Router>
  );
};

export default Routs;
