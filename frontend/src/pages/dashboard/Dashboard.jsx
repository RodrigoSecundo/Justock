import React, { useEffect, useMemo, useState } from "react";
import { FiCheckCircle, FiAlertTriangle, FiXCircle } from "react-icons/fi";
import "../../styles/pages/dashboard/dashboard.css";

import { getDashboardResumo, getDashboardInventoryOverview, getDashboardRecentActivity, getDashboardAlerts } from "../../utils/api";
import { Bar } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js";
import { getAccessibilityPrefs } from "../../utils/accessibility";
import { getThemePref } from "../../utils/appearance";
import { useSrOptimized, srProps } from "../../utils/useA11y";

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

const GREY_PALETTE = [
  "rgba(154, 166, 178, 0.85)",
  "rgba(107, 119, 133, 0.85)",
  "rgba(62, 75, 89, 0.85)",
  "rgba(43, 53, 66, 0.85)",
];
const DEFAULT_PALETTE = [
  "rgba(173, 214, 0, 0.8)",
  "rgba(0, 0, 255, 0.8)",
  "rgba(0, 51, 51, 0.8)",
  "rgba(255, 99, 71, 0.8)",
];

const Dashboard = () => {
  const srOpt = useSrOptimized();
  const [totalProducts, setTotalProducts] = useState(0);
  const [lowStockProducts, setLowStockProducts] = useState(0);
  const [connectedMarketplaces, setConnectedMarketplaces] = useState(0);
  const [syncStatus, setSyncStatus] = useState("OFF");
  const [chartFontWeight, setChartFontWeight] = useState(() => (getAccessibilityPrefs().altoContraste ? "bold" : "normal"));

  const [chartData, setChartData] = useState(() => {
    const dark = (() => { try { return getThemePref() === 'dark'; } catch { return false; } })();
    return {
      labels: ["Placas-mãe", "Processadores", "Fontes", "Placas de vídeo"],
      datasets: [
        {
          label: "Quantidade",
          data: [0, 0, 0, 0],
          backgroundColor: dark ? GREY_PALETTE : DEFAULT_PALETTE,
          borderRadius: { topLeft: 8, topRight: 8, bottomLeft: 0, bottomRight: 0 },
          borderSkipped: 'bottom',
        },
      ],
    };
  });

  // Os dados de recentActivity e alerts virão do backend futuramente.
  // Os tipos podem ser: validado, reembolsado, novo, sincronizado, baixo, fora-estoque, atualizado, etc.
  // Os ícones podem ser definidos no frontend conforme o tipo, por exemplo:
  // validado/novo/sincronizado/atualizado: verificado, baixo/reembolsado: exclamação, fora-estoque: X, etc.
  const [recentActivity, setRecentActivity] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [isDark, setIsDark] = useState(() => {
    try { return getThemePref() === 'dark'; } catch { return false; }
  });

  // Carrega dados dependentes de tema (paleta) sempre que 'isDark' alterar
  useEffect(() => {
    let cancelled = false;
    getDashboardInventoryOverview()
      .then((data) => {
        if (cancelled) return;
        setChartData({
          labels: (data && Array.isArray(data.labels)) ? data.labels : [],
          datasets: [
            {
              label: "Quantidade",
              data: (data && Array.isArray(data.values)) ? data.values : [],
              backgroundColor: isDark ? GREY_PALETTE : DEFAULT_PALETTE,
              borderRadius: { topLeft: 8, topRight: 8, bottomLeft: 0, bottomRight: 0 },
              borderSkipped: 'bottom',
            },
          ],
        });
      });
    return () => { cancelled = true; };
  }, [isDark]);

  useEffect(() => {
    // Ajusta fontes do Chart conforme alto contraste
    const applyA11yToCharts = (isHigh) => {
      try {
        ChartJS.defaults.font = ChartJS.defaults.font || {};
        ChartJS.defaults.font.weight = isHigh ? "bold" : "normal";
      } catch { /* para aqui */ }
      setChartFontWeight(isHigh ? "bold" : "normal");
    };
    applyA11yToCharts(getAccessibilityPrefs().altoContraste);
    const handler = (e) => applyA11yToCharts(!!e?.detail?.altoContraste);
    window.addEventListener('jt:accessibility-updated', handler);

    getDashboardResumo()
      .then((data) => {
        setTotalProducts(typeof data.total === "number" ? data.total : 0);
        setLowStockProducts(typeof data.lowStock === "number" ? data.lowStock : 0);
        setConnectedMarketplaces(typeof data.connectedMarketplaces === "number" ? data.connectedMarketplaces : 0);
        setSyncStatus(typeof data.syncStatus === "string" ? data.syncStatus : "OFF");
      })
      .catch(() => {
        setTotalProducts(0);
        setLowStockProducts(0);
        setConnectedMarketplaces(0);
        setSyncStatus("OFF");
      });

    // inventory-overview é carregado em um effect separado que respeita 'isDark'

    getDashboardRecentActivity()
      .then((data) => setRecentActivity(Array.isArray(data.activities) ? data.activities : []))
      .catch(() => setRecentActivity([]));

    getDashboardAlerts()
      .then((data) => setAlerts(Array.isArray(data.alerts) ? data.alerts : []))
      .catch(() => setAlerts([]));
    const onAppearance = (e) => {
      if (e?.detail?.theme) setIsDark(e.detail.theme === 'dark');
      else if (document?.body) setIsDark(document.body.getAttribute('data-theme') === 'dark');
    };
    window.addEventListener('jt:appearance-updated', onAppearance);
    onAppearance();
    return () => {
      window.removeEventListener('jt:accessibility-updated', handler);
      window.removeEventListener('jt:appearance-updated', onAppearance);
    };
  }, []);

  const barOptions = useMemo(() => {
    const text = isDark ? '#e5eef7' : '#1a3a3a';
    const grid = isDark ? 'rgba(229,238,247,0.12)' : 'rgba(26,58,58,0.12)';
    const border = isDark ? 'rgba(229,238,247,0.22)' : 'rgba(26,58,58,0.22)';
    return {
      responsive: true,
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { color: text, font: { weight: chartFontWeight } }, grid: { color: grid, borderColor: border } },
        y: { ticks: { color: text, font: { weight: chartFontWeight } }, grid: { color: grid, borderColor: border } },
      },
    };
  }, [isDark, chartFontWeight]);

  return (
    <div {...srProps(srOpt, { role: 'main', 'aria-label': 'Conteúdo principal do dashboard' })}>
          <div className="caixas-info" {...srProps(srOpt, { role: 'region', 'aria-label': 'Indicadores principais' })}>
            <div className="caixa-info azul" {...srProps(srOpt, { role: 'group', 'aria-label': `Total de Produtos: ${totalProducts.toLocaleString()}` })}>
              <div>Total de Produtos</div>
              <div className="valor-info" {...srProps(srOpt, { 'aria-live': 'polite', 'aria-atomic': 'true' })}>{totalProducts.toLocaleString()}</div>
            </div>
            <div className="caixa-info laranja" {...srProps(srOpt, { role: 'group', 'aria-label': `Produtos em Baixa: ${lowStockProducts}` })}>
              <div>Produtos em Baixa</div>
              <div className="valor-info" {...srProps(srOpt, { 'aria-live': 'polite', 'aria-atomic': 'true' })}>{lowStockProducts}</div>
            </div>
            <div className="caixa-info teal-escuro" {...srProps(srOpt, { role: 'group', 'aria-label': `Marketplaces Conectadas: ${connectedMarketplaces}` })}>
              <div>Marketplaces Conectadas</div>
              <div className="valor-info" {...srProps(srOpt, { 'aria-live': 'polite', 'aria-atomic': 'true' })}>{connectedMarketplaces}</div>
            </div>
            <div className="caixa-info verde-claro" {...srProps(srOpt, { role: 'group', 'aria-label': `Status da Sincronização: ${syncStatus}` })}>
              <div>Status da Sincronização</div>
              <div className="valor-info" {...srProps(srOpt, { 'aria-live': 'polite', 'aria-atomic': 'true' })}>{syncStatus}</div>
            </div>
          </div>

          <div className="conteudo-painel">
            <section className="visao-inventario" {...srProps(srOpt, { role: 'region', 'aria-labelledby': 'sec-visao-inventario' })}>
              <div className="cabecalho-secao">
                <h2 id="sec-visao-inventario">Visão Geral do Inventário</h2>
                <a href="#">ver mais {'>'}</a>
              </div>
              <Bar data={chartData} options={barOptions} />
            </section>

            <section className="secoes-lateral">
              <div className="atividade-recente caixa-secao">
                <h3>Atividade Recente</h3>
                <ul>
                  {recentActivity.map((item, index) => (
                    <li key={index} className={`item-atividade ${item.type}`}>
                      <span className="icone-atividade" aria-hidden="true">
                        {item.type === 'reembolsado' ? <FiAlertTriangle size={18} /> : <FiCheckCircle size={18} />}
                      </span>
                      <span className="texto-atividade">{item.text}</span>
                      <span className="tempo-atividade">{item.time}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="alertas caixa-secao">
                <h3>Alertas</h3>
                <ul>
                  {alerts.map((alert, index) => (
                    <li key={index} className={`item-alerta ${alert.type}`}>
                      <span className="icone-alerta" aria-hidden="true">
                        {alert.type === 'fora-estoque' ? <FiXCircle size={18} /> : alert.type === 'atualizado' ? <FiCheckCircle size={18} /> : <FiAlertTriangle size={18} />}
                      </span>
                      <span className="texto-alerta">{alert.text}</span>
                      <span className="tempo-alerta">{alert.time}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </section>
          </div>
    </div>
  );
};

export default Dashboard;
