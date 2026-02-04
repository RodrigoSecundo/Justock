import React, { useState, useEffect } from "react";
import { getPedidos } from "../../utils/api";
import { Calendar } from "primereact/calendar";
import "../../styles/pages/dashboard/dashboard.css";
import "../../styles/pages/dashboard/pedidos.css";
import { useSrOptimized, srProps } from "../../utils/useA11y";
import { notifySuccess, notifyError } from "../../utils/notify";
import DialogoReutilizavel from "../../components/common/DialogoReutilizavel";
import { InputText } from "primereact/inputtext";
import { Dropdown } from "primereact/dropdown";
import { Button } from "primereact/button";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";

const Pedidos = () => {
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [newOrder, setNewOrder] = useState({ dataEmissao: null, dataEntrega: null, marketplace: "", pagamento: "Dinheiro", status: "Pendente" });
  const [observation, setObservation] = useState("");
  const [orders, setOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [sortField, setSortField] = useState(null);
  const [sortOrder, setSortOrder] = useState(null);
  const srOpt = useSrOptimized();
  const [filters, setFilters] = useState({
    search: "",
    period: null,
    marketplace: "Todos",
    status: "Todos",
  });

  useEffect(() => {
    let data = [...orders];
    if (sortField && sortOrder) {
      data.sort((a, b) => {
        const av = a[sortField];
        const bv = b[sortField];
        if (av == null && bv == null) return 0;
        if (av == null) return -1 * sortOrder;
        if (bv == null) return 1 * sortOrder;
        if (typeof av === 'number' && typeof bv === 'number') {
          return (av - bv) * sortOrder;
        }
        return String(av).localeCompare(String(bv)) * sortOrder;
      });
    }
    setFilteredOrders(data);
  }, [orders, sortField, sortOrder]);

  useEffect(() => {
    getPedidos()
      .then((data) => {
        if (data && Array.isArray(data.orders)) setOrders(data.orders);
        else setOrders([]);
      })
      .catch(() => {
        setOrders([]);
      });
  }, []);

  const totalPages = Math.ceil(filteredOrders.length / itemsPerPage);

  const normalizeDate = (d) => {
    if (!d) return null;
    const nd = new Date(d);
    nd.setHours(0, 0, 0, 0);
    return nd;
  };
  const parseBRDate = (br) => {
    if (!br) return null;
    const [day, month, year] = br.split('/').map((v) => parseInt(v, 10));
    const dt = new Date(year, (month || 1) - 1, day || 1);
    dt.setHours(0, 0, 0, 0);
    return isNaN(dt.getTime()) ? null : dt;
  };

  const applyFilters = () => {
    let filtered = orders;

    if (filters.search) {
      filtered = filtered.filter((order) => order.id.toString().includes(filters.search));
    }

    const [startDate, endDate] = filters.period || [];
    const start = normalizeDate(startDate);
    const end = normalizeDate(endDate);
    if (start || end) {
      filtered = filtered.filter((order) => {
        const od = parseBRDate(order.dataEmissao);
        if (!od) return false;
        const afterStart = start ? od.getTime() >= start.getTime() : true;
        const beforeEnd = end ? od.getTime() <= end.getTime() : true;
        return afterStart && beforeEnd;
      });
    }

    if (filters.marketplace !== "Todos") {
      filtered = filtered.filter((order) => order.marketplace === filters.marketplace);
    }
    if (filters.status !== "Todos") {
      filtered = filtered.filter((order) => order.status === filters.status);
    }

    setFilteredOrders(filtered);
    setCurrentPage(1);
  };

  const clearFilters = () => {
    setFilters({ search: "", period: null, marketplace: "Todos", status: "Todos" });
    setFilteredOrders(orders);
    setCurrentPage(1);
  };

  const handleRowClick = (order) => {
    setSelectedOrder(order);
    setObservation("");
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setSelectedOrder(null);
    setObservation("");
  };

  const saveObservation = () => {
    if (!selectedOrder) return;
    const updated = orders.map(o =>
      o.id === selectedOrder.id ? { ...o, observacao: observation } : o
    );
    setOrders(updated);
    setTimeout(() => applyFilters(), 0);
    notifySuccess(`Observação salva para o pedido #${selectedOrder.id}.`);
    closeModal();
  };

  // Utilidades
  const toBR = (d) => {
    if (!d) return "";
    const dd = new Date(d);
    if (Number.isNaN(dd.getTime())) return "";
    const day = String(dd.getDate()).padStart(2, '0');
    const mon = String(dd.getMonth() + 1).padStart(2, '0');
    const yr = dd.getFullYear();
    return `${day}/${mon}/${yr}`;
  };
  const nextOrderId = () => {
    const ids = orders.map(o => Number(o.id) || 0);
    const max = ids.length ? Math.max(...ids) : 0;
    return max + 1;
  };
  const openAdd = () => {
    setNewOrder({ dataEmissao: null, dataEntrega: null, marketplace: "", pagamento: "Dinheiro", status: "Pendente" });
    setAddOpen(true);
  };
  const cancelAdd = () => setAddOpen(false);
  const confirmAdd = () => {
    if (!newOrder.dataEmissao) {
      notifyError('Data de emissão é obrigatória.');
      return;
    }
    const order = {
      id: nextOrderId(),
      dataEmissao: toBR(newOrder.dataEmissao),
      dataEntrega: newOrder.dataEntrega ? toBR(newOrder.dataEntrega) : "",
      marketplace: newOrder.marketplace || "-",
      pagamento: newOrder.pagamento,
      status: newOrder.status,
    };
    const updated = [order, ...orders];
    setOrders(updated);
    setTimeout(() => applyFilters(), 0);
  setAddOpen(false);
  notifySuccess(`Pedido #${order.id} adicionado.`);
  };

  const handleFilterChange = (key, value) => {
    if (key === "period") {
      // Garante que início <= final quando as duas datas forem escolhidas
      let normalized = value;
      if (Array.isArray(value) && value[0] && value[1]) {
        const a = normalizeDate(value[0]);
        const b = normalizeDate(value[1]);
        normalized = a && b && a.getTime() > b.getTime() ? [b, a] : [a, b];
      }
      setFilters((prev) => ({ ...prev, period: normalized }));
    } else {
      setFilters({ ...filters, [key]: value });
    }
  };

  const handleSort = (e) => {
    if (!e.sortField) return;
    if (sortField !== e.sortField) {
      setSortField(e.sortField);
      setSortOrder(1);
      return;
    }
    if (sortOrder === 1) {
      setSortOrder(-1);
    } else if (sortOrder === -1) {
      setSortField(null);
      setSortOrder(null);
    } else {
      setSortOrder(1);
    }
  };

  return (
    <div {...srProps(srOpt, { role: 'main', 'aria-label': 'Lista de pedidos' })}>
          <div className="pedidos-header prime-filtro">
            <div className="filter-group">
              <label htmlFor="filtro-pedido">Nº Pedido:</label>
              <InputText
                className="filter-search"
                value={filters.search}
                onChange={(e) => handleFilterChange("search", e.target.value)}
                id="filtro-pedido"
                placeholder="Buscar por Nº Pedido"
              />
            </div>

            <div className="filter-group">
              <label htmlFor="filtro-periodo">Período:</label>
              <Calendar
                inputId="filtro-periodo"
                value={filters.period}
                onChange={(e) => handleFilterChange("period", e.value)}
                selectionMode="range"
                dateFormat="dd/mm/yy"
                placeholder="Selecionar"
                className="filter-date-range"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
              />
            </div>

            <div className="filter-group">
              <label htmlFor="filtro-loja">Lojas:</label>
              <Dropdown
                className="filter-select w-full"
                value={filters.marketplace}
                inputId="filtro-loja"
                onChange={(e) => handleFilterChange("marketplace", e.value)}
                options={[ 'Todos', 'Mercado Livre', 'Amazon', 'Shopee' ]}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>

            <div className="filter-group">
              <label htmlFor="filtro-status">Status:</label>
              <Dropdown
                className="filter-select w-full"
                value={filters.status}
                inputId="filtro-status"
                onChange={(e) => handleFilterChange("status", e.value)}
                options={[ 'Todos', 'Pendente', 'Enviado', 'Entregue', 'Cancelado' ]}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>

            <button className="filter-button" onClick={applyFilters} {...srProps(srOpt, { 'aria-label': 'Aplicar filtros' })}>Filtrar</button>

            {(filters.search ||
              (Array.isArray(filters.period) && (filters.period[0] || filters.period[1])) ||
              filters.marketplace !== "Todos" ||
              filters.status !== "Todos") && (
              <button className="clear-filter-button" onClick={clearFilters} {...srProps(srOpt, { 'aria-label': 'Limpar filtros' })}>
                Limpar Filtro
              </button>
            )}
          </div>

          <div className="pedidos-table-container" {...srProps(srOpt, { role: 'region', 'aria-label': 'Tabela de pedidos' })}>
            <DataTable
              value={filteredOrders}
              paginator
              rows={itemsPerPage}
              className="w-full tabela-pedidos"
              emptyMessage="Nenhum pedido encontrado"
              rowHover
              sortField={sortField}
              sortOrder={sortOrder}
              onSort={handleSort}
            >
              <Column field="id" header="Nº Pedido" sortable />
              <Column field="dataEmissao" header="Data Emissão" sortable />
              <Column field="dataEntrega" header="Data Entrega" sortable />
              <Column field="marketplace" header="Marketplace" sortable />
              <Column field="pagamento" header="Pagamento" sortable />
              <Column field="status" header="Status Atual" sortable />
              <Column
                header=""
                style={{ width: '3rem', textAlign: 'center' }}
                body={(order) => (
                  <Button
                    icon="pi pi-pencil"
                    className="p-button-sm p-button-rounded p-button-text btn-acao-editar"
                    onClick={() => handleRowClick(order)}
                    tooltip="Editar pedido"
                    tooltipOptions={{ position: 'top' }}
                    aria-label={`Editar pedido ${order.id}`}
                  />
                )}
              />
            </DataTable>
          </div>

          <div className="pedidos-footer flex justify-content-between align-items-center mt-3">
            <button className="add-button" onClick={openAdd} {...srProps(srOpt, { 'aria-label': 'Adicionar pedido' })}>Adicionar Pedido</button>
            <span className="text-600">Total: {filteredOrders.length}</span>
          </div>
      
      {modalOpen && selectedOrder && (
        <DialogoReutilizavel
          visible={modalOpen}
          onHide={closeModal}
          header={`Editar Pedido #${selectedOrder.id}`}
          position="right"
          width="480px"
        >
          <div role="form" aria-label="Editar pedido" className="flex flex-column gap-3 p-2">
            <div className="flex flex-column gap-2">
              <label>Nº do pedido</label>
              <InputText value={selectedOrder.id} readOnly />
            </div>
            <div className="flex flex-column gap-2">
              <label>Data de emissão</label>
              <Calendar
                value={parseBRDate(selectedOrder.dataEmissao)}
                onChange={(e) => setSelectedOrder(o => ({ ...o, dataEmissao: toBR(e.value) }))}
                dateFormat="dd/mm/yy"
                placeholder="Selecione a data"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Data de entrega</label>
              <Calendar
                value={selectedOrder.dataEntrega ? parseBRDate(selectedOrder.dataEntrega) : null}
                onChange={(e) => setSelectedOrder(o => ({ ...o, dataEntrega: e.value ? toBR(e.value) : "" }))}
                dateFormat="dd/mm/yy"
                placeholder="Selecione a data"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Marketplace</label>
              <InputText
                value={selectedOrder.marketplace}
                onChange={(e) => setSelectedOrder(o => ({ ...o, marketplace: e.target.value }))}
                placeholder="Ex.: Mercado Livre"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Pagamento</label>
              <Dropdown
                value={selectedOrder.pagamento}
                onChange={(e) => setSelectedOrder(o => ({ ...o, pagamento: e.value }))}
                options={[ 'Dinheiro', 'Cartão de crédito', 'Boleto', 'Pix' ]}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Status atual</label>
              <Dropdown
                value={selectedOrder.status}
                onChange={(e) => setSelectedOrder(o => ({ ...o, status: e.value }))}
                options={[ 'Pendente', 'Enviado', 'Entregue', 'Cancelado', 'Reembolsado' ]}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Observação</label>
              <InputText
                value={observation}
                onChange={(e) => setObservation(e.target.value)}
                placeholder="Digite uma observação..."
              />
            </div>
            <div className="flex justify-content-end gap-2 mt-2">
              <Button label="Cancelar" severity="secondary" onClick={closeModal} />
              <Button label="Salvar" icon="pi pi-check" onClick={saveObservation} />
            </div>
          </div>
        </DialogoReutilizavel>
      )}

      {addOpen && (
  <DialogoReutilizavel
          visible={addOpen}
          onHide={cancelAdd}
          header="Novo Pedido"
          position="right"
          width="480px"
        >
          <div role="form" aria-label="Adicionar pedido" className="flex flex-column gap-3 p-2">
            <div className="flex flex-column gap-2">
              <label>Nº do pedido</label>
              <InputText value={nextOrderId()} readOnly />
            </div>
            <div className="flex flex-column gap-2">
              <label>Data de emissão (obrigatória)</label>
              <Calendar
                value={newOrder.dataEmissao}
                onChange={(e) => setNewOrder(v => ({ ...v, dataEmissao: e.value }))}
                dateFormat="dd/mm/yy"
                placeholder="Selecione a data"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Data de entrega (opcional)</label>
              <Calendar
                value={newOrder.dataEntrega}
                onChange={(e) => setNewOrder(v => ({ ...v, dataEntrega: e.value }))}
                dateFormat="dd/mm/yy"
                placeholder="Selecione a data"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Marketplace</label>
              <InputText value={newOrder.marketplace} onChange={(e) => setNewOrder(v => ({ ...v, marketplace: e.target.value }))} placeholder="Ex.: Mercado Livre" />
            </div>
            <div className="flex flex-column gap-2">
              <label>Pagamento</label>
              <Dropdown value={newOrder.pagamento} onChange={(e) => setNewOrder(v => ({ ...v, pagamento: e.value }))}
                options={[ 'Dinheiro', 'Cartão de crédito', 'Boleto', 'Pix' ]} placeholder="Selecione" appendTo="self" />
            </div>
            <div className="flex flex-column gap-2">
              <label>Status atual</label>
              <Dropdown value={newOrder.status} onChange={(e) => setNewOrder(v => ({ ...v, status: e.value }))}
                options={[ 'Pendente', 'Entregue', 'Cancelado', 'Reembolsado' ]} placeholder="Selecione" appendTo="self" />
            </div>
            <div className="flex justify-content-end gap-2 mt-2">
              <Button label="Cancelar" severity="secondary" onClick={cancelAdd} />
              <Button label="Adicionar Pedido" icon="pi pi-check" onClick={confirmAdd} />
            </div>
          </div>
  </DialogoReutilizavel>
      )}
    </div>
  );
};

export default Pedidos;
