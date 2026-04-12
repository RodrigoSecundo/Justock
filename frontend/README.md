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

- `testeAdminSEC@exemplo.com` / `S@nh4secr3t4`

## Variáveis de ambiente
Você pode apontar o frontend para outro backend definindo `VITE_API_BASE_URL` (ex.: `.env` ou na sua sessão):
```powershell
# Windows PowerShell
$env:VITE_API_BASE_URL = "https://api.meuservidor.com"
npm run dev
```

Para o cenário atual do projeto, o frontend usa preferencialmente:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:3001"
$env:VITE_BACKEND_API_BASE_URL = "http://localhost:8080"
npm run dev
```

## Integração atual com o backend

O frontend está em modo híbrido:
- Produtos e pedidos já consomem backend real
- A integração com Mercado Livre da tela de conexões já consome backend real
- Dashboard, relatórios, assinatura e outras partes ainda usam `db.json` em diferentes pontos

### Mercado Livre no frontend

O frontend já possui fluxo para:
- solicitar URL de autorização do Mercado Livre
- redirecionar o navegador para o login/autorização do ML
- consultar status da conexão
- disparar sincronização manual de pedidos e produtos
- desconectar a conta integrada
- atualizar periodicamente a tela de conexões sem recarregar a página

Observações:
- O retorno do OAuth do Mercado Livre acontece no backend público exposto por túnel HTTP.
- Depois do callback, o backend redireciona o navegador de volta para `http://localhost:5173/conexoes`.
- Se estiver usando ngrok free, o navegador pode passar pela página de aviso do próprio ngrok durante esse retorno.

Comportamento atual da página de Conexões:
- O card do Mercado Livre usa dados reais do backend para status, total de vendas, pedidos ativos, inventário e conta conectada.
- Amazon e Shopee continuam mockados e desconectados.
- O botão de sincronização do Mercado Livre fica centralizado em Conexões com o texto `Sincronizar pedidos e produtos`.

Comportamento atual da página de Pedidos:
- Pedidos sincronizados do Mercado Livre aparecem com identificação de origem do marketplace.
- Pedidos sincronizados do Mercado Livre são tratados como somente leitura.
- Pedidos manuais continuam podendo ser criados e editados normalmente.

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

