# JusTock - Guia Principal do Projeto

Este é o README principal do repositório. Ele consolida e melhora as instruções dos READMEs dedicados de:
- `frontend/README.md`
- `backend/README.MD`

## Visão geral

O projeto está dividido em duas partes:
- **Frontend**: React + Vite (`frontend/`)
- **Backend**: Spring Boot (`backend/Justock-Spring/justock-api/`)

### Arquitetura de dados (importante)
Atualmente o frontend usa uma estratégia híbrida:
- **Rotas que usam banco de dados real (backend Spring + Supabase/PostgreSQL):**
  - `GET /api/products/`
  - `GET /api/products/visualizar/{id}`
  - `POST /api/products/cadastrar`
  - `PUT /api/products/atualizar/{id}`
  - `DELETE /api/products/deletar/{id}`
  - `GET /api/Order/`
  - `POST /api/Order/cadastrar`
  - `PUT /api/Order/atualizar/{id}`
- **Rotas ainda mockadas via `json-server` (`frontend/db.json`):**
  - dashboard, conexões, relatórios, assinatura, usuários e demais objetos ainda não migrados

Observação importante sobre o Dashboard:
- O card **Total de Produtos** já usa dados reais do backend, somando a coluna `quantidade` da tabela `estoque` (via `GET /api/products/`).
- O card **Produtos em Baixa** permanece mockado no `db.json` por enquanto.
Ou seja: **Produtos e Pedidos já usam backend real**, enquanto outras telas ainda podem depender do mock.

### Nota de atualização - Pedidos
- O fluxo de **Pedidos** recebeu validações no frontend e no backend para evitar dados inconsistentes.
- **Data de emissão**: obrigatória, só pode ser hoje ou uma data passada.
- **Data de entrega**: opcional, mas quando informada deve ficar entre a data de emissão e a data atual; não pode ser anterior à emissão nem futura.
- **Status do pedido** aceito para inserções manuais: `EM ANDAMENTO`, `CANCELADO` e `CONCLUÍDO`.
- **Status de pagamento** aceito para inserções manuais: `PROCESSADO`, `EM PROCESSAMENTO`, `CANCELADO` e `NEGADO`.
- Pedidos antigos fora desses padrões foram normalizados no banco por migrations do Flyway.

---

## Pré-requisitos

### Para o Frontend
- Node.js 16+
- npm

### Para o Backend
- JDK 17
- Maven 3.9.11+ (ou usar o Maven Wrapper `mvnw` que já está no projeto)
- IDE de sua preferência (VS Code, IntelliJ, Eclipse etc.)

Sugestão de extensões no VS Code (Java):
- Java Extension Pack
- Spring Boot Extension Pack

---

## Como iniciar o projeto (passo a passo)

> Recomendado: abrir **3 terminais** na raiz `Justock`.

## 1) Frontend (Vite)
No terminal 1:

```powershell
cd frontend
npm install
npm run dev
```

Frontend em dev: `http://localhost:5173`

---

## 2) API mock (json-server)
No terminal 2:

```powershell
cd frontend
npm run api
```

Mock API: `http://localhost:3001`

Esse servidor lê o arquivo `frontend/db.json`.

---

## 3) Backend (Spring Boot)
No terminal 3:

```powershell
cd backend/Justock-Spring/justock-api
./mvnw.cmd spring-boot:run
```

Backend padrão: `http://localhost:8080`

Se quiser validar compilação antes de subir:

```powershell
cd backend/Justock-Spring/justock-api
./mvnw.cmd -DskipTests clean compile
```

---

## Variáveis de ambiente do Frontend

O frontend usa duas bases:
- `VITE_API_BASE_URL` → mock (`json-server`), padrão: `http://localhost:3001`
- `VITE_BACKEND_API_BASE_URL` → backend real, padrão: `http://localhost:8080`

Exemplo em PowerShell:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:3001"
$env:VITE_BACKEND_API_BASE_URL = "http://localhost:8080"
npm run dev
```

---

## Fluxo recomendado no dia a dia

- Suba **backend + frontend + mock** quando estiver trabalhando em várias telas.
- Se for trabalhar só em **Produtos/Pedidos**, priorize backend + frontend.
- Se for trabalhar em telas ainda mockadas, mantenha também o `npm run api`.

---

## Credenciais de desenvolvimento (frontend)

- `testeAdminSEC@exemplo.com` / `S@nh4secr3t4`

---

## Estrutura resumida

```text
Justock/
├─ backend/
│  ├─ README.MD
│  └─ Justock-Spring/justock-api/
├─ frontend/
│  ├─ README.md
│  ├─ db.json
│  └─ src/
└─ README.md   <-- (este arquivo)
```

---

## Documentação dedicada

Para detalhes específicos de cada stack:
- Frontend: `frontend/README.md`
- Backend: `backend/README.MD`
