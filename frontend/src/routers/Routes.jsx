import React, { useLayoutEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, useLocation } from "react-router-dom";
import BarraNavegacao from "../pages/home/barra_navegacao.jsx";
import TopoHome from "../pages/home/topo_home.jsx";
import RecursosDisponiveis from "../pages/home/recursos_home.jsx";
import SobreNos from "../pages/home/sobre_nos.jsx";
import RodapeHome from "../pages/home/rodape_home.jsx";
import PlanosModal from "../pages/home/planos_modal.jsx";
import Login from "../pages/login/Login.jsx";
import Dashboard from "../pages/dashboard/Dashboard.jsx";
import Produtos from "../pages/dashboard/Produtos.jsx";
import Pedidos from "../pages/dashboard/Pedidos.jsx";
import Conexoes from "../pages/dashboard/Conexoes.jsx";
import Relatorios from "../pages/dashboard/Relatorios.jsx";
import Configuracoes from "../pages/dashboard/Configuracoes.jsx";
import Assinatura from "../pages/dashboard/Assinatura.jsx";
import DashboardLayout from "../components/dashboard/DashboardLayout.jsx";
import ErrorBoundary from "../components/common/ErrorBoundary.jsx";
import { applyAppearance } from "../utils/appearance.js";

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
          <Route element={<DashboardLayout />}> 
            <Route path="/dashboard" element={<ErrorBoundary><Dashboard /></ErrorBoundary>} />
            <Route path="/conexoes" element={<ErrorBoundary><Conexoes /></ErrorBoundary>} />
            <Route path="/produtos" element={<ErrorBoundary><Produtos /></ErrorBoundary>} />
            <Route path="/pedidos" element={<ErrorBoundary><Pedidos /></ErrorBoundary>} />
            <Route path="/relatorios" element={<ErrorBoundary><Relatorios /></ErrorBoundary>} />
            <Route path="/configuracoes" element={<ErrorBoundary><Configuracoes /></ErrorBoundary>} />
            <Route path="/assinatura" element={<ErrorBoundary><Assinatura /></ErrorBoundary>} />
          </Route>
        </Routes>
      </div>
      </DashboardScope>
    </Router>
  );
};

export default Routs;
