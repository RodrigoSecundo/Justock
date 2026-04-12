# Documentação da API - JusTock

## 1. Descrição geral da API

A API do JusTock faz parte do Projeto Integrador (PI) e foi desenvolvida para dar suporte ao gerenciamento de estoque, pedidos e integrações com marketplaces. O backend centraliza operações de autenticação, cadastro e consulta de produtos, controle de pedidos, administração de marketplaces e vinculação de usuários com lojas integradas.

O objetivo da API é permitir que o frontend e outros consumidores consigam manipular os dados principais do sistema por meio de rotas REST protegidas por autenticação JWT.

## 2. Objetivo do sistema

O sistema JusTock foi criado para ajudar no controle operacional de vendas e estoque, reunindo em um único ambiente:

- gerenciamento de produtos em estoque;
- gerenciamento de pedidos;
- controle de marketplaces cadastrados;
- associação entre usuários e marketplaces;
- registro de eventos de webhook;
- autenticação e controle de acesso por perfil.

## 3. URL base

Ambiente local de desenvolvimento:

```text
http://localhost:8080
```

Prefixo principal das rotas da API:

```text
/api
```

Exemplo completo:

```text
http://localhost:8080/api/products/
```

## 4. Tecnologias utilizadas

- Java 17
- Spring Boot 3.5.5
- Spring Web
- Spring Data JPA
- Spring Security
- JWT com biblioteca `jjwt`
- PostgreSQL
- Supabase como banco hospedado
- Flyway para migrations
- Maven

## 5. Formato de resposta

A API trabalha com respostas em JSON. Na maior parte das rotas, o retorno segue o envelope abaixo:

```json
{
  "status": 200,
  "message": "Mensagem da operação",
  "data": {}
}
```

Exemplo com lista:

```json
{
  "status": 200,
  "message": "Produtos encontrados!",
  "data": [
    {
      "idProduto": 1,
      "categoria": "Eletrônicos",
      "marca": "Logitech",
      "nomeDoProduto": "Mouse sem fio",
      "estado": "ATIVO",
      "preco": 129.9,
      "codigoDeBarras": "7891234567890",
      "quantidade": 25,
      "quantidadeReservada": 3,
      "marcador": "MANUAL",
      "usuario": 1
    }
  ]
}
```

Exemplo de erro de validação tratado pelo backend:

```json
{
  "status": 400,
  "message": "Erro de validação",
  "data": {
    "email": "Email inválido"
  }
}
```

## 6. Autenticação

### Tipo de autenticação

A API utiliza JWT. O token é gerado no login e deve ser enviado no header `Authorization` em todas as rotas protegidas.

O token contém o email do usuário autenticado como subject e a role no claim `role`. A expiração configurada atualmente é de 86400000 ms, ou seja, 24 horas.

### Como utilizar no header

```http
Authorization: Bearer SEU_TOKEN_JWT
```

### Fluxo básico de autenticação

1. O cliente envia email e senha para `POST /api/auth/login`.
2. A API valida as credenciais e devolve um token JWT.
3. O cliente armazena o token.
4. Nas demais rotas protegidas, o cliente envia `Authorization: Bearer <token>`.
5. Quando desejar encerrar a sessão, o cliente chama `POST /api/auth/logout` com o mesmo header. O token é inserido em blacklist.

### Perfis de acesso

- `ADMIN`: pode acessar rotas administrativas de cadastro, alteração e exclusão.
- `USER`: pode autenticar e acessar rotas que exigem apenas usuário autenticado.

### Observações reais da implementação

- Somente as rotas em `/api/auth/**` são liberadas sem autenticação.
- As demais rotas exigem token válido.
- Se um token estiver na blacklist, o filtro JWT responde HTTP `401` com o corpo abaixo, fora do envelope padrão:

```json
{
  "message": "Token invalidated, please login again"
}
```

## 7. Ferramenta de documentação escolhida

Ferramenta escolhida: Postman.

Justificativa: o projeto atual não possui integração Swagger/OpenAPI configurada no backend. Assim, o formato desta documentação foi organizado para ser facilmente convertido em uma coleção Postman com variáveis como `baseUrl` e `token`.

## 8. Endpoints documentados

Os endpoints abaixo foram documentados com base direta no código do backend do PI.

---

## 8.1 Login

**Método HTTP:** `POST`

**URL:**

```text
/api/auth/login
```

**Descrição:**

Autentica um administrador ou usuário e retorna um token JWT.

**Parâmetros:**

- Path: nenhum
- Query: nenhum
- Body:
  - `email` (string, obrigatório)
  - `password` (string, obrigatório)

**Exemplo de requisição:**

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@empresa.com",
  "password": "S@nh4secr3t4"
}
```

**Exemplo de resposta de sucesso:**

```json
{
  "status": 200,
  "message": "Login realizado com sucesso!",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "admin@empresa.com",
    "name": "Administrator",
    "role": "ADMIN"
  }
}
```

**Códigos de status HTTP:**

- `200`: login realizado com sucesso
- `400`: erro de validação no corpo da requisição
- `401`: email ou senha inválidos
- `500`: erro interno do servidor

---

## 8.2 Logout

**Método HTTP:** `POST`

**URL:**

```text
/api/auth/logout
```

**Descrição:**

Invalida o token JWT atual, adicionando-o a uma blacklist.

**Parâmetros:**

- Path: nenhum
- Query: nenhum
- Header:
  - `Authorization: Bearer <token>`
- Body: nenhum

**Exemplo de requisição:**

```http
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Exemplo de resposta de sucesso:**

```json
{
  "status": 200,
  "message": "Logout realizado com sucesso!",
  "data": "Token invalidado"
}
```

**Códigos de status HTTP:**

- `200`: logout realizado com sucesso
- `400`: token não fornecido no header
- `401`: token blacklistado pelo filtro JWT
- `500`: token malformado ou erro interno durante processamento

---

## 8.3 Listar produtos

**Método HTTP:** `GET`

**URL:**

```text
/api/products/
```

**Descrição:**

Retorna a lista de produtos cadastrados no estoque.

**Permissão:**

Usuário autenticado.

**Parâmetros:**

- Path: nenhum
- Query: nenhum
- Body: nenhum

**Exemplo de requisição:**

```http
GET /api/products/
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Exemplo de resposta de sucesso:**

```json
{
  "status": 200,
  "message": "Produtos encontrados!",
  "data": [
    {
      "idProduto": 1,
      "categoria": "Eletrônicos",
      "marca": "Logitech",
      "nomeDoProduto": "Mouse sem fio",
      "estado": "ATIVO",
      "preco": 129.9,
      "codigoDeBarras": "7891234567890",
      "quantidade": 25,
      "quantidadeReservada": 3,
      "marcador": "MANUAL",
      "usuario": 1
    }
  ]
}
```

**Códigos de status HTTP:**

- `200`: lista retornada com sucesso
- `401`: token ausente, expirado, inválido ou blacklistado
- `403`: acesso negado
- `500`: erro interno do servidor

---

## 8.4 Cadastrar produto

**Método HTTP:** `POST`

**URL:**

```text
/api/products/cadastrar
```

**Descrição:**

Cadastra um novo produto no estoque.

**Permissão:**

Apenas `ADMIN`.

**Parâmetros:**

- Path: nenhum
- Query: nenhum
- Body:
  - `categoria` (string, obrigatório)
  - `marca` (string, obrigatório)
  - `nomeDoProduto` (string, obrigatório)
  - `estado` (string, obrigatório)
  - `preco` (number, obrigatório, maior que 0)
  - `codigoDeBarras` (string, obrigatório)
  - `quantidade` (integer, obrigatório, mínimo 0)
  - `quantidadeReservada` (integer, obrigatório, mínimo 0)
  - `marcador` (string, obrigatório)
  - `usuario` (integer, obrigatório)

**Exemplo de requisição:**

```http
POST /api/products/cadastrar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

```json
{
  "categoria": "Eletrônicos",
  "marca": "Logitech",
  "nomeDoProduto": "Mouse sem fio",
  "estado": "ATIVO",
  "preco": 129.9,
  "codigoDeBarras": "7891234567890",
  "quantidade": 25,
  "quantidadeReservada": 3,
  "marcador": "MANUAL",
  "usuario": 1
}
```

**Exemplo de resposta de sucesso:**

```json
{
  "status": 200,
  "message": "Produto cadastrado com sucesso!",
  "data": {
    "idProduto": 1,
    "categoria": "Eletrônicos",
    "marca": "Logitech",
    "nomeDoProduto": "Mouse sem fio",
    "estado": "ATIVO",
    "preco": 129.9,
    "codigoDeBarras": "7891234567890",
    "quantidade": 25,
    "quantidadeReservada": 3,
    "marcador": "MANUAL",
    "usuario": 1
  }
}
```

**Códigos de status HTTP:**

- `200`: produto cadastrado com sucesso
- `400`: erro de validação
- `401`: token ausente, expirado, inválido ou blacklistado
- `403`: usuário autenticado sem perfil `ADMIN`
- `409`: conflito ou duplicidade no banco
- `500`: erro interno do servidor

---

## 8.5 Cadastrar pedido

**Método HTTP:** `POST`

**URL:**

```text
/api/Order/cadastrar
```

**Descrição:**

Cadastra um pedido no sistema. O backend valida datas e restringe os valores aceitos de status do pedido e status de pagamento.

**Permissão:**

Apenas `ADMIN`.

**Parâmetros:**

- Path: nenhum
- Query: nenhum
- Body:
  - `idPedidoMarketplace` (integer, opcional)
  - `usuarioMarketplaceId` (integer, opcional)
  - `dataEntrega` (date `yyyy-MM-dd`, opcional)
  - `dataEmissao` (date `yyyy-MM-dd`, obrigatório)
  - `statusPagamento` (string, obrigatório)
  - `statusPedido` (string, obrigatório)

**Valores aceitos:**

- `statusPedido`: `EM ANDAMENTO`, `CANCELADO`, `CONCLUIDO` no uso textual; no código a comparação esperada é `CONCLUÍDO`.
- `statusPagamento`: `PROCESSADO`, `EM PROCESSAMENTO`, `CANCELADO`, `NEGADO`.

**Regras de negócio reais do backend:**

- `dataEmissao` é obrigatória;
- `dataEmissao` não pode ser futura;
- `dataEntrega`, quando informada, não pode ser anterior a `dataEmissao`;
- `dataEntrega`, quando informada, não pode ser futura.

**Exemplo de requisição:**

```http
POST /api/Order/cadastrar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

```json
{
  "idPedidoMarketplace": 1001,
  "usuarioMarketplaceId": 1,
  "dataEntrega": "2026-04-10",
  "dataEmissao": "2026-04-08",
  "statusPagamento": "PROCESSADO",
  "statusPedido": "EM ANDAMENTO"
}
```

**Exemplo de resposta de sucesso:**

```json
{
  "status": 200,
  "message": "Order cadastrado com sucesso!",
  "data": {
    "idPedido": 15,
    "idPedidoMarketplace": 1001,
    "usuarioMarketplaceId": 1,
    "dataEntrega": "2026-04-10",
    "dataEmissao": "2026-04-08",
    "statusPagamento": "PROCESSADO",
    "statusPedido": "EM ANDAMENTO"
  }
}
```

**Códigos de status HTTP:**

- `200`: pedido cadastrado com sucesso
- `400`: erro de regra de negócio ou validação das datas/status
- `401`: token ausente, expirado, inválido ou blacklistado
- `403`: usuário autenticado sem perfil `ADMIN`
- `500`: erro interno do servidor

---

## 8.6 Listar marketplaces

**Método HTTP:** `GET`

**URL:**

```text
/api/Marketplace/
```

**Descrição:**

Retorna a lista de marketplaces cadastrados no sistema.

**Permissão:**

Usuário autenticado.

**Parâmetros:**

- Path: nenhum
- Query: nenhum
- Body: nenhum

**Exemplo de requisição:**

```http
GET /api/Marketplace/
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Exemplo de resposta de sucesso:**

```json
{
  "status": 200,
  "message": "Marketplaces encontrados!",
  "data": [
    {
      "marketplaceId": 1,
      "nomeMarketplace": "Mercado Livre",
      "apiUrl": "https://api.mercadolibre.com",
      "createdAt": "2026-04-11T10:15:00",
      "updatedAt": "2026-04-11T10:15:00"
    }
  ]
}
```

**Códigos de status HTTP:**

- `200`: lista retornada com sucesso
- `401`: token ausente, expirado, inválido ou blacklistado
- `403`: acesso negado
- `500`: erro interno do servidor

---

## 8.7 Cadastrar vinculação usuário x marketplace

**Método HTTP:** `POST`

**URL:**

```text
/api/UserMarketplace/cadastrar
```

**Descrição:**

Cadastra uma vinculação entre usuário e marketplace, incluindo dados da loja e credenciais de integração.

**Permissão:**

Apenas `ADMIN`.

**Parâmetros:**

- Path: nenhum
- Query: nenhum
- Body:
  - `usuario` (integer, obrigatório)
  - `marketplaceId` (integer, obrigatório)
  - `idLoja` (string, obrigatório)
  - `nomeLoja` (string, obrigatório)
  - `clienteId` (string, obrigatório)
  - `clienteSecret` (string, obrigatório)
  - `accessToken` (string, obrigatório)
  - `refreshToken` (string, obrigatório)
  - `tokenExpiration` (datetime ISO-8601, obrigatório)
  - `statusIntegracao` (string, obrigatório)

**Exemplo de requisição:**

```http
POST /api/UserMarketplace/cadastrar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

```json
{
  "usuario": 1,
  "marketplaceId": 1,
  "idLoja": "MLB-001",
  "nomeLoja": "Loja Principal",
  "clienteId": "client-id-app",
  "clienteSecret": "client-secret-app",
  "accessToken": "access-token-gerado",
  "refreshToken": "refresh-token-gerado",
  "tokenExpiration": "2026-04-12T12:00:00",
  "statusIntegracao": "ATIVA"
}
```

**Exemplo de resposta de sucesso:**

```json
{
  "status": 200,
  "message": "UserMarketplace cadastrado com sucesso!",
  "data": {
    "usuarioMarketplaceId": 10,
    "usuario": 1,
    "marketplaceId": 1,
    "idLoja": "MLB-001",
    "nomeLoja": "Loja Principal",
    "clienteId": "client-id-app",
    "clienteSecret": "client-secret-app",
    "accessToken": "access-token-gerado",
    "refreshToken": "refresh-token-gerado",
    "tokenExpiration": "2026-04-12T12:00:00",
    "statusIntegracao": "ATIVA"
  }
}
```

**Códigos de status HTTP:**

- `200`: vinculação cadastrada com sucesso
- `400`: erro de validação
- `401`: token ausente, expirado, inválido ou blacklistado
- `403`: usuário autenticado sem perfil `ADMIN`
- `409`: conflito ou duplicidade no banco
- `500`: erro interno do servidor

## 9. Padronização de nomenclaturas e respostas

Padrões observados no backend atual:

- todas as respostas usam JSON;
- a maior parte das rotas usa o envelope `status`, `message` e `data`;
- as rotas de leitura geralmente usam mensagens como `encontrado` ou `encontrados`;
- as rotas de escrita geralmente usam mensagens como `cadastrado com sucesso`, `atualizado` e `deletado`;
- a autenticação usa JWT no header `Authorization`;
- as rotas seguem convenções como `/visualizar/{id}`, `/cadastrar`, `/atualizar/{id}` e `/deletar/{id}`.

## 10. Observações importantes sobre a implementação atual

Para que a documentação reflita fielmente a API real do PI, é importante registrar alguns comportamentos do código atual:

- não há integração Swagger/OpenAPI configurada no projeto neste momento;
- algumas rotas CRUD retornam envelope com `status: 404` e `data: null` quando o recurso não existe, mas sem usar `ResponseEntity`; na prática, isso tende a manter HTTP `200` nessas situações;
- erros de validação, autenticação, autorização, conflito e exceções gerais são tratados pelo `GlobalExceptionHandler`, retornando HTTP `400`, `401`, `403`, `404`, `409` e `500` conforme o caso;
- o frontend do projeto já consome principalmente as rotas de produtos e pedidos do backend Spring;
- o sistema utiliza controle de acesso por role, diferenciando `ADMIN` e `USER`.

## 11. Conclusão

Esta documentação foi montada a partir da API real implementada no Projeto Integrador JusTock. Ela descreve a estrutura principal da autenticação, o formato das respostas e endpoints reais do backend, permitindo que outro desenvolvedor consiga entender e consumir a API sem depender de explicações adicionais da equipe.