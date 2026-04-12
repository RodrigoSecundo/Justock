import React, { useState, useEffect } from "react";
import { createPedido, getPedidos, updatePedido } from "../../utils/api";
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

const MANUAL_ORDER_STATUS_OPTIONS = ["EM ANDAMENTO", "CANCELADO", "CONCLUÍDO"];
const DEFAULT_MANUAL_ORDER_STATUS = MANUAL_ORDER_STATUS_OPTIONS[0];
const MANUAL_PAYMENT_STATUS_OPTIONS = ["PROCESSADO", "EM PROCESSAMENTO", "CANCELADO", "NEGADO"];
const DEFAULT_MANUAL_PAYMENT_STATUS = "EM PROCESSAMENTO";

const normalizeToDateOnly = (dateValue) => {
  if (!dateValue) return null;
  const normalizedDate = new Date(dateValue);
  normalizedDate.setHours(0, 0, 0, 0);
  return Number.isNaN(normalizedDate.getTime()) ? null : normalizedDate;
};

const Pedidos = () => {
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [newOrder, setNewOrder] = useState({ dataEmissao: null, dataEntrega: null, marketplace: "Manual", pagamento: DEFAULT_MANUAL_PAYMENT_STATUS, status: DEFAULT_MANUAL_ORDER_STATUS });
  const [observation, setObservation] = useState("");
  const [orders, setOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [sortField, setSortField] = useState(null);
  const [sortOrder, setSortOrder] = useState(null);
  const [isSavingOrder, setIsSavingOrder] = useState(false);
  const [isUpdatingOrder, setIsUpdatingOrder] = useState(false);
  const srOpt = useSrOptimized();
  const [filters, setFilters] = useState({
    search: "",
    period: null,
    marketplace: "Todos",
    status: "Todos",
  });

  const sortOrders = (list) => {
    const data = [...list];

    if (!sortField || !sortOrder) {
      data.sort((a, b) => (Number(a?.id) || 0) - (Number(b?.id) || 0));
      return data;
    }

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

    return data;
  };

  useEffect(() => {
    setFilteredOrders(sortOrders(orders));
  }, [orders, sortField, sortOrder]);

  const loadOrders = async () => {
    try {
      const data = await getPedidos();
      if (data && Array.isArray(data.orders)) {
        setOrders(data.orders);
        return;
      }
      setOrders([]);
    } catch {
      setOrders([]);
    }
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const totalPages = Math.ceil(filteredOrders.length / itemsPerPage);
  const today = normalizeToDateOnly(new Date());

  const normalizeDate = (d) => {
    return normalizeToDateOnly(d);
  };
  const parseBRDate = (br) => {
    if (!br) return null;
    const [day, month, year] = br.split('/').map((v) => parseInt(v, 10));
    const dt = new Date(year, (month || 1) - 1, day || 1);
    dt.setHours(0, 0, 0, 0);
    return isNaN(dt.getTime()) ? null : dt;
  };

  const isDeliveryBeforeIssue = (issueDate, deliveryDate) => {
    const normalizedIssue = normalizeDate(issueDate);
    const normalizedDelivery = normalizeDate(deliveryDate);
    return Boolean(normalizedIssue && normalizedDelivery && normalizedDelivery.getTime() < normalizedIssue.getTime());
  };

  const isDateAfterToday = (dateValue) => {
    const normalizedDate = normalizeDate(dateValue);
    return Boolean(normalizedDate && today && normalizedDate.getTime() > today.getTime());
  };

  const validateOrderDates = (issueDate, deliveryDate) => {
    if (!issueDate) {
      notifyError('Data de emissão é obrigatória.');
      return false;
    }

    if (isDateAfterToday(issueDate)) {
      notifyError('Data de emissão não pode ser futura.');
      return false;
    }

    if (isDeliveryBeforeIssue(issueDate, deliveryDate)) {
      notifyError('Data de entrega não pode ser anterior à data de emissão.');
      return false;
    }

    if (isDateAfterToday(deliveryDate)) {
      notifyError('Data de entrega não pode ser futura.');
      return false;
    }

    return true;
  };

  const handleSelectedIssueDateChange = (value) => {
    setSelectedOrder((currentOrder) => {
      if (!currentOrder) return currentOrder;

      const nextIssueDate = value ? toBR(value) : "";
      const currentDeliveryDate = currentOrder.dataEntrega ? parseBRDate(currentOrder.dataEntrega) : null;
      const shouldResetDelivery = isDeliveryBeforeIssue(value, currentDeliveryDate) || isDateAfterToday(currentDeliveryDate);

      return {
        ...currentOrder,
        dataEmissao: nextIssueDate,
        dataEntrega: shouldResetDelivery ? "" : currentOrder.dataEntrega,
      };
    });
  };

  const handleNewIssueDateChange = (value) => {
    setNewOrder((currentOrder) => ({
      ...currentOrder,
      dataEmissao: value,
      dataEntrega: isDeliveryBeforeIssue(value, currentOrder.dataEntrega) || isDateAfterToday(currentOrder.dataEntrega) ? null : currentOrder.dataEntrega,
    }));
  };

  const applyFilters = () => {
    let filtered = orders;

    if (filters.search) {
      filtered = filtered.filter((order) => String(order.numeroPedido ?? order.id).includes(filters.search));
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

    setFilteredOrders(sortOrders(filtered));
    setCurrentPage(1);
  };

  const clearFilters = () => {
    setFilters({ search: "", period: null, marketplace: "Todos", status: "Todos" });
    setFilteredOrders(sortOrders(orders));
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

  const saveOrder = async () => {
    if (!selectedOrder) return;

    const issueDate = parseBRDate(selectedOrder.dataEmissao);
    const deliveryDate = selectedOrder.dataEntrega ? parseBRDate(selectedOrder.dataEntrega) : null;

    if (!validateOrderDates(issueDate, deliveryDate)) {
      return;
    }

    try {
      setIsUpdatingOrder(true);
      const updatedOrder = await updatePedido(selectedOrder.id, {
        idPedidoMarketplace: selectedOrder.idPedidoMarketplace ?? selectedOrder.marketplace,
        marketplace: selectedOrder.marketplace,
        dataEmissao: issueDate,
        dataEntrega: deliveryDate,
        pagamento: selectedOrder.pagamento,
        status: selectedOrder.status,
      });

      await loadOrders();
      notifySuccess(`Pedido #${updatedOrder.numeroPedido ?? updatedOrder.id} atualizado.`);
      closeModal();
    } catch (error) {
      notifyError(error?.message || "Não foi possível atualizar o pedido.");
    } finally {
      setIsUpdatingOrder(false);
    }
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
    setNewOrder({ dataEmissao: null, dataEntrega: null, marketplace: "Manual", pagamento: DEFAULT_MANUAL_PAYMENT_STATUS, status: DEFAULT_MANUAL_ORDER_STATUS });
    setAddOpen(true);
  };
  const cancelAdd = () => setAddOpen(false);
  const confirmAdd = async () => {
    if (!validateOrderDates(newOrder.dataEmissao, newOrder.dataEntrega)) {
      return;
    }

    try {
      setIsSavingOrder(true);
      const createdOrder = await createPedido({
        idPedidoMarketplace: 4,
        dataEmissao: newOrder.dataEmissao,
        dataEntrega: newOrder.dataEntrega,
        pagamento: newOrder.pagamento,
        status: newOrder.status,
      });

      await loadOrders();
      setAddOpen(false);
      notifySuccess(`Pedido #${createdOrder.numeroPedido ?? createdOrder.id} adicionado.`);
    } catch (error) {
      notifyError(error?.message || "Não foi possível cadastrar o pedido.");
    } finally {
      setIsSavingOrder(false);
    }
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

  const renderOrderNumber = (order) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
      <span>{order.numeroPedido}</span>
      {order.marketplaceSource === 'MERCADO_LIVRE' && (
        <span style={{ fontSize: '0.7rem', padding: '2px 5px', backgroundColor: '#ffe600', color: '#2d3277', border: '1px solid #d4c000', borderRadius: '4px', fontWeight: 'bold' }}>
          MERCADO LIVRE
        </span>
      )}
      {order.marketplaceSource === 'MANUAL' && (
        <span style={{ fontSize: '0.7rem', padding: '2px 5px', backgroundColor: '#dff6e4', color: '#21613b', border: '1px solid #a8d5b5', borderRadius: '4px', fontWeight: 'bold' }}>
          MANUAL
        </span>
      )}
    </div>
  );

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
                options={[ 'Todos', 'Mercado Livre', 'Amazon', 'Shopee', 'Manual' ]}
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
                options={[ 'Todos', ...MANUAL_ORDER_STATUS_OPTIONS ]}
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
              <Column field="numeroPedido" header="Nº Pedido" sortable body={renderOrderNumber} />
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
                    tooltip={order.isReadOnly ? "Pedido sincronizado do Mercado Livre é somente leitura" : "Editar pedido"}
                    tooltipOptions={{ position: 'top' }}
                    aria-label={`Editar pedido ${order.numeroPedido}`}
                    disabled={order.isReadOnly}
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
          header={`Editar Pedido #${selectedOrder.numeroPedido}`}
          position="right"
          width="480px"
        >
          <div role="form" aria-label="Editar pedido" className="flex flex-column gap-3 p-2">
            <div className="flex flex-column gap-2">
              <label>Nº do pedido</label>
              <InputText value={selectedOrder.numeroPedido} readOnly />
            </div>
            <div className="flex flex-column gap-2">
              <label>Data de emissão</label>
              <Calendar
                value={parseBRDate(selectedOrder.dataEmissao)}
                onChange={(e) => handleSelectedIssueDateChange(e.value)}
                maxDate={today}
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
                minDate={selectedOrder.dataEmissao ? parseBRDate(selectedOrder.dataEmissao) : null}
                maxDate={today}
                dateFormat="dd/mm/yy"
                placeholder="Selecione a data"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
                disabled={!selectedOrder.dataEmissao}
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Marketplace</label>
              <Dropdown
                value={selectedOrder.marketplace}
                onChange={(e) => setSelectedOrder(o => ({ ...o, marketplace: e.value }))}
                options={[ 'Mercado Livre', 'Amazon', 'Shopee', 'Manual' ]}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Pagamento</label>
              <Dropdown
                value={selectedOrder.pagamento}
                onChange={(e) => setSelectedOrder(o => ({ ...o, pagamento: e.value }))}
                options={MANUAL_PAYMENT_STATUS_OPTIONS}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Status atual</label>
              <Dropdown
                value={selectedOrder.status}
                onChange={(e) => setSelectedOrder(o => ({ ...o, status: e.value }))}
                options={MANUAL_ORDER_STATUS_OPTIONS}
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
              <Button label="Salvar" icon="pi pi-check" onClick={saveOrder} loading={isUpdatingOrder} disabled={isUpdatingOrder} />
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
                onChange={(e) => handleNewIssueDateChange(e.value)}
                maxDate={today}
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
                minDate={newOrder.dataEmissao || null}
                maxDate={today}
                dateFormat="dd/mm/yy"
                placeholder="Selecione a data"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
                disabled={!newOrder.dataEmissao}
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Marketplace</label>
              <InputText value={newOrder.marketplace} readOnly />
            </div>
            <div className="flex flex-column gap-2">
              <label>Pagamento</label>
              <Dropdown value={newOrder.pagamento} onChange={(e) => setNewOrder(v => ({ ...v, pagamento: e.value }))}
                options={MANUAL_PAYMENT_STATUS_OPTIONS} placeholder="Selecione" appendTo="self" />
            </div>
            <div className="flex flex-column gap-2">
              <label>Status atual</label>
              <Dropdown value={newOrder.status} onChange={(e) => setNewOrder(v => ({ ...v, status: e.value }))}
                options={MANUAL_ORDER_STATUS_OPTIONS} placeholder="Selecione" appendTo="self" />
            </div>
            <div className="flex justify-content-end gap-2 mt-2">
              <Button label="Cancelar" severity="secondary" onClick={cancelAdd} />
              <Button label="Adicionar Pedido" icon="pi pi-check" onClick={confirmAdd} loading={isSavingOrder} disabled={isSavingOrder} />
            </div>
          </div>
  </DialogoReutilizavel>
      )}
    </div>
  );
};

export default Pedidos;
