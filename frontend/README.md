# JusTock - Frontend

Frontend da aplicação JusTock, construído com React + Vite. Este repositório contém o cliente que consome uma API REST, no desenvolvimento usamos um `db.json` servido via `json-server` como backend "fake" para facilitar integração local e desenvolvimento independente.

## Requisitos
- Node.js 16+
- npm (ou yarn)

## Instalação
```powershell
npm install
```

## Execução (desenvolvimento)
Abra dois terminais (ou abas):

Terminal 1 — API (json-server):
```powershell
npm run api
```
Isso inicia um servidor REST simples em `http://localhost:3001` lendo os dados de `db.json` na raiz do projeto. As rotas disponíveis seguem os recursos definidos no JSON (ex.: `GET /produtos`, `GET /pedidos`, `GET /relatorios?ano=2025`, `GET /usuarios`, etc.).

Terminal 2 — Frontend (Vite):
```powershell
npm run dev
```
Acesse a aplicação em `http://localhost:5173` (porta padrão do Vite no dev).

## Credenciais de login (dev)
Foi adicionado um usuário padrão para facilitar testes locais:
- Email: `admin@admin.com`
- Senha: `admin123`

Também existe o usuário original de desenvolvimento:
- Email: `admin@justock.com`
- Senha: `123456`

## Variáveis de ambiente
Você pode apontar o frontend para outro backend definindo `VITE_API_BASE_URL` (ex.: `.env` ou na sua sessão):
```powershell
# Windows PowerShell
$env:VITE_API_BASE_URL = "https://api.meuservidor.com"
npm run dev
```

## Estrutura do projeto (atualizada)
```
/ (repo root)
├─ db.json                       # Banco de dados fake (json-server)
├─ package.json                  # scripts + dependências (ver scripts: 'dev' e 'api')
├─ public/                       # arquivos públicos
├─ src/
│  ├─ assets/                    # imagens e arquivos estáticos
│  ├─ components/                # componentes reutilizáveis
│  │  ├─ common/                 # dialogos, notifications, etc.
│  │  └─ dashboard/              # componentes específicos do dashboard
│  ├─ pages/                     # páginas (dashboard, login, home, etc.)
│  ├─ routers/                    # definição de rotas (Routes.jsx)
│  ├─ styles/                     # estilos globais e por página
│  └─ utils/                      # helpers: api.js, auth.js, notify, appearance, a11y
├─ index.html
├─ vite.config.js
└─ README.md
```

> Nota: a pasta `src/mocks/` era usada anteriormente para injetar um `mockFetch`. Agora o projeto foi migrado para usar `db.json` + `json-server` e a importação global do mock foi removida. Se precisar restaurar mocks locais, veja `src/mocks/dashboardMocks.js` no histórico do repositório.

## Principais tecnologias
- React (v19)
- Vite (dev server / build)
- React Router
- PrimeReact / PrimeFlex / PrimeIcons (UI)
- Chart.js + react-chartjs-2 (gráficos)
- react-datepicker
- json-server (desenvolvimento local — serve `db.json` como API REST)
- eslint

## Dicas para desenvolvimento
- Use `npm run api` + `npm run dev` em paralelo.
- Verifique `db.json` e adapte os dados para reproduzir cenários (ex.: estoque baixo, pedidos cancelados, etc.).
- Para testar integração com backend real local, execute o backend em outra porta e aponte `VITE_API_BASE_URL` para ele.
