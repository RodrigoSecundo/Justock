import React, { useEffect, useMemo, useState } from "react";
import { getRelatoriosPorAno } from "../../utils/api";
import "../../styles/pages/dashboard/dashboard.css";
import "../../styles/pages/dashboard/relatorios.css";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js";
import { Line, Doughnut } from "react-chartjs-2";
import { getAccessibilityPrefs } from "../../utils/accessibility";
import { getThemePref } from "../../utils/appearance";
import { srProps } from "../../utils/useA11y";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

const YEARS = [2025, 2024, 2023];
const MARKETPLACES = ["Todas as lojas", "Amazon", "Shopee", "Mercado Livre"];
const LINE_METRICS = [
  { id: "total", label: "Total de vendas" },
  { id: "avg", label: "Valor médio da compra" },
  { id: "conv", label: "Taxa de conversão" },
];

const COLORS = {
  blue: "#2563eb",
  green: "#10b981",
  orange: "#f97316",
  darkTeal: "#0f3d3e",
};

const GREYS = ["#243b53", "#7b8794", "#e4e7ec"];

const DEFAULT_MONTHS = ["Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"];

function sum(arr) { return arr.reduce((a, b) => a + b, 0); }
function formatBRL(v) {
  return v.toLocaleString("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 2 });
}

const Relatorios = () => {
  const [filters, setFilters] = useState({ year: 2025, marketplace: "Todas as lojas" });
  const [raw, setRaw] = useState(null);
  const [lineMetric, setLineMetric] = useState("total");
  const [showExport, setShowExport] = useState(false);
  const [srOpt, setSrOpt] = useState(() => getAccessibilityPrefs().toggleLeitor === true);
  const [isDark, setIsDark] = useState(() => {
    try { return getThemePref() === 'dark'; } catch { return false; }
  });

  useEffect(() => {
    getRelatoriosPorAno(filters.year)
      .then(setRaw)
      .catch(() => setRaw(null));
  }, [filters.year]);

  useEffect(() => {
    const onAcc = (e) => {
      const v = e?.detail?.toggleLeitor;
      if (typeof v === 'boolean') setSrOpt(v);
      else setSrOpt(getAccessibilityPrefs().toggleLeitor === true);
    };
    window.addEventListener('jt:accessibility-updated', onAcc);
    onAcc({ detail: getAccessibilityPrefs() });
    return () => window.removeEventListener('jt:accessibility-updated', onAcc);
  }, []);

  // Tema: reagir à troca para ajustar cores dos gráficos
  useEffect(() => {
    const handleAppearance = (e) => {
      if (e?.detail?.theme) setIsDark(e.detail.theme === 'dark');
  else if (document?.body) setIsDark(document.body.getAttribute('data-theme') === 'dark');
    };
    window.addEventListener('jt:appearance-updated', handleAppearance);
    handleAppearance();
    return () => window.removeEventListener('jt:appearance-updated', handleAppearance);
  }, []);

  const months = useMemo(() => (raw?.months && Array.isArray(raw.months) ? raw.months : DEFAULT_MONTHS), [raw]);

  const agg = useMemo(() => {
    if (!raw) return null;
    const mk = raw.marketplaces;

    const byMkt = Object.fromEntries(Object.keys(mk).map(k => {
      const completed = mk[k].completed;
      const canceled = mk[k].canceled;
      const revenue = mk[k].revenue;
      const totalCompleted = sum(completed);
      const totalCanceled = sum(canceled);
      const totalRevenue = sum(revenue);
      const avg = totalCompleted > 0 ? totalRevenue / totalCompleted : 0;
      const conv = (totalCompleted + totalCanceled) > 0 ? totalCompleted / (totalCompleted + totalCanceled) : 0;
      return [k, { completed, canceled, revenue, totalCompleted, totalCanceled, totalRevenue, avg, conv }];
    }));

    const allCompletedMonthly = months.map((_, i) => sum(Object.values(byMkt).map(m => m.completed[i] || 0)));
    const allRevenueMonthly = months.map((_, i) => sum(Object.values(byMkt).map(m => m.revenue[i] || 0)));
    const allCanceledMonthly = months.map((_, i) => sum(Object.values(byMkt).map(m => m.canceled[i] || 0)));

    const totals = {
      completed: sum(allCompletedMonthly),
      canceled: sum(allCanceledMonthly),
      revenue: sum(allRevenueMonthly),
    };

    const avg = totals.completed > 0 ? totals.revenue / totals.completed : 0;
    const conv = (totals.completed + totals.canceled) > 0 ? totals.completed / (totals.completed + totals.canceled) : 0;
    const categories = (raw.categories || []).slice().sort((a,b) => {
      const av = (typeof a.percent === 'number') ? a.percent : a.sales;
      const bv = (typeof b.percent === 'number') ? b.percent : b.sales;
      return (bv || 0) - (av || 0);
    });
    const topCategory = categories[0]?.name || "-";

    return { byMkt, allCompletedMonthly, allRevenueMonthly, allCanceledMonthly, totals, avg, conv, categories, topCategory };
  }, [raw, months]);

  const kpis = useMemo(() => {
    if (!agg) return { total: 0, avg: 0, conv: 0, destaque: "-" };

    if (filters.marketplace === "Todas as lojas") {
      return { total: agg.totals.completed, avg: agg.avg, conv: agg.conv, destaque: agg.topCategory };
    }
    const mk = agg.byMkt[filters.marketplace];
    return {
      total: mk?.totalCompleted || 0,
      avg: mk?.avg || 0,
      conv: mk ? (mk.totalCompleted + mk.totalCanceled > 0 ? mk.totalCompleted / (mk.totalCompleted + mk.totalCanceled) : 0) : 0,
      destaque: agg.topCategory,
    };
  }, [agg, filters.marketplace]);

  const lineData = useMemo(() => {
    if (!agg) return { labels: months, datasets: [] };

    const buildDataset = (label, color, comp, canc, rev) => {
      if (lineMetric === "total") return { label, data: comp, borderColor: color, backgroundColor: color, tension: 0.35 };
      if (lineMetric === "avg") {
        const avgM = comp.map((v, i) => (v > 0 ? (rev[i] / v) : 0));
        return { label, data: avgM, borderColor: color, backgroundColor: color, tension: 0.35 };
      }
      if (lineMetric === "conv") {
        const convM = comp.map((v, i) => (v + canc[i] > 0 ? v / (v + canc[i]) : 0));
        return { label, data: convM, borderColor: color, backgroundColor: color, tension: 0.35 };
      }
      return { label, data: comp, borderColor: color, backgroundColor: color, tension: 0.35 };
    };

  const sets = [];
  const colors = isDark ? GREYS : [COLORS.blue, COLORS.orange, COLORS.green];
    const names = ["Amazon", "Shopee", "Mercado Livre"];

    if (filters.marketplace === "Todas as lojas") {
      names.forEach((n, idx) => {
        const m = agg.byMkt[n];
        sets.push(buildDataset(n, colors[idx], m.completed, m.canceled, m.revenue));
      });
    } else {
      const m = agg.byMkt[filters.marketplace];
      const idx = names.indexOf(filters.marketplace);
      const color = (isDark ? GREYS : [COLORS.blue, COLORS.orange, COLORS.green])[Math.max(0, idx)];
      sets.push(buildDataset(filters.marketplace, color, m.completed, m.canceled, m.revenue));
    }

    // Aumenta o tamanho dos pontos e a espessura das linhas
    return { labels: months, datasets: sets };
  }, [agg, months, filters.marketplace, lineMetric, isDark]);

  const lineOptions = useMemo(() => {
    const text = isDark ? '#e5eef7' : '#1a3a3a';
    const grid = isDark ? 'rgba(229, 238, 247, 0.12)' : 'rgba(26, 58, 58, 0.12)';
    const border = isDark ? 'rgba(229, 238, 247, 0.22)' : 'rgba(26,58,58,0.22)';
    const tooltipBg = isDark ? 'rgba(15, 22, 32, 0.95)' : '#ffffff';
    const tooltipColor = text;
    return ({
      responsive: true,
      scales: {
        x: {
          ticks: { color: text },
          grid: { color: grid, borderColor: border },
        },
        y: {
          min: lineMetric === 'conv' ? 0 : undefined,
          max: lineMetric === 'conv' ? 1 : undefined,
          ticks: {
            color: text,
            callback: (val) => lineMetric === 'conv' ? Number(val).toFixed(2) : val,
          },
          grid: { color: grid, borderColor: border },
        },
      },
      plugins: {
        legend: { position: 'top', labels: { color: text } },
        tooltip: {
          backgroundColor: tooltipBg,
          titleColor: tooltipColor,
          bodyColor: tooltipColor,
          borderColor: grid,
          borderWidth: 1,
          callbacks: {
            label: (ctx) => {
              const v = ctx.parsed.y;
              if (lineMetric === 'avg') return `${ctx.dataset.label}: ${formatBRL(v)}`;
              if (lineMetric === 'conv') return `${ctx.dataset.label}: ${Number(v).toFixed(2)}`;
              return `${ctx.dataset.label}: ${v.toLocaleString()}`;
            }
          }
        }
      },
      elements: {
        point: {
          radius: 5,
          hoverRadius: 7,
        },
        line: {
          borderWidth: 3,
        },
      },
    });
  }, [lineMetric, isDark]);

  // Top categorias - usamos barras horizontais (aproximação do funil sem dependências extras)
  // Categorias para funil (percentuais 0-100)
  const categoriasPercent = useMemo(() => {
    const cats = agg?.categories || [];
    if (!cats.length) return [];
    const values = cats.map(c => typeof c.percent === 'number' ? c.percent : c.sales);
    const total = values.reduce((a,b)=>a+b,0) || 1;
    // Normaliza para 100 se necessário
    const norm = values.map(v => (v / total) * 100);
    return cats.map((c, i) => ({ name: c.name, percent: Number(norm[i].toFixed(1)) }));
  }, [agg]);

  // Distribuição por marketplace (pizza)
  const pieData = useMemo(() => {
    if (!agg) return { labels: [], datasets: [] };
    const names = ["Amazon", "Shopee", "Mercado Livre"];
    const values = names.map(n => agg.byMkt[n].totalCompleted);
    return {
      labels: names,
      datasets: [{
        data: values,
        backgroundColor: isDark ? ["#9aa6b2", "#6b7785", "#3e4b59"] : [COLORS.blue, COLORS.orange, COLORS.green],
        borderColor: "#ffffff",
      }]
    };
  }, [agg, isDark]);
  const pieOptions = useMemo(() => {
    const text = isDark ? '#e5eef7' : '#1a3a3a';
    const tooltipBg = isDark ? 'rgba(15, 22, 32, 0.95)' : '#ffffff';
    return ({
      responsive: true,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: text,
            generateLabels: (chart) => {
              const dataset = chart.data.datasets[0] || { data: [] };
              const total = dataset.data.reduce((a,b)=>a+b,0) || 1;
              return chart.data.labels.map((label, i) => {
                const value = dataset.data[i] || 0;
                const pct = Math.round((value/total)*100);
                const colorArr = Array.isArray(dataset.backgroundColor) ? dataset.backgroundColor : ['#2563eb', '#f97316', '#10b981']; 
                return {
                  text: `${label} (${pct}%)`,
                  fillStyle: isDark ? '#6b7785' : colorArr[i % colorArr.length],
                  strokeStyle: isDark ? '#3e4b59' : '#fff',
                  lineWidth: 1,
                  hidden: false,
                  index: i,
                  fontColor: text
                };
              });
            }
          }
        },
        tooltip: {
          backgroundColor: tooltipBg,
          titleColor: text,
          bodyColor: text,
          borderColor: isDark ? 'rgba(229,238,247,0.12)' : 'rgba(26,58,58,0.12)',
          borderWidth: 1,
          callbacks: { label: (ctx) => {
            const data = ctx.dataset.data;
            const total = data.reduce((a,b)=>a+b,0) || 1;
            const v = ctx.parsed;
            const pct = ((v/total)*100).toFixed(1);
            return `${ctx.label}: ${v.toLocaleString()} (${pct}%)`;
          }}
        }
      }
    });
  }, [isDark]);

  const applyFilters = (e) => {
    e?.preventDefault?.();
  };

  return (
    <div>

          <div className="relatorios-filtros">
            <div className="filtros-centro">
              <div className="filtro-item">
                <label>Ano de análise:</label>
                <select className="select-ano" value={filters.year} onChange={e => setFilters(f => ({ ...f, year: Number(e.target.value) }))}>
                  {YEARS.map(y => (<option key={y} value={y}>{y}</option>))}
                </select>
              </div>

              <div className="filtro-item">
                <label>Marketplace:</label>
                <select value={filters.marketplace} onChange={e => setFilters(f => ({ ...f, marketplace: e.target.value }))}>
                  {MARKETPLACES.map(m => (<option key={m} value={m}>{m}</option>))}
                </select>
              </div>

              <button className="btn-filtrar" onClick={applyFilters}>Filtrar</button>
            </div>

            <button className="btn-exportar" onClick={() => setShowExport(true)}>Exportar</button>
          </div>

          <div className="relatorios-kpis" role={srOpt ? "region" : undefined} aria-label={srOpt ? "Indicadores principais" : undefined}>
            <div className="kpi-box azul">
              <div className="kpi-title">Total de Vendas</div>
              <div className="kpi-value" aria-live={srOpt ? "polite" : undefined} aria-atomic={srOpt ? "true" : undefined}>{kpis.total.toLocaleString()}</div>
            </div>
            <div className="kpi-box azul">
              <div className="kpi-title">Valor Médio de Venda</div>
              <div className="kpi-value" aria-live={srOpt ? "polite" : undefined} aria-atomic={srOpt ? "true" : undefined}>{formatBRL(kpis.avg || 0)}</div>
            </div>
            <div className="kpi-box azul">
              <div className="kpi-title">Taxa de conversão (concl. x canc.)</div>
              <div className="kpi-value" aria-live={srOpt ? "polite" : undefined} aria-atomic={srOpt ? "true" : undefined}>{(kpis.conv || 0).toFixed(2)}</div>
            </div>
            <div className="kpi-box azul">
              <div className="kpi-title">Categoria Destaque</div>
              <div className="kpi-value" aria-live={srOpt ? "polite" : undefined} aria-atomic={srOpt ? "true" : undefined}>{kpis.destaque}</div>
            </div>
          </div>

          <div className="relatorios-graficos" {...srProps(srOpt, { role: 'region', 'aria-label': 'Gráficos de desempenho' })}>
            <section className="grafico grande" role={srOpt ? "region" : undefined} aria-labelledby={srOpt ? "sec-evolucao" : undefined}>
              <div className="section-header">
                <div className="left">
                  <select className="line-metric" value={lineMetric} onChange={(e) => setLineMetric(e.target.value)}>
                    {LINE_METRICS.map(m => (<option key={m.id} value={m.id}>{m.label}</option>))}
                  </select>
                </div>
              </div>
              {srOpt && <h3 id="sec-evolucao" className="sr-only">Evolução por mês</h3>}
              <Line data={lineData} options={lineOptions} />
            </section>

            <section className="grafico" role={srOpt ? "region" : undefined} aria-labelledby={srOpt ? "sec-top-categorias" : undefined}>
              <div className="section-header"><h3 id="sec-top-categorias">Top Categorias</h3></div>
              <Funnel data={categoriasPercent} isDark={isDark} />
            </section>

            <section className="grafico" role={srOpt ? "region" : undefined} aria-labelledby={srOpt ? "sec-distribuicao" : undefined}>
              <div className="section-header"><h3 id="sec-distribuicao">Distribuição de Vendas</h3></div>
              <Doughnut data={pieData} options={pieOptions} />
            </section>
          </div>
      
      {showExport && (
        <div className="modal-sobreposicao">
          <div className="modal-conteudo export-modal">
            <p>Exportar Relatório</p>
            <div className="modal-botoes">
              <button className="btn-excel" onClick={() => { setShowExport(false); import('../../utils/notify').then(m=>m.notifySuccess('Exportação EXCEL iniciada')); }}>EXCEL</button>
              <button className="btn-csv" onClick={() => { setShowExport(false); import('../../utils/notify').then(m=>m.notifySuccess('Exportação CSV iniciada')); }}>CSV</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Relatorios;

const Funnel = ({ data, isDark }) => {
  if (!data || !data.length) return null;
  const minWidth = 42;
  const heights = 12;
  const gap = 2;
  const n = data.length;
  const totalHeight = n * heights + (n - 1) * gap;

  const maxPct = Math.max(...data.map(d => d.percent || 0), 1);
  const widthsNorm = data.map(d => {
    const norm = ((d.percent || 0) / maxPct) * 100;
    return Math.max(minWidth, Math.min(100, norm));
  });
  const widths = widthsNorm.reduce((acc, w, i) => {
    if (i === 0) return [w];
    const prev = acc[i - 1];
    const minDrop = 4; 
    const maxDrop = 10; 
    const intended = Math.min(w, prev - minDrop);
    const lower = prev - maxDrop;
    const upper = prev - minDrop;
    const clamped = Math.max(lower, Math.min(intended, upper));
    const adjusted = Math.max(minWidth, clamped);
    acc.push(adjusted);
    return acc;
  }, []);

  if (widths.length >= 3) {
    const idxFontes = widths.length - 3;
    const idxCoolers = widths.length - 2;
    const idxOutros = widths.length - 1;

    const targetCoolers = Math.min(widths[idxCoolers], widths[idxFontes] - 8);
    widths[idxCoolers] = Math.max(36, targetCoolers);

    const targetOutros = Math.min(widths[idxOutros], widths[idxCoolers] - 6);
    widths[idxOutros] = Math.max(30, targetOutros);
  }

  // Funil em escala de cinza puro no modo escuro, sem repetição
  const funnelGreys = [
    "#020617",
    "#111827",
    "#1f2937",
    "#374151",
    "#4b5563",
    "#6b7280",
    "#9ca3af",
    "#d1d5db",
    "#e5e7eb",
  ];

  const colors = isDark
    ? data.map((_, idx) => funnelGreys[idx % funnelGreys.length])
    : [COLORS.blue, COLORS.orange, COLORS.green, COLORS.darkTeal];

  const rows = widths.map((w, i) => {
  const next = i < n - 1 ? widths[i + 1] : Math.max(10, w * 0.6);
    const topW = w;
    const botW = next;
    const leftTop = (100 - topW) / 2;
    const rightTop = leftTop + topW;
    const leftBot = (100 - botW) / 2;
    const rightBot = leftBot + botW;
  const yTop = i * (heights + gap);
  const yBot = yTop + heights;
    const color = colors[i % colors.length];

    const points = `${leftTop},${yTop} ${rightTop},${yTop} ${rightBot},${yBot} ${leftBot},${yBot}`;
    const labelY = yTop + heights * 0.62;
    return { points, color, label: `${data[i].name} ${data[i].percent}%`, labelY };
  });

  return (
    <svg className="funnel-svg" viewBox={`0 0 100 ${totalHeight}`} preserveAspectRatio="xMidYMid meet">
      {rows.map((r, idx) => (
        <g key={idx}>
          <polygon
            className="funnel-seg"
            points={r.points}
            fill={r.color}
            opacity="0.98"
            stroke="#111111"
            strokeWidth="1.2"
          />
          <text
            x="50"
            y={r.labelY}
            textAnchor="middle"
            className="funnel-text"
            stroke="#000000"
            strokeWidth="0.4"
            paintOrder="stroke"
          >
            {r.label}
          </text>
        </g>
      ))}
    </svg>
  );
};
