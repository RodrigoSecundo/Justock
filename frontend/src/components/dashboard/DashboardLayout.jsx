import React from 'react';
import { Outlet } from 'react-router-dom';
import BarraLateral from './BarraLateral';
import BarraSuperior from './BarraSuperior';

// Layout persistente para rotas do dashboard. Mantém a barra lateral montada
// evitando refresh completo em modo "mista" quando a rota muda.
// As páginas internas devem renderizar apenas o conteúdo dentro de .main-content
// sem repetir estrutura de painel.
const DashboardLayout = () => {
  return (
    <div className="painel-container">
      <BarraLateral />
      <main className="painel-principal">
        <BarraSuperior />
        <div className="main-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
};

export default DashboardLayout;