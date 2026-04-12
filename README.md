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
  - `GET /api/mercadolivre/status`
  - `GET /api/mercadolivre/auth-url`
  - `POST /api/mercadolivre/sync`
  - `POST /api/mercadolivre/disconnect`
  - `POST /api/mercadolivre/webhook`
- **Rotas ainda mockadas via `json-server` (`frontend/db.json`):**
  - dashboard, relatórios, assinatura, usuários e demais objetos ainda não migrados
  - a tela de conexões segue híbrida: Mercado Livre usa backend real, enquanto Amazon e Shopee continuam mockados

Observação importante sobre o Dashboard:
- O card **Total de Produtos** já usa dados reais do backend, somando a coluna `quantidade` da tabela `estoque` (via `GET /api/products/`).
- O card **Produtos em Baixa** permanece mockado no `db.json` por enquanto.
Ou seja: **Produtos e Pedidos já usam backend real**, enquanto outras telas ainda podem depender do mock.

Observação importante sobre Conexões:
- O card do **Mercado Livre** já usa backend real para status da conexão e métricas da conta conectada.
- Os cards de **Amazon** e **Shopee** continuam mockados e marcados como não conectados.

### Nota de atualização - Pedidos
- O fluxo de **Pedidos** recebeu validações no frontend e no backend para evitar dados inconsistentes.
- **Data de emissão**: obrigatória, só pode ser hoje ou uma data passada.
- **Data de entrega**: opcional, mas quando informada deve ficar entre a data de emissão e a data atual; não pode ser anterior à emissão nem futura.
- **Status do pedido** aceito para inserções manuais: `EM ANDAMENTO`, `CANCELADO` e `CONCLUÍDO`.
- **Status de pagamento** aceito para inserções manuais: `PROCESSADO`, `EM PROCESSAMENTO`, `CANCELADO` e `NEGADO`.
- Pedidos antigos fora desses padrões foram normalizados no banco por migrations do Flyway.
- Pedidos sincronizados do Mercado Livre são gravados com metadados de origem do marketplace e aparecem separados dos pedidos manuais.
- Pedidos sincronizados do Mercado Livre são tratados como somente leitura no frontend.

### Nota de atualização - Mercado Livre
- A integração com o Mercado Livre está funcional para **conectar conta vendedora**, **renovar token**, **sincronizar produtos e pedidos**, **desconectar** e **manter sincronização automática periódica**.
- O fluxo OAuth agora usa **Authorization Code + PKCE**, exigido pela configuração atual da aplicação no Mercado Livre.
- O callback público configurado no app do Mercado Livre deve apontar para o backend exposto pelo túnel HTTP, por exemplo:
  - `https://SEU-TUNEL.ngrok-free.dev/api/mercadolivre/callback`
- O webhook público pode apontar para:
  - `https://SEU-TUNEL.ngrok-free.dev/api/mercadolivre/webhook`
- O projeto opera hoje no modo **usuário interno compartilhado** do Justock. Ou seja: a conta interna padrão do sistema é a mesma, mas a conta vendedora conectada no Mercado Livre pode ser trocada.
- A sincronização manual agora fica centralizada no card do **Mercado Livre** em **Conexões**, com o botão **Sincronizar pedidos e produtos**.
- Ao sincronizar, o backend faz **upsert** por `marketplace_resource_id` tanto para produtos quanto para pedidos e remove registros antigos do vendedor anterior quando necessário.
- O card do Mercado Livre em **Conexões** exibe dados reais da API, incluindo total de vendas, pedidos ativos, inventário e conta conectada.
- O backend já recebe webhooks do Mercado Livre e registra o payload recebido.
- Existe também sincronização automática periódica no backend e atualização periódica da tela de **Conexões** no frontend, sem precisar recarregar a página manualmente.
- A marca dos produtos sincronizados do Mercado Livre passa a ser preenchida a partir dos atributos do item quando disponível, evitando o uso fixo de `N/A`.
- Limitação operacional do ngrok free: o fluxo de callback pode passar pela página de aviso/interstitial do próprio ngrok antes de voltar ao app local.

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

Observações para a integração Mercado Livre:
- O frontend local continua em `http://localhost:5173`.
- O backend recebe o callback do Mercado Livre pela URL pública configurada em `mercadolivre.redirect.uri`.
- Depois do callback, o backend redireciona o navegador para `mercadolivre.frontend.redirect-uri`, cujo padrão é `http://localhost:5173/conexoes`.
- Se já existir outra instância ocupando a porta `8080`, o `spring-boot:run` falhará com erro de porta em uso.

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

## Configuração do Mercado Livre

As propriedades relevantes do backend ficam em `backend/Justock-Spring/justock-api/src/main/resources/application.properties`:

```properties
mercadolivre.client.id=SEU_APP_ID
mercadolivre.client.secret=SUA_SECRET_KEY
mercadolivre.redirect.uri=https://SEU-TUNEL.ngrok-free.dev/api/mercadolivre/callback
mercadolivre.frontend.redirect-uri=http://localhost:5173/conexoes
mercadolivre.shared.usuario-id=1
mercadolivre.auto-sync.enabled=true
mercadolivre.auto-sync.fixed-delay-ms=900000
mercadolivre.auto-sync.initial-delay-ms=120000
```

Notas importantes:
- `mercadolivre.redirect.uri` deve ser **idêntico** ao callback cadastrado no app do Mercado Livre.
- Se o app do Mercado Livre estiver com PKCE habilitado, o backend já envia `code_challenge` e usa `code_verifier` automaticamente.
- `mercadolivre.shared.usuario-id` define o usuário interno compartilhado do Justock usado para persistir a integração.
- `mercadolivre.auto-sync.fixed-delay-ms` controla o intervalo da sincronização automática do backend.

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
