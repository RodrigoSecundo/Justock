import React, { useState, useEffect } from "react";
import { getProdutos } from "../../utils/api";
import "../../styles/pages/dashboard/dashboard.css";
import "../../styles/pages/dashboard/produtos.css";
import { useSrOptimized, srProps } from "../../utils/useA11y";
import DialogoReutilizavel from "../../components/common/DialogoReutilizavel";
import { InputText } from "primereact/inputtext";
import { InputNumber } from "primereact/inputnumber";
import { Dropdown } from "primereact/dropdown";
import { Button } from "primereact/button";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";

const ModalAdicionarProduto = ({ isOpen, onClose, onAddProduct, nextId }) => {
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

  const handleSubmit = (e) => {
    e.preventDefault();
    const finalMarca = formData.marca === "Outras" ? formData.customMarca : formData.marca;
    const newProduct = {
      id: nextId,
      ...formData,
      marca: finalMarca,
      preco: `R$ ${formData.preco.replace(/\D/g, "").replace(/(\d)(\d{2})$/, "$1,$2").replace(/(\d)(?=(\d{3})+(?!\d))/g, "$1.")}`,
      estoque: parseInt(formData.estoque)
    };
    onAddProduct(newProduct);
    onClose();
    setFormData({
      categoria: "Placas-mãe",
      marca: "ASUS",
      nome: "",
      estoque: 0,
      preco: "",
      codigoBarras: "",
      customMarca: ""
    });
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
          <Button type="button" label="Cancelar" severity="secondary" onClick={onClose} />
          <Button type="submit" label="Adicionar" icon="pi pi-check" />
        </div>
      </form>
  </DialogoReutilizavel>
  );
};

const ModalEditarProduto = ({ isOpen, onClose, product, onSave }) => {
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
      preco: product.preco.replace("R$ ", ""),
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
        preco: product.preco.replace("R$ ", ""),
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

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!product) return;
    const finalMarca = formData.marca === "Outras" ? formData.customMarca : formData.marca;
    const updated = {
      ...product,
      categoria: formData.categoria,
      marca: finalMarca,
      nome: formData.nome,
      estoque: parseInt(formData.estoque),
      preco: `R$ ${formData.preco.replace(/\D/g, "").replace(/(\d)(\d{2})$/, "$1,$2").replace(/(\d)(?=(\d{3})+(?!\d))/g, "$1.")}`,
      codigoBarras: formData.codigoBarras
    };
    onSave(updated);
    onClose();
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
          <Button type="button" label="Cancelar" severity="secondary" onClick={onClose} />
          <Button type="submit" label="Salvar" icon="pi pi-check" />
        </div>
      </form>
    </DialogoReutilizavel>
  );
};

const Produtos = () => {
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editProduct, setEditProduct] = useState(null);
  const srOpt = useSrOptimized();
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [sortField, setSortField] = useState(null);
  const [sortOrder, setSortOrder] = useState(null);

  useEffect(() => {
    let data = [...products];
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
    setFilteredProducts(data);
  }, [products, sortField, sortOrder]);

  useEffect(() => {
    getProdutos()
      .then((data) => {
        if (data && Array.isArray(data.products)) {
          setProducts(data.products);
        } else {
          setProducts([]);
        }
      })
      .catch(() => {
        setProducts([]);
      });
  }, []);
  const [filters, setFilters] = useState({
    categoria: "Todos os Categorias",
    preco: "Todos os preços"
  });

  const nextId = products.length ? Math.max(...products.map(p => p.id)) + 1 : 1;

  const handleAddProduct = (newProduct) => {
    setProducts([...products, newProduct]);
  };

  const totalPages = Math.ceil(filteredProducts.length / itemsPerPage);

  const handleOpenEdit = (product) => {
    setEditProduct(product);
    setIsEditOpen(true);
  };

  const handleSaveEdit = (updatedProduct) => {
    setProducts(products.map((p) => (p.id === updatedProduct.id ? updatedProduct : p)));
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

    setFilteredProducts(filtered);
    setCurrentPage(1);
  };

  const clearFilters = () => {
    setFilters({
      categoria: "Todos os Categorias",
      preco: "Todos os preços"
    });
    setFilteredProducts(products);
    setCurrentPage(1);
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
              <Column field="nome" header="Nome do Produto" sortable />
              <Column field="estoque" header="Estoque" sortable />
              <Column field="preco" header="Preço" sortable />
              <Column field="codigoBarras" header="Código de Barras" />
              <Column
                header=""
                style={{ width: '3rem', textAlign: 'center' }}
                body={(product) => (
                  <Button
                    icon="pi pi-pencil"
                    className="p-button-sm p-button-rounded p-button-text btn-acao-editar"
                    onClick={() => handleOpenEdit(product)}
                    tooltip="Editar"
                    tooltipOptions={{ position: 'top' }}
                    aria-label="Editar produto"
                  />
                )}
              />
            </DataTable>
          </div>
          <div className="produtos-footer flex justify-content-between align-items-center mt-3">
            <button className="add-button" onClick={() => setIsModalOpen(true)} {...srProps(srOpt, { 'aria-label': 'Adicionar novo produto' })}>Adicionar Produto</button>
            <span className="text-600">Total: {filteredProducts.length}</span>
          </div>
      <ModalAdicionarProduto
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onAddProduct={handleAddProduct}
        nextId={nextId}
      />
      <ModalEditarProduto
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        product={editProduct}
        onSave={handleSaveEdit}
      />
    </div>
  );
};

export default Produtos;
