import React, { useState, useEffect } from "react";
import { createProduto, deleteProduto, getProdutos, updateProduto } from "../../utils/api";
import "../../styles/pages/dashboard/dashboard.css";
import "../../styles/pages/dashboard/produtos.css";
import { useSrOptimized, srProps } from "../../utils/useA11y";
import { notifyError, notifySuccess } from "../../utils/notify";
import DialogoReutilizavel from "../../components/common/DialogoReutilizavel";
import { InputText } from "primereact/inputtext";
import { InputNumber } from "primereact/inputnumber";
import { Dropdown } from "primereact/dropdown";
import { Button } from "primereact/button";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";

const ModalAdicionarProduto = ({ isOpen, onClose, onAddProduct, isSaving }) => {
  const [formData, setFormData] = useState({
    categoria: "Placas-mãe",
    marca: "ASUS",
    nome: "",
    estoque: 0,
    preco: "",
    codigoBarras: "",
    customMarca: ""
  });

  const categorias = [
    "Placas-mãe",
    "Processadores",
    "Placas de vídeo",
    "Memórias RAM",
    "Armazenamento",
    "Fontes",
    "Coolers",
    "Outros"
  ];
  const marcas = ["ASUS", "NVIDIA", "AMD", "MSI", "Intel", "Corsair", "Samsung", "Gigabyte", "Kingston", "WD", "G.Skill", "Seagate", "Outras"];

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === "marca" && value !== "Outras") {
      setFormData({ ...formData, [name]: value, customMarca: "" });
    } else {
      setFormData({ ...formData, [name]: value });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const finalMarca = formData.marca === "Outras" ? formData.customMarca : formData.marca;
    const newProduct = {
      categoria: formData.categoria,
      marca: finalMarca,
      nome: formData.nome,
      estoque: Number(formData.estoque ?? 0),
      preco: formData.preco,
      codigoBarras: formData.codigoBarras,
    };

    const created = await onAddProduct(newProduct);
    if (created) {
      onClose();
      setFormData({
        categoria: "Placas-mãe",
        marca: "ASUS",
        nome: "",
        estoque: 0,
        preco: "",
        codigoBarras: "",
        customMarca: "",
      });
    }
  };

  return (
  <DialogoReutilizavel
      visible={isOpen}
      onHide={onClose}
      header="Adicionar Novo Produto"
      position="right"
      width="480px"
    >
      <form onSubmit={handleSubmit} className="flex flex-column gap-3 p-2">
        <div className="flex flex-column gap-2">
          <label>Categoria</label>
          <Dropdown value={formData.categoria} onChange={(e) => handleChange({ target: { name: 'categoria', value: e.value } })}
            options={categorias} placeholder="Selecione" className="w-full" required appendTo="self" />
        </div>
        <div className="flex flex-column gap-2">
          <label>Marca</label>
          <Dropdown value={formData.marca} onChange={(e) => handleChange({ target: { name: 'marca', value: e.value } })}
            options={marcas} placeholder="Selecione" className="w-full" required appendTo="self" />
          {formData.marca === "Outras" && (
            <InputText name="customMarca" value={formData.customMarca} onChange={handleChange} placeholder="Digite a marca" required />
          )}
        </div>
        <div className="flex flex-column gap-2">
          <label>Nome do Produto</label>
          <InputText name="nome" value={formData.nome} onChange={handleChange} required />
        </div>
        <div className="flex flex-column gap-2">
          <label>Estoque</label>
          <InputNumber inputId="estoque" name="estoque" value={formData.estoque} onValueChange={(e) => handleChange({ target: { name: 'estoque', value: e.value } })} useGrouping={false} min={0} />
        </div>
        <div className="flex flex-column gap-2">
          <label>Preço (R$)</label>
          <InputText name="preco" value={formData.preco} onChange={handleChange} placeholder="Ex: 1200,00" required />
        </div>
        <div className="flex flex-column gap-2">
          <label>Código de Barras</label>
          <InputText name="codigoBarras" value={formData.codigoBarras} onChange={handleChange} required />
        </div>
        <div className="flex justify-content-end gap-2 mt-2">
          <Button type="button" label="Cancelar" severity="secondary" onClick={onClose} disabled={isSaving} />
          <Button type="submit" label="Adicionar" icon="pi pi-check" loading={isSaving} disabled={isSaving} />
        </div>
      </form>
  </DialogoReutilizavel>
  );
};

const ModalEditarProduto = ({ isOpen, onClose, product, onSave, isSaving }) => {
  const [formData, setFormData] = useState(() => {
    if (!product) {
      return {
        categoria: "Placas-mãe",
        marca: "ASUS",
        nome: "",
        estoque: 0,
        preco: "",
        codigoBarras: "",
        customMarca: ""
      };
    }
    return {
      categoria: product.categoria,
      marca: product.marca,
      nome: product.nome,
      estoque: product.estoque,
      preco: String(product.preco || "").replace("R$ ", ""),
      codigoBarras: product.codigoBarras,
      customMarca: ""
    };
  });

  useEffect(() => {
    if (product) {
      setFormData({
        categoria: product.categoria,
        marca: product.marca,
        nome: product.nome,
        estoque: product.estoque,
        preco: String(product.preco || "").replace("R$ ", ""),
        codigoBarras: product.codigoBarras,
        customMarca: ""
      });
    }
  }, [product]);

  const categorias = [
    "Placas-mãe",
    "Processadores",
    "Placas de vídeo",
    "Memórias RAM",
    "Armazenamento",
    "Fontes",
    "Coolers",
    "Outros"
  ];

  const marcas = ["ASUS", "NVIDIA", "AMD", "MSI", "Intel", "Corsair", "Samsung", "Gigabyte", "Kingston", "WD", "G.Skill", "Seagate", "Outras"];

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === "marca" && value !== "Outras") {
      setFormData({ ...formData, [name]: value, customMarca: "" });
    } else {
      setFormData({ ...formData, [name]: value });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!product) return;
    const finalMarca = formData.marca === "Outras" ? formData.customMarca : formData.marca;
    const updated = {
      ...product,
      categoria: formData.categoria,
      marca: finalMarca,
      nome: formData.nome,
      estoque: Number(formData.estoque ?? 0),
      preco: formData.preco,
      codigoBarras: formData.codigoBarras
    };
    const saved = await onSave(updated);
    if (saved) {
      onClose();
    }
  };

  return (
    <DialogoReutilizavel
      visible={isOpen}
      onHide={onClose}
      header="Editar Produto"
      position="right"
      width="480px"
    >
      <form onSubmit={handleSubmit} className="flex flex-column gap-3 p-2">
        <div className="flex flex-column gap-2">
          <label>Categoria</label>
          <Dropdown value={formData.categoria} onChange={(e) => handleChange({ target: { name: 'categoria', value: e.value } })}
            options={categorias} placeholder="Selecione" className="w-full" required appendTo="self" />
        </div>
        <div className="flex flex-column gap-2">
          <label>Marca</label>
          <Dropdown value={formData.marca} onChange={(e) => handleChange({ target: { name: 'marca', value: e.value } })}
            options={marcas} placeholder="Selecione" className="w-full" required appendTo="self" />
          {formData.marca === "Outras" && (
            <InputText name="customMarca" value={formData.customMarca} onChange={handleChange} placeholder="Digite a marca" required />
          )}
        </div>
        <div className="flex flex-column gap-2">
          <label>Nome do Produto</label>
          <InputText name="nome" value={formData.nome} onChange={handleChange} required />
        </div>
        <div className="flex flex-column gap-2">
          <label>Estoque</label>
          <InputNumber inputId="estoque-edit" name="estoque" value={formData.estoque} onValueChange={(e) => handleChange({ target: { name: 'estoque', value: e.value } })} useGrouping={false} min={0} />
        </div>
        <div className="flex flex-column gap-2">
          <label>Preço (R$)</label>
          <InputText name="preco" value={formData.preco} onChange={handleChange} placeholder="Ex: 1200,00" required />
        </div>
        <div className="flex flex-column gap-2">
          <label>Código de Barras</label>
          <InputText name="codigoBarras" value={formData.codigoBarras} onChange={handleChange} required />
        </div>
        <div className="flex justify-content-end gap-2 mt-2">
          <Button type="button" label="Cancelar" severity="secondary" onClick={onClose} disabled={isSaving} />
          <Button type="submit" label="Salvar" icon="pi pi-check" loading={isSaving} disabled={isSaving} />
        </div>
      </form>
    </DialogoReutilizavel>
  );
};

const Produtos = () => {
  const itemsPerPage = 10;
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editProduct, setEditProduct] = useState(null);
  const srOpt = useSrOptimized();
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [sortField, setSortField] = useState(null);
  const [sortOrder, setSortOrder] = useState(null);
  const [isCreatingProduct, setIsCreatingProduct] = useState(false);
  const [isUpdatingProduct, setIsUpdatingProduct] = useState(false);
  const [deletingProductId, setDeletingProductId] = useState(null);
  const [isSyncingML, setIsSyncingML] = useState(false);

  const sortProducts = (list) => {
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
    setFilteredProducts(sortProducts(products));
  }, [products, sortField, sortOrder]);

  const loadProducts = async () => {
    try {
      const data = await getProdutos();
      if (data && Array.isArray(data.products)) {
        setProducts(data.products);
      } else {
        setProducts([]);
      }
    } catch {
      setProducts([]);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const [filters, setFilters] = useState({
    categoria: "Todos os Categorias",
    preco: "Todos os preços"
  });

  const handleAddProduct = async (newProduct) => {
    try {
      setIsCreatingProduct(true);
      const fallbackUser = Number(products?.[0]?.usuario ?? 1) || 1;
      await createProduto({
        ...newProduct,
        estado: "ATIVO",
        quantidadeReservada: 0,
        marcador: "MANUAL",
        usuario: fallbackUser,
      });
      await loadProducts();
      notifySuccess("Produto adicionado com sucesso!");
      return true;
    } catch (error) {
      notifyError(error?.message || "Não foi possível adicionar o produto.");
      return false;
    } finally {
      setIsCreatingProduct(false);
    }
  };

  const handleOpenEdit = (product) => {
    setEditProduct(product);
    setIsEditOpen(true);
  };

  const handleSaveEdit = async (updatedProduct) => {
    try {
      setIsUpdatingProduct(true);
      await updateProduto(updatedProduct.id, updatedProduct);
      await loadProducts();
      notifySuccess("Produto atualizado com sucesso!");
      return true;
    } catch (error) {
      notifyError(error?.message || "Não foi possível atualizar o produto.");
      return false;
    } finally {
      setIsUpdatingProduct(false);
    }
  };

  const handleDeleteProduct = async (product) => {
    const confirmed = window.confirm(`Deseja realmente excluir o produto \"${product.nome}\"?`);
    if (!confirmed) return;

    try {
      setDeletingProductId(product.id);
      await deleteProduto(product.id);
      await loadProducts();
      notifySuccess("Produto excluído com sucesso!");
    } catch (error) {
      notifyError(error?.message || "Não foi possível excluir o produto.");
    } finally {
      setDeletingProductId(null);
    }
  };

  const handleSyncML = async () => {
    try {
      setIsSyncingML(true);
      await import("../../utils/api").then(api => api.syncMercadoLivre());
      notifySuccess("Estoque sincronizado ao vivo! Atualizando lista...");
      await loadProducts();
    } catch (e) {
      notifyError(e?.message || "Erro de conexão com o servidor ao sincronizar.");
    } finally {
      setIsSyncingML(false);
    }
  };

  const applyFilters = () => {
    let filtered = products;

    if (filters.categoria !== "Todos os Categorias") {
      filtered = filtered.filter(product => product.categoria === filters.categoria);
    }

    if (filters.preco !== "Todos os preços") {
      const priceRanges = {
        "R$10 - R$1000": [10, 1000],
        "R$1000 - R$2000": [1000, 2000],
        "R$2000 - R$3000": [2000, 3000],
        "R$3000 - R$4000": [3000, 4000],
        "R$3000 - R$5000": [3000, 5000],
        "R$5000+": [5000, Infinity]
      };
      const [min, max] = priceRanges[filters.preco];
      filtered = filtered.filter(product => {
        const price = parseFloat(product.preco.replace("R$ ", "").replace(".", "").replace(",", "."));
        return price >= min && price <= max;
      });
    }

    setFilteredProducts(sortProducts(filtered));
  };

  const clearFilters = () => {
    setFilters({
      categoria: "Todos os Categorias",
      preco: "Todos os preços"
    });
    setFilteredProducts(sortProducts(products));
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
    <div {...srProps(srOpt, { role: 'main', 'aria-label': 'Lista de produtos' })}>
          <div className="cabecalho-produtos prime-filtro" {...srProps(srOpt, { role: 'region', 'aria-label': 'Filtros de produtos' })}>
            <div className="grupo-filtro">
              <label htmlFor="filtro-categoria">Categoria:</label>
              <Dropdown
                id="filtro-categoria"
                className="w-full"
                value={filters.categoria}
                onChange={(e) => setFilters({ ...filters, categoria: e.value })}
                options={[
                  'Todos os Categorias',
                  'Placas-mãe',
                  'Processadores',
                  'Placas de vídeo',
                  'Memórias RAM',
                  'Armazenamento',
                  'Fontes',
                  'Coolers',
                  'Outros',
                ]}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <div className="grupo-filtro">
              <label htmlFor="filtro-preco">Preço:</label>
              <Dropdown
                id="filtro-preco"
                className="w-full"
                value={filters.preco}
                onChange={(e) => setFilters({ ...filters, preco: e.value })}
                options={[
                  'Todos os preços',
                  'R$10 - R$1000',
                  'R$1000 - R$2000',
                  'R$2000 - R$3000',
                  'R$3000 - R$4000',
                  'R$3000 - R$5000',
                  'R$5000+',
                ]}
                placeholder="Selecione"
                appendTo="self"
              />
            </div>
            <button className="botao-filtro" onClick={applyFilters} {...srProps(srOpt, { 'aria-label': 'Aplicar filtros' })}>Filtrar</button>
            {(filters.categoria !== "Todos os Categorias" || filters.preco !== "Todos os preços") && (
              <button className="botao-limpar-filtro" onClick={clearFilters} {...srProps(srOpt, { 'aria-label': 'Limpar filtros' })}>Limpar Filtro</button>
            )}
          </div>
          <div className="produtos-table-container" {...srProps(srOpt, { role: 'region', 'aria-label': 'Tabela de produtos' })}>
            <DataTable
              value={filteredProducts}
              paginator
              rows={itemsPerPage}
              className="w-full tabela-produtos"
              emptyMessage="Nenhum produto encontrado"
              sortField={sortField}
              sortOrder={sortOrder}
              onSort={handleSort}
            >
              <Column field="id" header="ID" sortable />
              <Column field="categoria" header="Categoria" sortable />
              <Column field="marca" header="Marca" sortable />
              <Column field="nome" header="Nome do Produto" sortable body={(rowData) => (
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                   {rowData.nome}
                   {rowData.marcador === 'ML' && <span style={{ fontSize: '0.7rem', padding: '2px 5px', backgroundColor: '#ffe600', color: '#2d3277', border: '1px solid #d4c000', borderRadius: '4px', fontWeight: 'bold'}}>MERCADO LIVRE</span>}
                </div>
              )} />
              <Column field="estoque" header="Estoque" sortable />
              <Column field="preco" header="Preço" sortable />
              <Column field="codigoBarras" header="Código de Barras" />
              <Column
                header=""
                style={{ width: '6rem', textAlign: 'center' }}
                body={(product) => (
                  <div className="flex gap-1 justify-content-center">
                    <Button
                      icon="pi pi-pencil"
                      className="p-button-sm p-button-rounded p-button-text btn-acao-editar"
                      onClick={() => handleOpenEdit(product)}
                      tooltip="Editar"
                      tooltipOptions={{ position: 'top' }}
                      aria-label="Editar produto"
                    />
                    <Button
                      icon="pi pi-trash"
                      className="p-button-sm p-button-rounded p-button-text btn-acao-editar"
                      onClick={() => handleDeleteProduct(product)}
                      tooltip="Excluir"
                      tooltipOptions={{ position: 'top' }}
                      aria-label="Excluir produto"
                      loading={deletingProductId === product.id}
                      disabled={deletingProductId === product.id}
                    />
                  </div>
                )}
              />
            </DataTable>
          </div>
          <div className="produtos-footer flex justify-content-between align-items-center mt-3">
            <div className="flex gap-2">
              <button className="add-button" onClick={() => setIsModalOpen(true)} {...srProps(srOpt, { 'aria-label': 'Adicionar novo produto' })}>Adicionar Produto</button>
              <Button label="Sincronizar M.Livre" icon="pi pi-refresh" loading={isSyncingML} onClick={handleSyncML} className="p-button-outlined" style={{ borderColor: '#ffe600', color: '#2d3277', backgroundColor: '#fffdee'}} />
            </div>
            <span className="text-600">Total: {filteredProducts.length}</span>
          </div>
      <ModalAdicionarProduto
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onAddProduct={handleAddProduct}
        isSaving={isCreatingProduct}
      />
      <ModalEditarProduto
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        product={editProduct}
        onSave={handleSaveEdit}
        isSaving={isUpdatingProduct}
      />
    </div>
  );
};

export default Produtos;
