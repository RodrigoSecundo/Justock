# JusTock

README principal do repositório. Este arquivo resume o estado atual do projeto e aponta para a documentação específica de frontend e backend.

## Visão geral

O projeto está dividido em duas aplicações:
- Frontend React + Vite em `frontend/`
- Backend Spring Boot em `backend/Justock-Spring/justock-api/`

Hoje o sistema opera em modo híbrido: parte dos dados vem do backend real e parte ainda vem do mock local via `json-server`.

## Novidades recentes

As mudanças mais relevantes registradas neste estado do projeto são:
- Catálogo de produtos reorganizado para usar estoque centralizado como fonte principal da verdade
- Novo modelo `marketplace_listing` para anúncios de marketplace, separado do produto interno que controla estoque
- Vínculo e desvínculo manual de anúncios do Mercado Livre com produtos internos já cadastrados
- Colapso visual de anúncios já vinculados na mesma linha do produto interno na tela `Produtos`
- Pedidos manuais agora trabalham com itens explícitos e calculam total a partir desses itens
- Reconciliação automática de estoque em pedidos manuais, inclusive reversão ao cancelar ou editar itens
- Sincronização de estoque do Mercado Livre corrigida para pausar anúncio ao zerar estoque e reativar no reabastecimento
- Normalização de marca do Mercado Livre ampliada com fallback por atributos, variações e inferência pelo título
- Backfill Flyway para normalizar marcas antigas persistidas como `N/A`
- Autenticação do frontend conectada ao backend real com login e cadastro por API
- Cadastro público exposto na rota `/cadastro`, inclusive a partir do modal de planos da home
- Rotas privadas do painel protegidas contra acesso direto sem sessão válida
- Sessão do frontend com expiração por 1 hora de inatividade e invalidação entre versões do app
- Logout corrigido para realmente invalidar a sessão antes do redirecionamento
- Resolução da conta autenticada com distinção entre conta principal e contas isoladas
- Perfil do usuário conectado com leitura e atualização reais de nome, número e senha
- Dashboard principal com `Atividade Recente` e `Alertas` vindos do backend real
- Notificações reais na barra superior, com contador, marcação como visualizada e reaproveitamento dos alertas de estoque
- Registro persistente de eventos do dashboard para ações de produtos, pedidos, sincronizações e mudanças de configuração/tema
- Estados vazios para contas novas em dashboard, relatórios e assinatura
- Ajustes visuais de tema escuro para dashboard, relatórios, assinatura, produtos e pedidos
- O link `ver mais >` do gráfico principal do dashboard agora redireciona para `Produtos`
- A logo da barra lateral foi estabilizada no modo de alto contraste, tanto minimizada quanto expandida
- O modal de suporte recebeu correções de contraste no tema claro e refinamento visual entre home e dashboard
- O tema salvo pelo usuário no dashboard agora fica restrito ao painel interno; a home sempre usa o tema padrão
- O hero da home teve ajuste de quebra de linha para evitar linhas isoladas na chamada principal
- O frontend agora possui fallback automático de URLs quando `frontend/.env.local` não existe
- Lint do frontend ajustado para o formato atual do ESLint 9

## Estado atual dos dados

### Backend real

Estas áreas já usam o backend Spring + PostgreSQL/Supabase:
- Login e cadastro
- Produtos
- Pedidos
- Integração com Mercado Livre
- Dashboard principal, incluindo atividade recente, alertas e notificações

### Mock local (`frontend/db.json`)

Estas áreas ainda dependem total ou parcialmente do mock:
- Relatórios
- Assinatura
- Usuários
- Amazon e Shopee em Conexões

Observação importante:
- `Relatórios` e `Assinatura` ainda usam mock, mas agora já distinguem o comportamento da conta principal e de contas novas, inclusive com estados vazios controlados

## Autenticação e sessão

O fluxo atual de autenticação funciona assim:
- `Login` e `Cadastro` consomem o backend real em `/api/auth`
- O login retorna informações da conta autenticada, incluindo `dashboardUserId` e indicador de `primaryAdmin`
- Rotas privadas como `/dashboard`, `/pedidos`, `/produtos`, `/conexoes`, `/relatorios`, `/configuracoes` e `/assinatura` exigem sessão válida
- A sessão do frontend fica em `sessionStorage`, não em `localStorage`
- Após 1 hora de inatividade, a sessão expira e o usuário volta para a tela de login
- Quando a versão interna do frontend muda, sessões antigas são invalidadas para evitar reaproveitamento entre builds
- O logout limpa a sessão local antes de redirecionar para `/login`
- O frontend consulta e atualiza o perfil atual em `/api/auth/me` e `/api/auth/me/profile`

Fluxos expostos hoje na interface:
- `/login` para autenticação
- `/cadastro` para criação de conta

## Dashboard hoje

Os cards e gráficos do dashboard principal estão neste estado:
- A conta principal `testeAdminSEC@exemplo.com` continua acessando o dashboard compartilhado legado
- As demais contas usam o próprio contexto de dados e não reaproveitam mais o dashboard compartilhado
- `Total de Produtos`: real, somando o estoque dos produtos vindos do backend
- `Produtos em Baixa`: real, contando produtos com estoque menor que `3`
- `Marketplaces Conectadas`: real, hoje fica `1` quando o Mercado Livre está conectado e `0` quando não está
- `Status da Sincronização`: real, hoje fica `ON` com Mercado Livre conectado e `OFF` sem conexão
- `Visão Geral do Inventário`: real, usando as 4 categorias com maior ocorrência nos produtos reais; se houver menos de 4 categorias distintas, exibe uma barra default zerada
- O atalho `ver mais >` deste bloco leva para a tela `Produtos`
- `Atividade Recente`: real, persistida no backend e limitada aos 5 registros mais recentes exibidos no bloco
- `Alertas`: real, exibindo apenas produtos esgotados ou em baixa (`estoque < 3`), também limitado aos 5 alertas mais recentes exibidos no bloco
- `Notificações`: reais na barra superior, com contador de não lidas, lista visual própria e ação de marcar como visualizada
- Para contas novas ou ainda sem movimentação suficiente, o dashboard mostra estados vazios em vez de dados compartilhados

Comportamentos visuais recentes do frontend:
- A home mantém sempre o tema padrão, independentemente do tema escolhido dentro do dashboard
- O modal de suporte da home não reaproveita mais inadvertidamente o tema escuro salvo no painel
- A sidebar do dashboard preserva proporção correta da logo também em alto contraste

## Produtos e pedidos

### Produtos

- CRUD manual usa backend real e fica associado ao contexto da conta autenticada
- O estoque interno continua sendo a fonte principal da verdade para produtos próprios e anúncios vinculados
- Anúncios do Mercado Livre agora são persistidos como `marketplace_listing`, sem duplicar automaticamente um produto principal no catálogo
- Quando um anúncio é vinculado a um produto interno, ele passa a compartilhar o mesmo estoque desse produto
- Quando o anúncio é mantido separado, o sistema conserva um produto sombra de estoque com marcador `ANUNCIO_ESTOQUE`
- A tela `Produtos` colapsa anúncios já vinculados dentro da linha do produto interno e mantém visíveis apenas os anúncios ainda não vinculados
- Produtos sincronizados do Mercado Livre ficam identificados como origem de marketplace
- Produtos de marketplace não podem ser editados nem excluídos manualmente, a UI mantém os botões visivelmente bloqueados e exibe erro ao clicar
- Para contas que não são a principal, as ações manuais de produtos ficam ocultas na interface atual

### Pedidos

- CRUD manual usa backend real e respeita o contexto da conta autenticada
- Pedidos manuais agora salvam itens explícitos com produto, quantidade, preço unitário e subtotal
- O valor total do pedido é calculado no backend a partir da soma dos subtotais dos itens
- O estoque dos itens é aplicado automaticamente em pedidos manuais não cancelados e revertido quando necessário em edição, cancelamento ou exclusão
- Produtos internos e anúncios separados com estoque próprio podem ser usados em pedidos manuais
- Validações manuais ativas:
  - Data de emissão obrigatória e não futura
  - Data de entrega opcional, mas nunca anterior à emissão nem futura
  - Status do pedido manual: `EM ANDAMENTO`, `CANCELADO`, `CONCLUÍDO`
  - Status de pagamento manual: `PROCESSADO`, `EM PROCESSAMENTO`, `CANCELADO`, `NEGADO`
- Pedidos sincronizados do Mercado Livre são somente leitura no fluxo manual
- Clique na linha do pedido abre modal de visualização
- Lápis edita apenas pedidos manuais
- Pedidos de marketplace exibem observação automática com o número externo do marketplace
- Para contas que não são a principal, as ações manuais de pedidos ficam ocultas na interface atual

## Mercado Livre

A integração com Mercado Livre já suporta:
- OAuth com Authorization Code + PKCE
- Conectar e desconectar conta vendedora
- Renovação de token por `refresh_token`
- Sincronização manual de produtos e pedidos
- Sincronização automática periódica no backend
- Registro de webhooks recebidos
- Upsert por `marketplace_resource_id`
- Remoção de dados antigos quando a conta compartilhada é trocada e uma nova sincronização roda
- Persistência dedicada de anúncios em `marketplace_listing`
- Vínculo manual de anúncios a produtos internos do Justock
- Normalização de marca por atributos do item, variações e inferência pelo título
- Normalização de categoria por `category_id` do ML com fallback por heurística no nome do produto
- Pausa automática do anúncio quando o estoque compartilhado chega a zero, com reativação no reabastecimento

Detalhes operacionais importantes:
- O projeto mantém um usuário interno compartilhado do Justock para a conta principal (`mercadolivre.shared.usuario-id`)
- A conta principal é definida por `justock.primary-admin.email` e por padrão é `testeAdminSEC@exemplo.com`
- Fora essa conta principal, os demais usuários usam o próprio contexto de dashboard e integração
- A conta conectada do Mercado Livre pode ser trocada
- A primeira sincronização automática roda 2 minutos após subir o backend
- As próximas sincronizações automáticas rodam a cada 15 minutos
- Sincronizações do Mercado Livre também alimentam a atividade recente e os alertas do dashboard quando produtos ou pedidos são criados/atualizados
- O backfill Flyway atual do backend vai até `V15`, incluindo consolidação de inventário legado do ML e normalização de marcas antigas

### Fluxo de testes homologado

O Mercado Livre não fornece sandbox separado para pedidos, anúncios e compras. O fluxo oficial de testes é feito em produção com usuários de teste.

Resumo do processo:
- Criar um vendedor de teste e um comprador de teste via API do Mercado Livre
- Autorizar a aplicação do Justock com o vendedor de teste
- Publicar um anúncio de teste com o vendedor de teste
- Comprar esse anúncio com o comprador de teste
- Sincronizar produtos e pedidos no Justock
- Validar os dados em `Pedidos` e no card de `Conexões`

Observações importantes:
- Não usar conta real para comprar ou vender no fluxo de testes
- Usuários de teste podem expirar por inatividade e não devem ser tratados como permanentes
- Se a validação de e-mail for solicitada em uma conta de teste, o código é formado pelos últimos dígitos do `id` do usuário de teste
- As credenciais geradas para usuários de teste devem ser guardadas fora do repositório

### Criação de usuários de teste

Depois de obter um `access_token` válido do dono da aplicação do ML, os usuários de teste podem ser criados via terminal:

```powershell
curl -X POST \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"site_id":"MLB"}' \
  https://api.mercadolibre.com/users/test_user
```

Executar o comando duas vezes:
- uma para o vendedor de teste
- outra para o comprador de teste

### Verificação de pedido no Justock

Após a compra de teste:
- usar `Sincronizar pedidos e produtos` na tela `Conexões` ou `POST /api/mercadolivre/sync`
- conferir se o pedido apareceu na tela `Pedidos`
- conferir se o card do Mercado Livre em `Conexões` mostra os totais atualizados

O card de `Conexões` usa dados reais do backend:
- `totalVendas`: total retornado por `/orders/search`
- `pedidosAtivos`: total filtrado de pedidos ativos no ML
- `totalInventario`: total retornado por `/users/{seller}/items/search`

Foi corrigido um problema em que um filtro inválido de status do ML zerava todos os totais do card, mesmo quando havia pedido e inventário válidos.

## Pré-requisitos

### Frontend

- Node.js 20+
- npm

### Backend

- JDK 21
- Maven 3.9+ ou uso do Maven Wrapper incluído no projeto

## Como rodar localmente

Recomendado: abrir 3 terminais na raiz do repositório.

### 1. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Aplicação em `http://localhost:5173`

### 2. Mock local

```powershell
cd frontend
npm run api
```

Mock em `http://localhost:3001`, lendo `frontend/db.json`

### 3. Backend

```powershell
cd backend/Justock-Spring/justock-api
./mvnw.cmd spring-boot:run
```

No Windows, o `mvnw.cmd` do projeto tenta selecionar automaticamente um JDK 21 instalado em `%USERPROFILE%\.jdk\jdk-21*`, então o comando acima deve funcionar sem preparação manual do terminal.

Se não houver um JDK 21 nesse caminho, aí sim será necessário configurar `JAVA_HOME`/`Path` manualmente.

API em `http://localhost:8080`

Para validar compilação do backend:

```powershell
cd backend/Justock-Spring/justock-api
./mvnw.cmd -q -DskipTests compile
```

## Variáveis de ambiente do frontend

O frontend lê as URLs locais a partir de `frontend/.env.local`, que já fica fora do versionamento por causa do `.gitignore` do Vite.

Use `frontend/.env.example` como base e crie seu arquivo local:

```powershell
cd frontend
Copy-Item .env.example .env.local
```

Valores esperados:

```dotenv
VITE_API_BASE_URL=http://localhost:3001
VITE_BACKEND_API_BASE_URL=http://localhost:8080
```

Se `frontend/.env.local` não existir, o frontend agora usa fallback automático:
- Em ambiente local: `http://localhost:3001` e `http://localhost:8080`
- Em `justock.com.br` ou `www.justock.com.br`: `https://mock.justock.com.br` e `https://api.justock.com.br`

## Configuração sensível do backend

As propriedades sensíveis do backend não ficam mais no arquivo versionado. Agora o Spring importa um arquivo local opcional em `backend/Justock-Spring/justock-api/application-local.properties`.

Use `backend/Justock-Spring/justock-api/application-local.example.properties` como base:

```powershell
cd backend/Justock-Spring/justock-api
Copy-Item application-local.example.properties application-local.properties
```

Depois de copiar, preencha o `application-local.properties` com os valores reais do seu ambiente antes de rodar o backend.

Exemplo do conteúdo esperado:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha
JWT_SECRET=sua_chave_jwt_com_pelo_menos_32_caracteres
MERCADOLIVRE_CLIENT_ID=seu_app_id
MERCADOLIVRE_CLIENT_SECRET=sua_secret_key
MERCADOLIVRE_REDIRECT_URI=https://seu-dominio/api/mercadolivre/callback
MERCADOLIVRE_FRONTEND_REDIRECT_URI=http://localhost:5173/conexoes
```

Notas:
- `mercadolivre.redirect.uri` deve ser idêntico ao callback cadastrado no app do Mercado Livre
- O callback do ML entra pelo backend público e depois redireciona para o frontend local em `Conexões`
- Em qualquer túnel HTTP com interstitial, o navegador pode passar por uma tela intermediária antes do retorno
- O backend continua exigindo JDK 21; o wrapper só automatiza a escolha desse Java no Windows quando ele já está instalado no caminho esperado

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
└─ README.md
```

## Documentação dedicada

- Frontend: `frontend/README.md`
- Backend: `backend/README.MD`
