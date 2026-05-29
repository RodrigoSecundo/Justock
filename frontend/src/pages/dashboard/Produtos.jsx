import React, { useCallback, useEffect, useRef, useState } from "react";
import * as XLSX from "xlsx";
import { createProduto, deleteProduto, getProdutos, importProdutos, manterAnuncioSeparado, updateProduto, vincularAnuncioMarketplace } from "../../utils/api";
import "../../styles/pages/dashboard/dashboard.css";
import "../../styles/pages/dashboard/produtos.css";
import { useSrOptimized, srProps } from "../../utils/useA11y?v=20260514-6";
import { notifyError, notifySuccess } from "../../utils/notify";
import { isPrimaryAdminUser } from "../../utils/auth";
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

const DOWNLOAD_FILES = {
  excel: {
    nome: "estoque.xlsx",
    url: "/downloads/estoque.xlsx",
  },
  csv: {
    nome: "estoque.csv",
    url: "/downloads/estoque.csv",
  },
};

const FILE_ACCEPT = ".csv,.xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/csv";
const IMPORT_ALLOWED_EXTENSIONS = new Set(["csv", "xlsx"]);
const IMPORT_REQUIRED_FIELDS = ["categoria", "marca", "nome", "estoque", "preco", "codigoBarras"];
const IMPORT_HEADER_ALIASES = {
  categoria: "categoria",
  marca: "marca",
  nomedoproduto: "nome",
  estoqueinteiro: "estoque",
  estoque: "estoque",
  preco: "preco",
  codigodebarras: "codigoBarras",
};

function getFileExtension(fileName) {
  return String(fileName || "").split(".").pop()?.toLowerCase() || "";
}

function normalizeImportHeader(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");
}

function readFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ""));
    reader.onerror = () => reject(new Error("Não foi possível ler o arquivo CSV."));
    reader.readAsText(file, "utf-8");
  });
}

function readFileAsArrayBuffer(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error("Não foi possível ler o arquivo Excel."));
    reader.readAsArrayBuffer(file);
  });
}

function splitCsvLine(line) {
  const values = [];
  let current = "";
  let inQuotes = false;

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    const nextChar = line[index + 1];

    if (char === '"') {
      if (inQuotes && nextChar === '"') {
        current += '"';
        index += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (char === ";" && !inQuotes) {
      values.push(current);
      current = "";
      continue;
    }

    current += char;
  }

  values.push(current);
  return values;
}

function trimTrailingEmptyRows(rows) {
  let lastNonEmptyIndex = rows.length - 1;

  while (lastNonEmptyIndex >= 0) {
    const row = Array.isArray(rows[lastNonEmptyIndex]) ? rows[lastNonEmptyIndex] : [];
    const hasValue = row.some((cell) => String(cell ?? "").trim() !== "");
    if (hasValue) {
      break;
    }
    lastNonEmptyIndex -= 1;
  }

  return lastNonEmptyIndex >= 0 ? rows.slice(0, lastNonEmptyIndex + 1) : [];
}

function mapImportHeaders(headerRow) {
  const columnIndexByField = {};

  headerRow.forEach((headerCell, index) => {
    const normalizedHeader = normalizeImportHeader(headerCell);
    const field = IMPORT_HEADER_ALIASES[normalizedHeader];
    if (!field) return;

    if (field in columnIndexByField) {
      throw new Error(`O arquivo possui a coluna "${headerCell}" repetida.`);
    }

    columnIndexByField[field] = index;
  });

  const missingFields = IMPORT_REQUIRED_FIELDS.filter((field) => !(field in columnIndexByField));
  if (missingFields.length > 0) {
    throw new Error("O arquivo não contém todas as colunas obrigatórias do modelo de importação.");
  }

  return columnIndexByField;
}

function parseImportPrice(value, rowNumber) {
  const text = String(value ?? "").trim();
  if (!text) {
    throw new Error(`A linha ${rowNumber} está com o preço vazio.`);
  }

  let normalized = text.replace(/\s/g, "");
  if (normalized.includes(",")) {
    normalized = normalized.replace(/[^\d,.-]/g, "").replace(/\./g, "").replace(",", ".");
  } else {
    normalized = normalized.replace(/[^\d.-]/g, "");
  }

  const parsed = Number(normalized);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`A linha ${rowNumber} possui um preço inválido.`);
  }

  return parsed;
}

function parseImportStock(value, rowNumber) {
  const text = String(value ?? "").trim();
  if (!/^\d+$/.test(text)) {
    throw new Error(`A linha ${rowNumber} possui um estoque inválido. Use apenas números inteiros maiores ou iguais a zero.`);
  }

  return Number(text);
}

function getRequiredCellValue(row, headerMap, field, rowNumber, label) {
  const value = String(row[headerMap[field]] ?? "").trim();
  if (!value) {
    throw new Error(`A linha ${rowNumber} está com o campo ${label} vazio.`);
  }
  return value;
}

function mapRowsToImportedProducts(rows) {
  if (!Array.isArray(rows) || rows.length === 0) {
    throw new Error("O arquivo está vazio.");
  }

  const [headerRow, ...dataRows] = trimTrailingEmptyRows(rows);
  if (!headerRow || headerRow.length === 0) {
    throw new Error("Não foi possível identificar o cabeçalho do arquivo.");
  }

  if (dataRows.length === 0) {
    throw new Error("O arquivo não possui linhas de dados para importar.");
  }

  const headerMap = mapImportHeaders(headerRow);

  return dataRows.map((row, dataIndex) => {
    const rowNumber = dataIndex + 2;
    const normalizedRow = Array.isArray(row) ? row : [];
    const hasAnyValue = normalizedRow.some((cell) => String(cell ?? "").trim() !== "");

    if (!hasAnyValue) {
      throw new Error(`A linha ${rowNumber} está vazia. Remova linhas vazias antes de importar.`);
    }

    const categoria = getRequiredCellValue(normalizedRow, headerMap, "categoria", rowNumber, "Categoria");
    const marca = getRequiredCellValue(normalizedRow, headerMap, "marca", rowNumber, "Marca");
    const nome = getRequiredCellValue(normalizedRow, headerMap, "nome", rowNumber, "Nome do Produto");
    const estoque = parseImportStock(normalizedRow[headerMap.estoque], rowNumber);
    const preco = parseImportPrice(normalizedRow[headerMap.preco], rowNumber);
    const codigoBarras = getRequiredCellValue(normalizedRow, headerMap, "codigoBarras", rowNumber, "Código de Barras");

    return {
      categoria,
      marca,
      nome,
      estoque,
      preco,
      codigoBarras,
    };
  });
}

async function parseCsvImportFile(file) {
  const text = (await readFileAsText(file)).replace(/^\uFEFF/, "").replace(/\r/g, "").trimEnd();
  const lines = text.split("\n");
  const rows = lines.map((line) => splitCsvLine(line).map((value) => String(value ?? "").trim()));
  return mapRowsToImportedProducts(rows);
}

async function parseXlsxImportFile(file) {
  const buffer = await readFileAsArrayBuffer(file);
  const workbook = XLSX.read(buffer, { type: "array", cellText: true, cellDates: false });
  const firstSheetName = workbook.SheetNames?.[0];

  if (!firstSheetName) {
    throw new Error("Não foi possível identificar uma planilha válida no arquivo Excel.");
  }

  const worksheet = workbook.Sheets[firstSheetName];
  const rows = XLSX.utils.sheet_to_json(worksheet, {
    header: 1,
    raw: false,
    defval: "",
    blankrows: true,
  });

  return mapRowsToImportedProducts(rows);
}

async function parseImportedStockFile(file) {
  const extension = getFileExtension(file?.name);

  if (!IMPORT_ALLOWED_EXTENSIONS.has(extension)) {
    throw new Error("Selecione apenas arquivos .xlsx ou .csv.");
  }

  if (extension === "csv") {
    return parseCsvImportFile(file);
  }

  if (extension === "xlsx") {
    return parseXlsxImportFile(file);
  }

  throw new Error("Formato de arquivo não suportado para importação.");
}

const ModalImportarEstoque = ({ isOpen, onClose, onImport, isImporting }) => {
  const inputRef = useRef(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);

  const clearSelection = useCallback(() => {
    setSelectedFile(null);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  }, []);

  useEffect(() => {
    if (!isOpen) {
      clearSelection();
      setIsDragActive(false);
    }
  }, [clearSelection, isOpen]);

  const triggerFileSelect = () => {
    inputRef.current?.click();
  };

  const selectFile = (file) => {
    if (!file) return;

    const extension = getFileExtension(file.name);

    if (!IMPORT_ALLOWED_EXTENSIONS.has(extension)) {
      clearSelection();
      notifyError("Selecione apenas arquivos .xlsx ou .csv.");
      return;
    }

    setSelectedFile(file);
    setIsDragActive(false);
  };

  const handleInputChange = (event) => {
    const [file] = event.target.files || [];
    selectFile(file);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragActive(false);
    const [file] = event.dataTransfer.files || [];
    selectFile(file);
  };

  const handleImportClick = async () => {
    if (!selectedFile || isImporting) return;
    const imported = await onImport(selectedFile);
    if (imported) {
      clearSelection();
    }
  };

  const handleDownload = ({ nome, url }) => {
    const link = document.createElement("a");
    link.href = url;
    link.download = nome;
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  return (
    <DialogoReutilizavel
      visible={isOpen}
      onHide={onClose}
      header="Importar Estoque"
      position="top"
      width="min(720px, 96vw)"
      className="modal-importar-estoque"
      contentClassName="modal-importar-estoque-content"
    >
      <div className="importar-estoque-modal">
        <div className="importar-estoque-downloads">
          <Button
            type="button"
            label="Baixar Modelo Excel"
            icon="pi pi-file-excel"
            onClick={() => handleDownload(DOWNLOAD_FILES.excel)}
          />
          <Button
            type="button"
            label="Baixar Modelo CSV"
            icon="pi pi-download"
            severity="secondary"
            outlined
            onClick={() => handleDownload(DOWNLOAD_FILES.csv)}
          />
        </div>

        <input
          ref={inputRef}
          type="file"
          accept={FILE_ACCEPT}
          className="importar-estoque-input"
          onChange={handleInputChange}
        />

        <div
          className={`importar-estoque-dropzone ${isDragActive ? "is-drag-active" : ""}`.trim()}
          onClick={triggerFileSelect}
          onDragOver={(event) => {
            event.preventDefault();
            setIsDragActive(true);
          }}
          onDragLeave={() => setIsDragActive(false)}
          onDrop={handleDrop}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              triggerFileSelect();
            }
          }}
          aria-label="Selecionar arquivo para importação de estoque"
        >
          <i className="pi pi-upload importar-estoque-icone" aria-hidden="true" />
          <strong>Arraste o arquivo para cá</strong>
          <span>ou clique para selecionar um CSV ou XLSX</span>
          <Button
            type="button"
            label="Selecionar Arquivo"
            icon="pi pi-folder-open"
            className="importar-estoque-escolher"
            onClick={(event) => {
              event.stopPropagation();
              triggerFileSelect();
            }}
          />
          {selectedFile ? (
            <div className="importar-estoque-arquivo" aria-live="polite">
              <span className="importar-estoque-arquivo-nome">{selectedFile.name}</span>
              <span className="importar-estoque-arquivo-tamanho">
                {(selectedFile.size / 1024).toFixed(1)} KB selecionado(s)
              </span>
            </div>
          ) : null}
        </div>

        <p className="importar-estoque-observacao">
          Obs: Recomendamos o uso do modelo em xlsx (Excel) para maior compatibilidade e facilidade na alocação nos campos.
        </p>

        <div className="importar-estoque-acoes">
          <Button type="button" label="Fechar" severity="secondary" onClick={onClose} disabled={isImporting} />
          <Button
            type="button"
            label="Importar Estoque"
            icon="pi pi-upload"
            onClick={handleImportClick}
            disabled={!selectedFile || isImporting}
            loading={isImporting}
          />
        </div>
      </div>
    </DialogoReutilizavel>
  );
};

const ModalVincularAnuncio = ({ isOpen, onClose, listing, availableProducts, onConfirm, isSaving }) => {
  const [selectedProductId, setSelectedProductId] = useState(null);

  useEffect(() => {
    if (!isOpen) {
      setSelectedProductId(null);
      return;
    }
    setSelectedProductId(listing?.produtoVinculadoId ?? null);
  }, [isOpen, listing]);

  const options = availableProducts.map((product) => ({
    label: `${product.nome} (${product.codigoBarras || "Sem código"})`,
    value: product.id,
  }));

  const handleConfirm = async () => {
    if (!selectedProductId || !listing) return;
    const linked = await onConfirm(listing, selectedProductId);
    if (linked) {
      onClose();
    }
  };

  return (
    <DialogoReutilizavel
      visible={isOpen}
      onHide={onClose}
      header="Escolher Produto"
      position="top"
      width="min(640px, 94vw)"
      className="modal-vincular-anuncio"
      contentClassName="modal-vincular-anuncio-content"
      style={{ minHeight: "30rem" }}
    >
      <div className="flex flex-column gap-3 p-2">
        <div className="produto-vinculo-resumo">
          <span className="produto-vinculo-label">Anúncio importado</span>
          <strong>{listing?.nome || "-"}</strong>
          <span className="produto-vinculo-meta">Código identificado: {listing?.codigoBarras || "N/A"}</span>
        </div>
        <div className="flex flex-column gap-2">
          <label htmlFor="produto-vinculo-select">Produto interno</label>
          <Dropdown
            inputId="produto-vinculo-select"
            value={selectedProductId}
            onChange={(event) => setSelectedProductId(event.value)}
            options={options}
            placeholder="Selecione um produto já cadastrado"
            className="w-full"
            filter
            showClear
            scrollHeight="320px"
          />
        </div>
        <div className="flex justify-content-end gap-2 mt-2">
          <Button type="button" label="Cancelar" severity="secondary" onClick={onClose} disabled={isSaving} />
          <Button type="button" label="Vincular" icon="pi pi-check" onClick={handleConfirm} disabled={!selectedProductId || isSaving} loading={isSaving} />
        </div>
      </div>
    </DialogoReutilizavel>
  );
};

const ModalDesvincularAnuncio = ({ isOpen, onClose, product, onConfirm, isSaving }) => {
  const linkedListings = Array.isArray(product?.linkedListings) ? product.linkedListings : [];
  const linkedCount = linkedListings.length;

  const handleConfirm = async () => {
    if (!product || linkedCount === 0) return;
    const unlinked = await onConfirm(product);
    if (unlinked) {
      onClose();
    }
  };

  return (
    <DialogoReutilizavel
      visible={isOpen}
      onHide={isSaving ? () => {} : onClose}
      header={linkedCount === 1 ? "Desvincular anúncio" : "Desvincular anúncios"}
      position="top"
      width="min(560px, 94vw)"
      closable={!isSaving}
      dismissableMask={!isSaving}
      className="modal-desvincular-anuncio"
    >
      <div className="desvincular-anuncio-modal">
        <div className="produto-vinculo-resumo">
          <span className="produto-vinculo-label">Produto interno</span>
          <strong>{product?.nome || "-"}</strong>
          <span className="produto-vinculo-meta">
            {linkedCount === 1 ? "O anúncio abaixo voltará a ficar separado." : `${linkedCount} anúncio(s) voltarão a ficar separados.`}
          </span>
        </div>

        <div className="desvincular-anuncio-lista">
          {linkedListings.map((listing) => (
            <div key={listing.id} className="desvincular-anuncio-item">
              <span className="desvincular-anuncio-nome">{listing.nome}</span>
              <span className="desvincular-anuncio-codigo">Código: {listing.codigoBarras || "N/A"}</span>
            </div>
          ))}
        </div>

        <div className="flex justify-content-end gap-2 mt-2">
          <Button type="button" label="Cancelar" severity="secondary" onClick={onClose} disabled={isSaving} />
          <Button type="button" label={linkedCount === 1 ? "Desvincular" : "Desvincular todos"} severity="danger" icon="pi pi-unlink" onClick={handleConfirm} loading={isSaving} disabled={isSaving} />
        </div>
      </div>
    </DialogoReutilizavel>
  );
};

function buildVisibleProductRows(sourceProducts) {
  const internalProducts = (Array.isArray(sourceProducts) ? sourceProducts : [])
    .filter((product) => product.tipoRegistro === "PRODUTO")
    .map((product) => ({
      ...product,
      linkedListings: [],
    }));

  const productById = new Map(internalProducts.map((product) => [product.id, product]));
  const visibleListings = [];

  for (const product of Array.isArray(sourceProducts) ? sourceProducts : []) {
    if (product.tipoRegistro !== "ANUNCIO") {
      continue;
    }

    if (product.produtoVinculadoId != null) {
      const linkedProduct = productById.get(product.produtoVinculadoId);
      if (linkedProduct) {
        linkedProduct.linkedListings = [...linkedProduct.linkedListings, product];
        continue;
      }
    }

    visibleListings.push({
      ...product,
      statusVinculo: "NAO_VINCULADO",
    });
  }

  return [...internalProducts, ...visibleListings];
}

const Produtos = () => {
  const itemsPerPage = 10;
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editProduct, setEditProduct] = useState(null);
  const srOpt = useSrOptimized();
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [sortField, setSortField] = useState(null);
  const [sortOrder, setSortOrder] = useState(null);
  const [isCreatingProduct, setIsCreatingProduct] = useState(false);
  const [isImportingProducts, setIsImportingProducts] = useState(false);
  const [isUpdatingProduct, setIsUpdatingProduct] = useState(false);
  const [deletingProductId, setDeletingProductId] = useState(null);
  const [linkingListingId, setLinkingListingId] = useState(null);
  const [unlinkingProductId, setUnlinkingProductId] = useState(null);
  const [selectedListing, setSelectedListing] = useState(null);
  const [selectedUnlinkProduct, setSelectedUnlinkProduct] = useState(null);
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [isUnlinkModalOpen, setIsUnlinkModalOpen] = useState(false);
  const canManageProducts = isPrimaryAdminUser();

  const sortProducts = useCallback((list) => {
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
  }, [sortField, sortOrder]);

  useEffect(() => {
    setFilteredProducts(sortProducts(buildVisibleProductRows(products)));
  }, [products, sortProducts]);

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

  const handleImportProducts = async (file) => {
    try {
      setIsImportingProducts(true);
      const importedProducts = await parseImportedStockFile(file);

      if (!Array.isArray(importedProducts) || importedProducts.length === 0) {
        throw new Error("Nenhum produto válido foi encontrado no arquivo enviado.");
      }

      await importProdutos(importedProducts.map((product) => ({
        ...product,
        estado: "ATIVO",
        quantidadeReservada: 0,
        marcador: "MANUAL",
      })));

      await loadProducts();
      notifySuccess(`${importedProducts.length} produto(s) importado(s) com sucesso!`);
      setIsImportModalOpen(false);
      return true;
    } catch (error) {
      notifyError(error?.message || "Não foi possível importar o arquivo de estoque.");
      return false;
    } finally {
      setIsImportingProducts(false);
    }
  };

  const handleOpenEdit = (product) => {
    if (product?.isReadOnly) {
      notifyError("Produtos sincronizados de marketplace não podem ser editados manualmente.");
      return;
    }

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
    if (product?.isReadOnly) {
      notifyError("Produtos sincronizados de marketplace não podem ser excluídos manualmente.");
      return;
    }

    const confirmed = window.confirm(`Deseja realmente excluir o produto "${product.nome}"?`);
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

  const openLinkModal = (listing) => {
    setSelectedListing(listing);
    setIsLinkModalOpen(true);
  };

  const closeLinkModal = () => {
    setIsLinkModalOpen(false);
    setSelectedListing(null);
  };

  const openUnlinkModal = (product) => {
    setSelectedUnlinkProduct(product);
    setIsUnlinkModalOpen(true);
  };

  const closeUnlinkModal = () => {
    if (unlinkingProductId != null) {
      return;
    }
    setIsUnlinkModalOpen(false);
    setSelectedUnlinkProduct(null);
  };

  const handleLinkListing = async (listing, productId) => {
    try {
      setLinkingListingId(listing.id);
      await vincularAnuncioMarketplace(listing.id, productId);
      await loadProducts();
      notifySuccess("Anúncio vinculado com sucesso.");
      return true;
    } catch (error) {
      notifyError(error?.message || "Não foi possível vincular o anúncio ao produto.");
      return false;
    } finally {
      setLinkingListingId(null);
    }
  };

  const handleUnlinkProduct = async (product) => {
    const linkedListings = Array.isArray(product?.linkedListings) ? product.linkedListings : [];
    if (linkedListings.length === 0) {
      return false;
    }

    try {
      setUnlinkingProductId(product.id);
      await Promise.all(linkedListings.map((listing) => manterAnuncioSeparado(listing.id)));
      await loadProducts();
      notifySuccess(linkedListings.length === 1 ? "Anúncio desvinculado com sucesso." : "Anúncios desvinculados com sucesso.");
      return true;
    } catch (error) {
      notifyError(error?.message || "Não foi possível desfazer o vínculo do anúncio.");
      return false;
    } finally {
      setUnlinkingProductId(null);
    }
  };

  const availableInternalProducts = products.filter((product) => product.tipoRegistro === "PRODUTO");

  const renderProductName = (rowData) => (
    <div className="produto-nome-cell">
      <span>{rowData.nome}</span>
      {rowData.tipoRegistro === "ANUNCIO" && <span className="produto-badge produto-badge-ml">MERCADO LIVRE</span>}
      {rowData.tipoRegistro === "PRODUTO" && rowData.quantidadeAnunciosVinculados > 0 && (
        <span className="produto-badge produto-badge-linked">{rowData.quantidadeAnunciosVinculados} anúncio(s)</span>
      )}
    </div>
  );

  const renderLinkStatus = (rowData) => {
    if (rowData.tipoRegistro === "PRODUTO") {
      const linkedListings = Array.isArray(rowData.linkedListings) ? rowData.linkedListings : [];
      const linkedCount = linkedListings.length;
      const hasLinks = linkedCount > 0;
      return (
        <div className={`produto-vinculo-cell ${hasLinks ? "produto-vinculo-actions produto-vinculo-linked-layout" : "produto-vinculo-centered-layout"}`.trim()}>
          <span className={`produto-vinculo-status ${hasLinks ? "is-linked" : "is-unlinked"}`.trim()}>
            {hasLinks ? "Vinculado" : "Não vinculado"}
          </span>
          {hasLinks ? (
            <>
              <small>{linkedCount === 1 ? linkedListings[0]?.nome || "1 anúncio compartilhando o estoque" : `${linkedCount} anúncio(s) compartilhando o estoque`}</small>
              {canManageProducts ? (
                <div className="produto-vinculo-buttons">
                  <Button
                    type="button"
                    label={linkedCount === 1 ? "Desvincular" : "Desvincular todos"}
                    severity="secondary"
                    outlined
                    size="small"
                    onClick={() => openUnlinkModal(rowData)}
                    loading={unlinkingProductId === rowData.id}
                    disabled={unlinkingProductId === rowData.id}
                  />
                </div>
              ) : null}
            </>
          ) : null}
        </div>
      );
    }

    return (
      <div className="produto-vinculo-cell produto-vinculo-actions produto-vinculo-centered-layout produto-vinculo-listing-layout">
        <span className="produto-vinculo-status is-unlinked">Não vinculado</span>
        <small>Escolha um produto interno para compartilhar o mesmo estoque.</small>
        <div className="produto-vinculo-buttons">
          <Button
            type="button"
            label="Vincular produto"
            size="small"
            onClick={() => openLinkModal(rowData)}
            disabled={linkingListingId === rowData.id}
          />
        </div>
      </div>
    );
  };

  const applyFilters = () => {
    let filtered = buildVisibleProductRows(products);

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
    setFilteredProducts(sortProducts(buildVisibleProductRows(products)));
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
              dataKey="rowKey"
              paginator
              rows={itemsPerPage}
              className="w-full tabela-produtos"
              emptyMessage="Nenhum produto encontrado"
              sortField={sortField}
              sortOrder={sortOrder}
              onSort={handleSort}
            >
              <Column field="displayId" header="ID" sortable />
              <Column field="categoria" header="Categoria" sortable />
              <Column field="marca" header="Marca" sortable />
              <Column field="nome" header="Nome do Produto" sortable body={renderProductName} />
              <Column field="statusVinculo" header="Vinculação" body={renderLinkStatus} style={{ width: '20rem' }} />
              <Column field="estoque" header="Estoque" sortable />
              <Column field="preco" header="Preço" sortable />
              <Column field="codigoBarras" header="Código de Barras" />
              <Column
                header=""
                style={{ width: '6rem', textAlign: 'center' }}
                body={(product) => (
                  canManageProducts && product.tipoRegistro === "PRODUTO" ? <div className="flex gap-1 justify-content-center">
                    <Button
                      icon="pi pi-pencil"
                      className={`p-button-sm p-button-rounded p-button-text btn-acao-editar ${product.isReadOnly ? 'btn-acao-bloqueada' : ''}`.trim()}
                      onClick={() => handleOpenEdit(product)}
                      tooltip={product.isReadOnly ? null : "Editar"}
                      tooltipOptions={{ position: 'top' }}
                      aria-label="Editar produto"
                    />
                    <Button
                      icon="pi pi-trash"
                      className={`p-button-sm p-button-rounded p-button-text btn-acao-editar ${product.isReadOnly ? 'btn-acao-bloqueada' : ''}`.trim()}
                      onClick={() => handleDeleteProduct(product)}
                      tooltip={product.isReadOnly ? null : "Excluir"}
                      tooltipOptions={{ position: 'top' }}
                      aria-label="Excluir produto"
                      loading={deletingProductId === product.id}
                      disabled={deletingProductId === product.id}
                    />
                  </div> : null
                )}
              />
            </DataTable>
          </div>
          <div className="produtos-footer flex justify-content-between align-items-center mt-3">
            <div className="flex gap-2">
              {canManageProducts ? (
                <>
                  <button className="import-button" onClick={() => setIsImportModalOpen(true)} {...srProps(srOpt, { 'aria-label': 'Importar estoque' })}>Importar Estoque</button>
                  <button className="add-button" onClick={() => setIsModalOpen(true)} {...srProps(srOpt, { 'aria-label': 'Adicionar novo produto' })}>Adicionar Produto</button>
                </>
              ) : (
                <span className="text-600">Produtos manuais ficam disponíveis apenas para a conta principal.</span>
              )}
            </div>
            <span className="text-600">Total: {filteredProducts.length}</span>
          </div>
      <ModalAdicionarProduto
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onAddProduct={handleAddProduct}
        isSaving={isCreatingProduct}
      />
      <ModalImportarEstoque
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onImport={handleImportProducts}
        isImporting={isImportingProducts}
      />
      <ModalEditarProduto
        key={editProduct?.id ?? "novo-produto"}
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        product={editProduct}
        onSave={handleSaveEdit}
        isSaving={isUpdatingProduct}
      />
      <ModalVincularAnuncio
        isOpen={isLinkModalOpen}
        onClose={closeLinkModal}
        listing={selectedListing}
        availableProducts={availableInternalProducts}
        onConfirm={handleLinkListing}
        isSaving={linkingListingId != null}
      />
      <ModalDesvincularAnuncio
        isOpen={isUnlinkModalOpen}
        onClose={closeUnlinkModal}
        product={selectedUnlinkProduct}
        onConfirm={handleUnlinkProduct}
        isSaving={unlinkingProductId != null}
      />
    </div>
  );
};

export default Produtos;
