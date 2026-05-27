# JusTock - Backend

Backend da aplicação JusTock, construído com Spring Boot, Spring Security, JPA/Hibernate, Flyway e PostgreSQL.

## Novidades recentes

- Endpoints de autenticação para login e cadastro integrados ao frontend
- Endpoints para consultar e atualizar o perfil da conta autenticada
- Resolução centralizada da conta atual com distinção entre conta principal e contas isoladas
- Nova persistência de eventos do dashboard com Flyway `V10`
- Endpoints reais para atividade recente, alertas e notificações do dashboard
- Registro automático de eventos em produtos, pedidos, sincronização do Mercado Livre e mudanças de configuração/tema
- Contador de notificações não lidas e marcação individual como visualizada
- Exibição limitada dos blocos de atividade recente e alertas aos 5 itens mais novos, sem perder a persistência dos eventos

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL / Supabase

## Requisitos

- JDK 21
- Maven 3.9+ ou uso do Maven Wrapper incluído no projeto
- IDE de sua preferência

No VS Code, recomenda-se:
- Java Extension Pack
- Spring Boot Extension Pack

## Execução local

Na pasta `backend/Justock-Spring/justock-api`:

```powershell
./mvnw.cmd spring-boot:run
```

No Windows, o `mvnw.cmd` do projeto tenta selecionar automaticamente um JDK 21 instalado em `%USERPROFILE%\.jdk\jdk-21*`, então o comando acima deve funcionar sem export manual de `JAVA_HOME`.

Se esse JDK 21 não existir nesse caminho, configure `JAVA_HOME`/`Path` manualmente antes de rodar o wrapper.

Backend padrão em `http://localhost:8080`

Para validar compilação:

```powershell
./mvnw.cmd -q -DskipTests compile
```

O projeto continua compilando com `java.version=21`; o wrapper só evita que o terminal use por engano um Java mais antigo quando encontra um JDK 21 local compatível.

Para rodar testes:

```powershell
./mvnw.cmd test
```

## Banco de dados

- Banco atual: PostgreSQL / Supabase
- Migrations gerenciadas com Flyway
- O histórico atual do projeto vai até `V10`

## O que já está real no backend

- Login e cadastro com emissão de token
- Leitura e atualização do perfil atual via autenticação JWT
- CRUD de produtos
- CRUD de pedidos
- Integração com Mercado Livre
- Resumo de status do Mercado Livre para a tela de Conexões
- Feed de atividade recente do dashboard
- Alertas de estoque do dashboard
- Notificações do dashboard com estado de leitura

## Regras de negócio atuais

### Autenticação

- `POST /api/auth/login` autentica o usuário e retorna o token consumido pelo frontend
- `POST /api/auth/register` cria uma nova conta
- `GET /api/auth/me` retorna os dados atuais da conta autenticada
- `PUT /api/auth/me/profile` atualiza nome, número e senha da conta atual
- O login e o endpoint de perfil retornam `dashboardUserId` e `primaryAdmin` para o frontend decidir o contexto de dados
- O frontend usa esse token para proteger o acesso ao painel e APIs autenticadas

### Conta principal e isolamento de dados

- A conta principal é definida pela propriedade `justock.primary-admin.email`
- Por padrão, a conta principal é `testeAdminSEC@exemplo.com`
- Apenas a conta principal reaproveita o dashboard compartilhado legado via `mercadolivre.shared.usuario-id`
- As demais contas usam o próprio `id` como contexto de dashboard, produtos, pedidos e integração
- A resolução desse contexto fica concentrada em `CurrentAccountService`

### Produtos

- Leituras e gravações usam o contexto resolvido da conta autenticada
- Produtos manuais podem ser criados, editados e excluídos
- Produtos sincronizados de marketplace não podem ser editados ou excluídos manualmente via API do Justock
- Produtos sincronizados do Mercado Livre são identificados por `marketplace_source`, `marketplace_resource_id` e marcador `ML`
- Criação, edição, exclusão e sincronização de produtos alimentam os eventos do dashboard

### Pedidos

- Leituras e gravações usam o contexto resolvido da conta autenticada
- Pedidos manuais seguem validações de data e status
- Pedidos sincronizados de marketplace não podem ser editados ou excluídos manualmente via API do Justock
- Pedidos do Mercado Livre recebem observação automática com o número externo do marketplace
- Criação, edição, exclusão e sincronização de pedidos alimentam os eventos do dashboard

### Dashboard e notificações

- `Atividade Recente` é persistida na tabela `dashboard_event`
- `Alertas` consideram apenas produtos com estoque zerado ou abaixo do limite atual (`< 3`)
- O sino de notificações usa os mesmos alertas de estoque, com contador real de não lidas
- O feed visual de atividade, alertas e dropdown de notificações retorna até 5 itens por vez
- O total de notificações não lidas é calculado separadamente para não distorcer o badge do frontend

## Integração com Mercado Livre

### O que já está implementado

- Geração de URL de autorização OAuth
- Fluxo de callback com Authorization Code + PKCE
- Persistência de `access_token`, `refresh_token`, expiração e dados da loja em `cliente_marketplace`
- Renovação automática de token com `refresh_token`
- Endpoint para status da conexão
- Endpoint para desconectar a conta integrada
- Sincronização manual de produtos e pedidos
- Sincronização automática periódica por agendamento (`@Scheduled`)
- Upsert de produtos e pedidos por `marketplace_resource_id`
- Limpeza de registros antigos ao trocar a conta conectada e executar nova sincronização
- Registro de payload bruto de webhook em `eventos_webhook`
- Extração de marca por atributos do item
- Normalização de categoria do item com `category_id` do ML e fallback por heurística no título
- Resumo de conexão com totais de pedidos e inventário para a tela `Conexões`
- Resolução do usuário de dashboard a partir da conta autenticada antes de consultar status, sincronizar ou desconectar

### O que ainda não está implementado

- Processamento assíncrono dos webhooks recebidos
- Sincronização dos itens de pedido em `estoque_pedido`

### Endpoints do Mercado Livre

- `GET /api/mercadolivre/auth-url`
- `GET /api/mercadolivre/status`
- `GET /api/mercadolivre/callback`
- `POST /api/mercadolivre/sync`
- `POST /api/mercadolivre/disconnect`
- `POST /api/mercadolivre/webhook`

## Endpoints de autenticação

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `PUT /api/auth/me/profile`

## Endpoints do dashboard

- `GET /api/dashboard/recent-activity`
- `GET /api/dashboard/alerts`
- `GET /api/dashboard/notifications`
- `POST /api/dashboard/notifications/{eventId}/read`
- `POST /api/dashboard/activity`

### Sincronização automática

Por padrão:
- primeira execução: 2 minutos após subir o backend
- próximas execuções: a cada 15 minutos

Propriedades:

```properties
mercadolivre.auto-sync.enabled=true
mercadolivre.auto-sync.fixed-delay-ms=900000
mercadolivre.auto-sync.initial-delay-ms=120000
```

### Configuração local esperada no `application-local.properties`

O arquivo versionado ficou limpo e agora o backend importa um arquivo local ignorado pelo Git. Crie o arquivo a partir do modelo:

```powershell
Copy-Item application-local.example.properties application-local.properties
```

Depois disso, troque os placeholders pelos valores reais do seu ambiente.

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha
JWT_SECRET=sua_chave_jwt_com_pelo_menos_32_caracteres
JUSTOCK_PRIMARY_ADMIN_EMAIL=testeAdminSEC@exemplo.com
MERCADOLIVRE_CLIENT_ID=seu_app_id
MERCADOLIVRE_CLIENT_SECRET=sua_secret_key
MERCADOLIVRE_REDIRECT_URI=https://seu-tunel.ngrok-free.dev/api/mercadolivre/callback
MERCADOLIVRE_FRONTEND_REDIRECT_URI=http://localhost:5173/conexoes
MERCADOLIVRE_SHARED_USUARIO_ID=1
MERCADOLIVRE_AUTO_SYNC_ENABLED=true
MERCADOLIVRE_AUTO_SYNC_FIXED_DELAY_MS=900000
MERCADOLIVRE_AUTO_SYNC_INITIAL_DELAY_MS=120000
```

### Observações práticas

- `mercadolivre.redirect.uri` precisa bater exatamente com a URL cadastrada no app do Mercado Livre
- O backend recebe o callback pelo túnel HTTP público e redireciona para o frontend local
- Em qualquer túnel HTTP com interstitial, o navegador pode passar por uma página intermediária antes do callback final
- O projeto usa hoje um usuário interno compartilhado (`mercadolivre.shared.usuario-id=1`) apenas para a conta principal
- Ao trocar a conta do Mercado Livre conectada, os produtos e pedidos antigos da conta anterior são removidos quando a nova sincronização roda
- O bloqueio manual de edição e exclusão não interfere na própria sincronização automática/manual do Mercado Livre, porque ela usa o serviço interno de sync e não o CRUD manual do Justock

## Testes com Mercado Livre

### Premissa

O Mercado Livre não oferece sandbox separado para pedidos. O fluxo oficial de testes é feito com usuários de teste no ambiente real da plataforma.

No Justock, o fluxo recomendado é:
- criar um vendedor de teste e um comprador de teste
- conectar o vendedor de teste pelo OAuth do Justock
- publicar um item de teste
- comprar com o comprador de teste
- sincronizar produtos e pedidos pelo backend

### Criando usuários de teste

Com um `access_token` válido do dono da aplicação do Mercado Livre, é possível criar usuários de teste por terminal:

```powershell
curl -X POST \
	-H "Authorization: Bearer SEU_ACCESS_TOKEN" \
	-H "Content-Type: application/json" \
	-d '{"site_id":"MLB"}' \
	https://api.mercadolibre.com/users/test_user
```

Executar duas vezes, guardando as respostas:
- vendedor de teste
- comprador de teste

Notas:
- as credenciais retornadas não devem ser versionadas no repositório
- usuários de teste podem expirar por inatividade
- se o ML pedir validação por e-mail para usuário de teste, o código é formado pelos últimos dígitos do `id` do usuário

### Fluxo de ponta a ponta no backend

1. Gerar a URL com `GET /api/mercadolivre/auth-url`
2. Autorizar a aplicação com o vendedor de teste no navegador
3. Confirmar a conexão com `GET /api/mercadolivre/status`
4. Publicar um anúncio com a conta de teste do vendedor
5. Comprar com a conta de teste do comprador
6. Rodar `POST /api/mercadolivre/sync`
7. Verificar produtos e pedidos gravados no banco

### Totais exibidos em Conexões

O resumo do card do Mercado Livre é calculado a partir de chamadas reais da API:
- `totalVendas`: `/orders/search?seller=...`
- `pedidosAtivos`: `/orders/search?seller=...&order.status=...`
- `totalInventario`: `/users/{seller}/items/search?limit=1`

Foi corrigido um problema no backend em que um filtro inválido de status do ML fazia a chamada de `pedidosAtivos` falhar e zerava também `totalVendas` e `totalInventario`. Agora cada métrica falha isoladamente.

