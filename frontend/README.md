# JusTock - Frontend

Frontend da aplicação JusTock, construído com React + Vite. O projeto consome o backend real nos fluxos centrais e inclui suporte local com `json-server` para cenários complementares de desenvolvimento.

## Novidades recentes

- Tela `Produtos` reorganizada para mostrar um catálogo misto de produtos internos e anúncios do Mercado Livre
- Anúncios já vinculados agora colapsam visualmente na linha do produto interno, evitando duplicidade aparente
- Fluxo de vincular e desvincular anúncio agora usa diálogos do PrimeReact em vez de `confirm` nativo do navegador
- Coluna `Vinculação` ganhou refinamento visual e suporte consistente a tema claro/escuro
- Pedidos manuais agora trabalham com itens explícitos e refletem estoque real dos produtos selecionados
- Tela `Conexões` opera sobre uma integração Mercado Livre com retry, fila persistida de webhook e reprocessamento automático no backend
- Tema escuro foi normalizado para diálogos e dropdowns do PrimeReact usados no dashboard
- Login e cadastro agora usam o backend real via `/api/auth`
- Cadastro público disponível em `/cadastro`, inclusive a partir do modal de planos da home
- Rotas privadas do painel foram protegidas contra acesso direto sem autenticação
- Sessão do frontend agora expira com 1 hora de inatividade
- Logout agora invalida a sessão antes do redirecionamento
- Sessão do frontend agora guarda `dashboardUserId` e `primaryAdmin`
- Perfil do usuário no dashboard agora carrega e salva dados reais
- Dashboard deixou de compartilhar dados com contas não principais
- Dashboard principal agora consome atividade recente e alertas reais do backend
- Barra superior ganhou notificações reais com contador, lista visual e marcação como visualizada
- Relatórios e assinatura agora mostram estado vazio para contas novas
- Tema escuro foi ajustado para dashboard, relatórios, assinatura, produtos e pedidos
- O link `ver mais >` do gráfico principal do dashboard agora navega para `/produtos`
- A logo da sidebar foi corrigida no modo de alto contraste, evitando escala incorreta quando minimizada ou expandida
- O modal de suporte teve o contraste corrigido no tema claro
- O tema salvo no dashboard passou a valer apenas dentro do painel; a home sempre usa o tema padrão
- O hero da home teve ajuste na quebra da chamada principal para evitar uma linha isolada
- O frontend agora usa fallback automático de URLs quando `.env.local` está ausente
- Configurações do dashboard agora registram eventos de atividade no backend
- Configuração de lint do frontend foi atualizada e o projeto volta a validar com `npm run lint`

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

## Variáveis de ambiente

O frontend lê as URLs locais de `frontend/.env.local`, que já fica fora do versionamento.

Crie o arquivo local a partir do exemplo:

```powershell
Copy-Item .env.example .env.local
```

Conteúdo esperado:

```dotenv
VITE_API_BASE_URL=http://localhost:3001
VITE_BACKEND_API_BASE_URL=http://localhost:8080
```

Onde:
- `VITE_API_BASE_URL`: mock local
- `VITE_BACKEND_API_BASE_URL`: backend real

Fallback automático quando `.env.local` não existe:
- Em `localhost`: `VITE_API_BASE_URL=http://localhost:3001` e `VITE_BACKEND_API_BASE_URL=http://localhost:8080`
- Em `justock.com.br` e `www.justock.com.br`: `VITE_API_BASE_URL=https://mock.justock.com.br` e `VITE_BACKEND_API_BASE_URL=https://api.justock.com.br`

## Estado atual do frontend

### Já usando backend real

- Login
- Cadastro
- Perfil atual da conta autenticada
- Produtos
- Pedidos
- Status e ações do Mercado Livre em Conexões
- Dashboard principal, incluindo atividade recente, alertas e notificações

### Suporte local com `db.json`

- Relatórios
- Assinatura
- Usuários
- Amazon e Shopee em Conexões

## Autenticação e rotas privadas

O frontend agora trabalha com sessão autenticada nestes termos:
- `POST /api/auth/login` para login
- `POST /api/auth/register` para cadastro
- `GET /api/auth/me` para carregar o perfil atual
- `PUT /api/auth/me/profile` para atualizar nome, número e senha
- Sessão guardada em `sessionStorage`
- Expiração automática após 1 hora sem atividade do usuário
- Invalidação de sessão quando a versão interna da aplicação muda
- Redirecionamento automático para `/login` ao tentar acessar rotas privadas sem sessão válida
- Redirecionamento de volta para a rota original após login, quando aplicável
- O frontend usa `dashboardUserId` e `primaryAdmin` para decidir o contexto visível do dashboard

Rotas públicas atuais:
- `/`
- `/login`
- `/cadastro`

Rotas privadas atuais:
- `/dashboard`
- `/dashboard/conexoes`
- `/conexoes`
- `/produtos`
- `/pedidos`
- `/relatorios`
- `/configuracoes`
- `/assinatura`

Observação: o logout da barra superior limpa a sessão local antes de navegar para `/login`.

## Dashboard principal

Hoje o dashboard está assim:
- A conta principal `testeAdminSEC@exemplo.com` continua acessando o dashboard compartilhado legado
- As demais contas passam a usar o próprio contexto de dados
- `Total de Produtos`: real
- `Produtos em Baixa`: real, com regra `estoque < 3`
- `Marketplaces Conectadas`: real, hoje refletindo o status do Mercado Livre (`1` conectado, `0` desconectado)
- `Status da Sincronização`: real, `ON` ou `OFF` conforme conexão do Mercado Livre
- `Visão Geral do Inventário`: real, usando as 4 categorias com maior contagem entre os produtos reais
- O CTA `ver mais >` deste card leva o usuário para `/produtos`
- `Atividade Recente`: real, atualizada a partir dos eventos persistidos no backend e exibindo até 5 itens no bloco
- `Alertas`: reais, mostrando apenas estoque baixo e produto esgotado, também limitados aos 5 itens mais recentes do bloco
- `Notificações` na barra superior: reais, com contador de não lidas, dropdown próprio e ação de marcar como visualizada
- Para contas novas ou ainda sem dados suficientes, dashboard, relatórios e assinatura exibem estados vazios em vez de reaproveitar dados compartilhados

O gráfico de inventário já aplica marcações inteiras no eixo Y, com step adaptativo (`1`, `5`, `10`, `25`, `50`, `100`) conforme o maior valor.

Ajustes recentes de experiência e tema:
- A home sempre renderiza no tema padrão, sem herdar a preferência visual salva no dashboard
- O modal de suporte possui aparência própria e contraste legível no tema claro
- A troca entre logo expandida e compacta da sidebar não depende mais do nome do asset, evitando regressões no alto contraste

Os blocos do dashboard reagem automaticamente a mudanças vindas de:
- criação, edição e exclusão de produtos
- criação, edição e exclusão de pedidos
- sincronização manual ou automática do Mercado Livre
- salvamento de configurações e troca de tema

## Mercado Livre no frontend

O frontend já implementa:
- solicitar URL de autorização do Mercado Livre
- redirecionar o navegador para login/autorização do ML
- consultar status da conexão
- disparar sincronização manual de pedidos e produtos
- desconectar a conta integrada
- atualizar periodicamente a tela de Conexões sem reload manual
- refletir uma integração backend com retry, reprocessamento persistido de webhook e controle local de ritmo nas chamadas ao ML

Observações:
- O callback OAuth do Mercado Livre entra no backend público exposto por túnel HTTP
- Depois do callback, o backend redireciona o navegador para `http://localhost:5173/conexoes`
- Em qualquer túnel HTTP com interstitial, o navegador pode passar por uma página intermediária

### Fluxo de teste pela interface

Para testar a integração fim a fim no frontend:

1. Criar conta em `Cadastro` ou entrar pelo `Login`
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
- O catálogo exibido em `Produtos` combina produto interno e anúncio de marketplace no mesmo grid
- Quando um anúncio está vinculado, ele deixa de aparecer como linha duplicada separada e passa a ficar resumido dentro da linha do produto interno
- Quando um anúncio não está vinculado, a UI exibe a ação `Vincular produto`
- Quando um produto possui anúncios vinculados, a UI exibe o estado `Vinculado` e permite `Desvincular` por modal de confirmação
- O estado visual `Não vinculado` e o badge do Mercado Livre foram refinados para manter contraste e consistência entre tema claro e escuro
- Produtos de marketplace não podem ser editados ou excluídos manualmente
- Para contas não principais, as ações manuais ficam ocultas na interface atual

### Pedidos

- Pedidos manuais agora possuem itens explícitos e total derivado desses itens
- O estoque dos produtos escolhidos é reconciliado automaticamente pelo backend, refletindo imediatamente na UI após salvar
- Clique na linha abre modal de visualização vindo do topo
- O lápis edita apenas pedidos manuais
- Pedidos de marketplace exibem observação automática com o número externo
- Pedidos de marketplace não podem ser editados manualmente
- Para contas não principais, as ações manuais ficam ocultas na interface atual

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
- `npm run lint`: valida o código do frontend com ESLint 9

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
- Para validar autenticação, confirme também acesso direto a URLs privadas como `/pedidos` e `/produtos`
- Verifique `frontend/db.json` quando precisar reproduzir cenários nas partes ainda mockadas

