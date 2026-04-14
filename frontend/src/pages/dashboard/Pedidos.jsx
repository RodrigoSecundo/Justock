import React, { useState, useEffect } from "react";
import { createPedido, getPedidos, updatePedido } from "../../utils/api";
import { Calendar } from "primereact/calendar";
import "../../styles/pages/dashboard/dashboard.css";
import "../../styles/pages/dashboard/pedidos.css";
import { useSrOptimized, srProps } from "../../utils/useA11y";
import { notifySuccess, notifyError } from "../../utils/notify";
import DialogoReutilizavel from "../../components/common/DialogoReutilizavel";
import { InputText } from "primereact/inputtext";
import { InputTextarea } from "primereact/inputtextarea";
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
  const itemsPerPage = 10;
  const [viewOpen, setViewOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [editingOrder, setEditingOrder] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [newOrder, setNewOrder] = useState({
    dataEmissao: null,
    dataEntrega: null,
    marketplace: "Manual",
    pagamento: DEFAULT_MANUAL_PAYMENT_STATUS,
    status: DEFAULT_MANUAL_ORDER_STATUS,
    observacao: "",
  });
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
      if (typeof av === "number" && typeof bv === "number") {
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

  const today = normalizeToDateOnly(new Date());

  const normalizeDate = (dateValue) => normalizeToDateOnly(dateValue);

  const parseBRDate = (br) => {
    if (!br) return null;
    const [day, month, year] = br.split("/").map((value) => parseInt(value, 10));
    const dt = new Date(year, (month || 1) - 1, day || 1);
    dt.setHours(0, 0, 0, 0);
    return Number.isNaN(dt.getTime()) ? null : dt;
  };

  const toBR = (dateValue) => {
    if (!dateValue) return "";
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) return "";
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  };

  const cloneOrderForModal = (order) => ({
    ...order,
    observacao: order?.observacao ?? "",
  });

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
      notifyError("Data de emissão é obrigatória.");
      return false;
    }

    if (isDateAfterToday(issueDate)) {
      notifyError("Data de emissão não pode ser futura.");
      return false;
    }

    if (isDeliveryBeforeIssue(issueDate, deliveryDate)) {
      notifyError("Data de entrega não pode ser anterior à data de emissão.");
      return false;
    }

    if (isDateAfterToday(deliveryDate)) {
      notifyError("Data de entrega não pode ser futura.");
      return false;
    }

    return true;
  };

  const handleEditingIssueDateChange = (value) => {
    setEditingOrder((currentOrder) => {
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
      dataEntrega: isDeliveryBeforeIssue(value, currentOrder.dataEntrega) || isDateAfterToday(currentOrder.dataEntrega)
        ? null
        : currentOrder.dataEntrega,
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
        const orderDate = parseBRDate(order.dataEmissao);
        if (!orderDate) return false;
        const afterStart = start ? orderDate.getTime() >= start.getTime() : true;
        const beforeEnd = end ? orderDate.getTime() <= end.getTime() : true;
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
  };

  const clearFilters = () => {
    setFilters({ search: "", period: null, marketplace: "Todos", status: "Todos" });
    setFilteredOrders(sortOrders(orders));
  };

  const handleRowClick = (order) => {
    setSelectedOrder(cloneOrderForModal(order));
    setViewOpen(true);
  };

  const openEditModal = (order) => {
    if (order?.isReadOnly) {
      notifyError("Não é possível editar pedido originado de marketplace manualmente.");
      return;
    }

    setEditingOrder(cloneOrderForModal(order));
    setEditOpen(true);
  };

  const closeViewModal = () => {
    setViewOpen(false);
    setSelectedOrder(null);
  };

  const closeEditModal = () => {
    setEditOpen(false);
    setEditingOrder(null);
  };

  const saveOrder = async () => {
    if (!editingOrder) return;

    const issueDate = parseBRDate(editingOrder.dataEmissao);
    const deliveryDate = editingOrder.dataEntrega ? parseBRDate(editingOrder.dataEntrega) : null;

    if (!validateOrderDates(issueDate, deliveryDate)) {
      return;
    }

    try {
      setIsUpdatingOrder(true);
      const updatedOrder = await updatePedido(editingOrder.id, {
        idPedidoMarketplace: editingOrder.idPedidoMarketplace ?? editingOrder.marketplace,
        marketplace: editingOrder.marketplace,
        dataEmissao: issueDate,
        dataEntrega: deliveryDate,
        pagamento: editingOrder.pagamento,
        status: editingOrder.status,
        observacao: editingOrder.observacao,
      });

      await loadOrders();
      notifySuccess(`Pedido #${updatedOrder.numeroPedido ?? updatedOrder.id} atualizado.`);
      closeEditModal();
    } catch (error) {
      notifyError(error?.message || "Não foi possível atualizar o pedido.");
    } finally {
      setIsUpdatingOrder(false);
    }
  };

  const nextOrderId = () => {
    const ids = orders.map((order) => Number(order.id) || 0);
    const max = ids.length ? Math.max(...ids) : 0;
    return max + 1;
  };

  const openAdd = () => {
    setNewOrder({
      dataEmissao: null,
      dataEntrega: null,
      marketplace: "Manual",
      pagamento: DEFAULT_MANUAL_PAYMENT_STATUS,
      status: DEFAULT_MANUAL_ORDER_STATUS,
      observacao: "",
    });
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
        observacao: newOrder.observacao,
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
      let normalized = value;
      if (Array.isArray(value) && value[0] && value[1]) {
        const a = normalizeDate(value[0]);
        const b = normalizeDate(value[1]);
        normalized = a && b && a.getTime() > b.getTime() ? [b, a] : [a, b];
      }
      setFilters((prev) => ({ ...prev, period: normalized }));
      return;
    }

    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleSort = (event) => {
    if (!event.sortField) return;
    if (sortField !== event.sortField) {
      setSortField(event.sortField);
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
    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
      <span>{order.numeroPedido}</span>
      {order.marketplaceSource === "MERCADO_LIVRE" && (
        <span style={{ fontSize: "0.7rem", padding: "2px 5px", backgroundColor: "#ffe600", color: "#2d3277", border: "1px solid #d4c000", borderRadius: "4px", fontWeight: "bold" }}>
          MERCADO LIVRE
        </span>
      )}
      {order.marketplaceSource === "MANUAL" && (
        <span style={{ fontSize: "0.7rem", padding: "2px 5px", backgroundColor: "#dff6e4", color: "#21613b", border: "1px solid #a8d5b5", borderRadius: "4px", fontWeight: "bold" }}>
          MANUAL
        </span>
      )}
    </div>
  );

  return (
    <div {...srProps(srOpt, { role: "main", "aria-label": "Lista de pedidos" })}>
      <div className="pedidos-header prime-filtro">
        <div className="filter-group">
          <label htmlFor="filtro-pedido">Nº Pedido:</label>
          <InputText
            className="filter-search"
            value={filters.search}
            onChange={(event) => handleFilterChange("search", event.target.value)}
            id="filtro-pedido"
            placeholder="Buscar por Nº Pedido"
          />
        </div>

        <div className="filter-group">
          <label htmlFor="filtro-periodo">Período:</label>
          <Calendar
            inputId="filtro-periodo"
            value={filters.period}
            onChange={(event) => handleFilterChange("period", event.value)}
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
            onChange={(event) => handleFilterChange("marketplace", event.value)}
            options={["Todos", "Mercado Livre", "Amazon", "Shopee", "Manual"]}
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
            onChange={(event) => handleFilterChange("status", event.value)}
            options={["Todos", ...MANUAL_ORDER_STATUS_OPTIONS]}
            placeholder="Selecione"
            appendTo="self"
          />
        </div>

        <button className="filter-button" onClick={applyFilters} {...srProps(srOpt, { "aria-label": "Aplicar filtros" })}>Filtrar</button>

        {(filters.search ||
          (Array.isArray(filters.period) && (filters.period[0] || filters.period[1])) ||
          filters.marketplace !== "Todos" ||
          filters.status !== "Todos") && (
          <button className="clear-filter-button" onClick={clearFilters} {...srProps(srOpt, { "aria-label": "Limpar filtros" })}>
            Limpar Filtro
          </button>
        )}
      </div>

      <div className="pedidos-table-container" {...srProps(srOpt, { role: "region", "aria-label": "Tabela de pedidos" })}>
        <DataTable
          value={filteredOrders}
          paginator
          rows={itemsPerPage}
          className="w-full tabela-pedidos"
          emptyMessage="Nenhum pedido encontrado"
          rowHover
          onRowClick={(event) => handleRowClick(event.data)}
          rowClassName={() => "pedido-row-clickable"}
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
            style={{ width: "3rem", textAlign: "center" }}
            body={(order) => (
              <Button
                icon="pi pi-pencil"
                className={`p-button-sm p-button-rounded p-button-text btn-acao-editar ${order.isReadOnly ? "btn-acao-bloqueada" : ""}`.trim()}
                onClick={(event) => {
                  event.originalEvent?.stopPropagation?.();
                  openEditModal(order);
                }}
                tooltip={order.isReadOnly ? null : "Editar pedido"}
                tooltipOptions={{ position: "top" }}
                aria-label={`Editar pedido ${order.numeroPedido}`}
              />
            )}
          />
        </DataTable>
      </div>

      <div className="pedidos-footer flex justify-content-between align-items-center mt-3">
        <button className="add-button" onClick={openAdd} {...srProps(srOpt, { "aria-label": "Adicionar pedido" })}>Adicionar Pedido</button>
        <span className="text-600">Total: {filteredOrders.length}</span>
      </div>

      {viewOpen && selectedOrder && (
        <DialogoReutilizavel
          visible={viewOpen}
          onHide={closeViewModal}
          header={`Pedido #${selectedOrder.numeroPedido}`}
          position="top"
          width="min(960px, 92vw)"
          className="pedido-visualizacao-modal"
          contentClassName="pedido-visualizacao-conteudo"
        >
          <div aria-label="Visualizar pedido" className="pedido-visualizacao-dialog flex flex-column gap-4 p-2">
            <div className="pedido-detalhe-grid">
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Nº interno</span>
                <strong>{selectedOrder.numeroPedido}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Marketplace</span>
                <strong>{selectedOrder.marketplace}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Pagamento</span>
                <strong>{selectedOrder.pagamento}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Status</span>
                <strong>{selectedOrder.status}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Data de emissão</span>
                <strong>{selectedOrder.dataEmissao || "-"}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Data de entrega</span>
                <strong>{selectedOrder.dataEntrega || "-"}</strong>
              </div>
            </div>

            <div className="pedido-info-bloco">
              <span className="pedido-detalhe-label">Observação</span>
              <div className="pedido-observacao-view">
                {selectedOrder.observacao?.trim() || "Sem observações para este pedido."}
              </div>
            </div>

            <div className="flex justify-content-end gap-2">
              <Button label="Fechar" severity="secondary" onClick={closeViewModal} />
            </div>
          </div>
        </DialogoReutilizavel>
      )}

      {editOpen && editingOrder && (
        <DialogoReutilizavel
          visible={editOpen}
          onHide={closeEditModal}
          header={`Editar Pedido #${editingOrder.numeroPedido}`}
          position="right"
          width="560px"
        >
          <div role="form" aria-label="Editar pedido" className="pedido-detalhe-dialog flex flex-column gap-3 p-2">
            <div className="pedido-detalhe-grid">
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Nº interno</span>
                <strong>{editingOrder.numeroPedido}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Origem</span>
                <strong>{editingOrder.marketplace}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Pagamento</span>
                <strong>{editingOrder.pagamento}</strong>
              </div>
              <div className="pedido-detalhe-card">
                <span className="pedido-detalhe-label">Status</span>
                <strong>{editingOrder.status}</strong>
              </div>
            </div>
            <div className="flex flex-column gap-2">
              <label>Data de emissão</label>
              <Calendar
                value={parseBRDate(editingOrder.dataEmissao)}
                onChange={(event) => handleEditingIssueDateChange(event.value)}
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
                value={editingOrder.dataEntrega ? parseBRDate(editingOrder.dataEntrega) : null}
                onChange={(event) => setEditingOrder((order) => ({ ...order, dataEntrega: event.value ? toBR(event.value) : "" }))}
                minDate={editingOrder.dataEmissao ? parseBRDate(editingOrder.dataEmissao) : null}
                maxDate={today}
                dateFormat="dd/mm/yy"
                placeholder="Selecione a data"
                appendTo={null}
                panelClassName="jt-calendar-dropdown"
                monthNavigator
                yearNavigator
                disabled={!editingOrder.dataEmissao}
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Marketplace</label>
              <InputText value={editingOrder.marketplace} readOnly />
            </div>
            <div className="flex flex-column gap-2">
              <label>Pagamento</label>
              <Dropdown
                value={editingOrder.pagamento}
                onChange={(event) => setEditingOrder((order) => ({ ...order, pagamento: event.value }))}
                options={MANUAL_PAYMENT_STATUS_OPTIONS}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Status atual</label>
              <Dropdown
                value={editingOrder.status}
                onChange={(event) => setEditingOrder((order) => ({ ...order, status: event.value }))}
                options={MANUAL_ORDER_STATUS_OPTIONS}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Observação</label>
              <InputTextarea
                value={editingOrder.observacao ?? ""}
                onChange={(event) => setEditingOrder((order) => ({ ...order, observacao: event.target.value }))}
                placeholder="Digite uma observação para o vendedor..."
                rows={4}
                autoResize
              />
            </div>
            <div className="flex justify-content-end gap-2 mt-2">
              <Button label="Cancelar" severity="secondary" onClick={closeEditModal} />
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
                onChange={(event) => handleNewIssueDateChange(event.value)}
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
                onChange={(event) => setNewOrder((value) => ({ ...value, dataEntrega: event.value }))}
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
              <Dropdown
                value={newOrder.pagamento}
                onChange={(event) => setNewOrder((value) => ({ ...value, pagamento: event.value }))}
                options={MANUAL_PAYMENT_STATUS_OPTIONS}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Status atual</label>
              <Dropdown
                value={newOrder.status}
                onChange={(event) => setNewOrder((value) => ({ ...value, status: event.value }))}
                options={MANUAL_ORDER_STATUS_OPTIONS}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="flex flex-column gap-2">
              <label>Observação</label>
              <InputTextarea
                value={newOrder.observacao ?? ""}
                onChange={(event) => setNewOrder((value) => ({ ...value, observacao: event.target.value }))}
                rows={4}
                autoResize
                placeholder="Digite uma observação para o vendedor..."
              />
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
