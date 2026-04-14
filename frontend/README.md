# JusTock - Frontend

Frontend da aplicação JusTock, construído com React + Vite. O projeto consome uma combinação de backend real e mock local via `json-server`.

## Requisitos

- Node.js 20+
- npm

## Instalação

```powershell
npm install
```

## Execução local

Abra dois terminais em `frontend/`.

### Terminal 1 - mock local

```powershell
npm run api
```

Mock em `http://localhost:3001`, lendo `frontend/db.json`.

### Terminal 2 - frontend

```powershell
npm run dev
```

Aplicação em `http://localhost:5173`.

## Credenciais de login (dev)

- `testeAdminSEC@exemplo.com` / `S@nh4secr3t4`

## Variáveis de ambiente

O frontend usa preferencialmente:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:3001"
$env:VITE_BACKEND_API_BASE_URL = "http://localhost:8080"
npm run dev
```

Onde:
- `VITE_API_BASE_URL`: mock local
- `VITE_BACKEND_API_BASE_URL`: backend real

## Estado atual do frontend

### Já usando backend real

- Produtos
- Pedidos
- Status e ações do Mercado Livre em Conexões
- Parte do dashboard principal

### Ainda usando mock total ou parcial

- Relatórios
- Assinatura
- Usuários
- Atividade recente do dashboard
- Alertas do dashboard
- Amazon e Shopee em Conexões

## Dashboard principal

Hoje o dashboard está assim:
- `Total de Produtos`: real
- `Produtos em Baixa`: real, com regra `estoque < 3`
- `Marketplaces Conectadas`: real, hoje refletindo o status do Mercado Livre (`1` conectado, `0` desconectado)
- `Status da Sincronização`: real, `ON` ou `OFF` conforme conexão do Mercado Livre
- `Visão Geral do Inventário`: real, usando as 4 categorias com maior contagem entre os produtos reais
- `Atividade Recente`: mock
- `Alertas`: mock

O gráfico de inventário já aplica marcações inteiras no eixo Y, com step adaptativo (`1`, `5`, `10`, `25`, `50`, `100`) conforme o maior valor.

## Mercado Livre no frontend

O frontend já implementa:
- solicitar URL de autorização do Mercado Livre
- redirecionar o navegador para login/autorização do ML
- consultar status da conexão
- disparar sincronização manual de pedidos e produtos
- desconectar a conta integrada
- atualizar periodicamente a tela de Conexões sem reload manual

Observações:
- O callback OAuth do Mercado Livre entra no backend público exposto por túnel HTTP
- Depois do callback, o backend redireciona o navegador para `http://localhost:5173/conexoes`
- Em ngrok free, o navegador pode passar pela página de aviso do próprio túnel

### Fluxo de teste pela interface

Para testar a integração fim a fim no frontend:

1. Entrar no Justock
2. Ir para `Conexões`
3. Clicar em `Conectar` no card do Mercado Livre
4. Fazer login no Mercado Livre com o vendedor de teste
5. Autorizar a aplicação
6. Voltar para `Conexões` e confirmar que a conta ficou conectada
7. Publicar um item de teste com o vendedor de teste
8. Comprar esse item com o comprador de teste em outra janela do navegador
9. Voltar para o Justock e usar `Sincronizar pedidos e produtos`
10. Conferir o pedido em `Pedidos` e os totais no card de `Conexões`

Regras importantes:
- não usar conta real no fluxo de compra e venda de teste
- contas de teste do ML devem ser mantidas fora do repositório
- se o ML pedir código de verificação de e-mail para usuário de teste, usar os últimos dígitos do `id` da conta de teste

### Tela Conexões

O card do Mercado Livre em `Conexões` consome `GET /api/mercadolivre/status` e mostra:
- `Total de vendas`
- `Pedidos ativos`
- `Quant. Inventário`
- `Conta`

Esse card é atualizado automaticamente em polling a cada 30 segundos e também após:
- conectar a conta
- sincronizar pedidos e produtos
- desconectar a conta

Foi corrigido um problema no backend que fazia os totais permanecerem zerados quando a consulta de pedidos ativos retornava erro de filtro no Mercado Livre.

## Produtos e pedidos no frontend

### Produtos

- Produtos sincronizados de marketplace aparecem identificados
- Produtos de marketplace não podem ser editados ou excluídos manualmente
- Os botões continuam com aparência bloqueada e exibem toast de erro ao clicar

### Pedidos

- Clique na linha abre modal de visualização vindo do topo
- O lápis edita apenas pedidos manuais
- Pedidos de marketplace exibem observação automática com o número externo
- Pedidos de marketplace não podem ser editados manualmente
- O botão bloqueado continua clicável só para exibir o toast de erro

## Estrutura do projeto

```text
frontend/
├─ db.json
├─ package.json
├─ public/
├─ src/
│  ├─ assets/
│  ├─ components/
│  │  ├─ common/
│  │  └─ dashboard/
│  ├─ pages/
│  ├─ routers/
│  ├─ styles/
│  └─ utils/
├─ index.html
├─ vite.config.js
└─ README.md
```

## Scripts

- `npm run dev`: inicia o Vite
- `npm run build`: gera build de produção
- `npm run preview`: visualiza build localmente
- `npm run api`: sobe o `json-server` usando `db.json`

## Principais tecnologias

- React 19
- Vite 7
- React Router
- PrimeReact / PrimeFlex / PrimeIcons
- Chart.js + react-chartjs-2
- react-datepicker
- json-server
- ESLint

## Dicas de desenvolvimento

- Use `npm run api` + `npm run dev` quando estiver mexendo em telas ainda híbridas ou mockadas
- Para validar apenas produtos, pedidos e Mercado Livre, backend + frontend costumam ser suficientes
- Verifique `frontend/db.json` quando precisar reproduzir cenários nas partes ainda mockadas

