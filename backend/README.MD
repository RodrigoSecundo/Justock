# JusTock - Backend

Backend da aplicação JusTock, construído com Spring Boot, Spring Security, JPA/Hibernate, Flyway e PostgreSQL.

## Requisitos

1. JDK 17
    Disponível em: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

2. Maven 3.9.11 ou superior
    Disponível em: https://maven.apache.org/download.cgi

3. IDE de sua preferência
    No VS Code, recomenda-se instalar:
    - Java Extension Pack
    - Spring Boot Extension Pack

Sugestão:
Se quiser um guia detalhado de instalação dessas ferramentas no Windows, consulte:
https://www.treinaweb.com.br/blog/configurando-ambiente-de-desenvolvimento-spring-boot-no-windows

## Execução local

Na pasta `backend/Justock-Spring/justock-api`:

```powershell
./mvnw.cmd spring-boot:run
```

Backend padrão: `http://localhost:8080`

Para validar contexto e compilação:

```powershell
./mvnw.cmd test
```

## Banco de dados

- Banco atual: PostgreSQL / Supabase
- Migrations gerenciadas com Flyway
- O histórico atual do projeto vai até a versão `V8`

## Integração com Mercado Livre

### O que já está implementado

- Geração de URL de autorização OAuth do Mercado Livre
- Fluxo de callback público para troca do `code` por `access_token`
- Suporte a PKCE (`code_challenge` + `code_verifier`)
- Persistência de `access_token`, `refresh_token`, expiração e dados da loja em `cliente_marketplace`
- Renovação automática de token com `refresh_token` quando necessário
- Endpoint para status da conexão
- Endpoint para desconectar a conta integrada
- Sincronização manual de produtos e pedidos via API do Mercado Livre
- Upsert de produtos e pedidos por `marketplace_resource_id`
- Limpeza de registros antigos do ML ao trocar a conta vendedora conectada
- Resumo real da conta conectada para a tela de Conexões, com total de vendas, pedidos ativos, inventário e `sellerId`
- Sincronização automática periódica no backend por agendamento (`@Scheduled`)
- Endpoint público de webhook para receber notificações do Mercado Livre
- Registro do payload bruto do webhook em `eventos_webhook`
- Extração da marca do produto a partir dos atributos do item retornados pelo Mercado Livre quando disponível

### O que ainda não está implementado

- Processamento assíncrono dos webhooks recebidos
- Sincronização de itens de pedido em `estoque_pedido`

### Endpoints atuais do Mercado Livre

- `GET /api/mercadolivre/auth-url`
- `GET /api/mercadolivre/status`
- `GET /api/mercadolivre/callback`
- `POST /api/mercadolivre/sync`
- `POST /api/mercadolivre/disconnect`
- `POST /api/mercadolivre/webhook`

### Configuração esperada no `application.properties`

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

### Observações práticas sobre o callback

- `mercadolivre.redirect.uri` precisa bater exatamente com a URL cadastrada no app do Mercado Livre.
- O frontend do projeto continua local em `http://localhost:5173`.
- O backend recebe o callback pelo túnel HTTP público e depois redireciona o navegador para a tela local de conexões.
- Em ngrok free, o navegador pode exibir a página de aviso/interstitial antes de seguir para o callback.

### Observações práticas sobre a sincronização

- O endpoint `POST /api/mercadolivre/sync` sincroniza produtos e pedidos em uma única operação.
- A sincronização automática usa as propriedades `mercadolivre.auto-sync.*` e só roda quando a integração está conectada.
- A tela de Conexões do frontend consulta esse resumo periodicamente para refletir mudanças sem exigir reload manual.
- Pedidos sincronizados do Mercado Livre são persistidos com origem do marketplace e podem ser diferenciados dos pedidos manuais.

### Modelo atual de uso

O projeto está configurado hoje para trabalhar com um **usuário interno compartilhado** do Justock, definido pela propriedade:

```properties
mercadolivre.shared.usuario-id=1
```

Isso significa:
- a conta interna do Justock pode continuar sendo a padrão para a equipe;
- a conta vendedora do Mercado Livre conectada pode ser trocada;
- uma nova conexão substitui a credencial anterior da integração compartilhada.

