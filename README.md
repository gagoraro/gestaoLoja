# Gestão Loja — A Vitoriosa de Angra

Sistema de gestão para loja com **login**, **caixa do dia** (abrir, registrar vendas, fechar com conferência), **cadastro de produtos**, **controle de estoque** (entradas/saídas com histórico) e **orçamentos com PDF e logo da empresa**.

| Camada    | Tecnologia                                                        |
|-----------|-------------------------------------------------------------------|
| Frontend  | React 18 + TypeScript + Vite + React Router                       |
| Backend   | Java 21 + Spring Boot 3.3 (Web, Data JPA, Validation, Security) + OpenPDF |
| Login     | Supabase Auth (front) + validação do JWT no Spring (chave pública) |
| Banco     | Supabase (PostgreSQL) — migrations versionadas com Flyway         |

---

## Arquitetura

```mermaid
flowchart LR
    U[Navegador] -->|SPA| FE[React + Vite]
    FE -->|REST JSON /api/*| BE[Spring Boot API]
    BE -->|JDBC + Flyway| DB[(Supabase Postgres<br/>schema "loja")]
    BE -->|application/pdf| FE
```

**Decisões principais**

1. **Supabase como Postgres gerenciado, acessado só pelo backend.** O React nunca fala direto com o Supabase; toda regra de negócio fica no Java. As tabelas ficam no schema **`loja`** (não no `public`), então **não são expostas pela Data API/PostgREST** do Supabase — não há risco de alguém ler/escrever com a `anon key`.
2. **Flyway é dono do schema** (`hibernate.ddl-auto=validate`). Ao subir, o backend cria/atualiza as tabelas no Supabase automaticamente. Mudanças futuras = novo arquivo `V2__...sql`.
3. **Estoque com trava pessimista.** Cada movimentação faz `SELECT ... FOR UPDATE` no produto, valida saldo (nunca negativo — também garantido por `CHECK` no banco) e grava o registro em `movimentacao_estoque` com o saldo resultante. Assim duas operações simultâneas não se sobrescrevem e todo saldo é auditável.
4. **Orçamento é calculado no servidor.** O front envia apenas `produtoId` + `quantidade`; preço e total vêm do cadastro. Os itens guardam **snapshot** (nome e preço da época), então editar um produto depois não altera orçamentos antigos.
5. **Orçamento não baixa estoque** (é proposta, não venda). O front só avisa quando a quantidade passa do saldo.
6. **PDF gerado no backend** (OpenPDF): layout idêntico em qualquer navegador e pronto para ser enviado por e-mail/WhatsApp no futuro.
7. **Exclusão lógica de produtos** (`ativo=false`) para preservar histórico de estoque e orçamentos.
8. **Login com Supabase Auth.** O front faz o login direto no Supabase e manda o token em cada chamada. O Spring valida assinatura (chave pública ES256 do projeto, via JWKS), emissor, audiência e validade — o backend não guarda nenhum segredo de autenticação.
9. **Caixa do dia.** Só existe um caixa aberto por vez (garantido por `UNIQUE` no banco). Cada venda baixa o estoque na mesma transação — se faltar estoque de qualquer item, nada é gravado. Cancelar uma venda devolve os itens ao estoque. No fechamento, o sistema compara o dinheiro contado com o esperado (troco inicial + vendas em dinheiro).
10. **Organização por feature** no backend (`produto/`, `estoque/`, `orcamento/`), cada uma com entidade → repository → service → controller → DTOs.

### Estrutura

```
gestaoLoja/
├── backend/                          Spring Boot
│   ├── pom.xml  mvnw
│   └── src/main/
│       ├── java/br/com/gestaoloja/
│       │   ├── produto/              CRUD de produtos
│       │   ├── estoque/              movimentações (entrada/saída) + histórico
│       │   ├── caixa/                caixa do dia + vendas
│       │   ├── orcamento/            orçamentos + OrcamentoPdfService
│       │   ├── common/               exceções + handler de erros (RFC 7807)
│       │   └── config/               segurança (JWT), CORS, propriedades
│       └── resources/
│           ├── application.yml       perfis "dev" (H2) e "supabase"
│           ├── logo-empresa.png      logo usado no PDF
│           └── db/migration/         V1 (produtos/orçamentos), V2 (caixa/vendas)
└── frontend/                         React + Vite
    └── src/
        ├── auth/                     cliente Supabase + contexto de login
        ├── api/                      client fetch (envia o token), tipos, formatação BRL
        ├── components/               Layout (menu hambúrguer), ItensEditor, Modal, ProdutoForm, EstoqueModal
        └── pages/                    Login, Caixa, Produtos, Estoque, Orçamentos, NovoOrçamento
```

### Modelo de dados

```mermaid
erDiagram
    produto ||--o{ movimentacao_estoque : tem
    produto ||--o{ orcamento_item : "referenciado em"
    orcamento ||--|{ orcamento_item : contem
    produto { bigint id PK
              varchar sku UK
              varchar nome
              numeric preco_unitario
              int quantidade_estoque
              bool ativo }
    movimentacao_estoque { bigint id PK
              bigint produto_id FK
              varchar tipo "ENTRADA|SAIDA"
              int quantidade
              int saldo_apos
              varchar motivo }
    orcamento { bigint id PK
              varchar cliente_nome
              numeric valor_total
              date validade }
    orcamento_item { bigint id PK
              bigint orcamento_id FK
              bigint produto_id FK
              varchar produto_nome "snapshot"
              numeric preco_unitario "snapshot"
              int quantidade
              numeric subtotal }
```

### API REST

| Método | Rota                                   | Descrição                                   |
|--------|----------------------------------------|---------------------------------------------|
| GET    | `/api/produtos?busca=`                 | Lista produtos ativos (busca por nome/SKU)  |
| GET    | `/api/produtos/{id}`                   | Detalhe                                     |
| POST   | `/api/produtos`                        | Cria (`estoqueInicial` opcional)            |
| PUT    | `/api/produtos/{id}`                   | Atualiza dados (não altera estoque)         |
| DELETE | `/api/produtos/{id}`                   | Exclusão lógica                             |
| POST   | `/api/produtos/{id}/movimentacoes`     | `{tipo: ENTRADA\|SAIDA, quantidade, motivo}`|
| GET    | `/api/produtos/{id}/movimentacoes`     | Últimas 50 movimentações                    |
| GET    | `/api/orcamentos`                      | Lista orçamentos                            |
| GET    | `/api/orcamentos/{id}`                 | Detalhe com itens                           |
| POST   | `/api/orcamentos`                      | `{clienteNome, clienteContato?, observacoes?, itens:[{produtoId, quantidade}]}` |
| DELETE | `/api/orcamentos/{id}`                 | Exclui                                      |
| GET    | `/api/orcamentos/{id}/pdf`             | Baixa o PDF (com logo)                      |
| GET    | `/api/caixa/atual`                     | Caixa aberto com vendas e totais (204 se fechado) |
| POST   | `/api/caixa/abrir`                     | `{valorAbertura}` — troco inicial           |
| POST   | `/api/caixa/vendas`                    | `{formaPagamento, itens:[{produtoId, quantidade}], observacao?}` — baixa estoque |
| POST   | `/api/caixa/vendas/{id}/cancelar`      | Cancela e devolve ao estoque                |
| POST   | `/api/caixa/{id}/fechar`               | `{valorContado?, observacao?}`              |
| GET    | `/api/caixa` · `/api/caixa/{id}`       | Histórico de caixas · detalhe               |

Formas de pagamento: `DINHEIRO`, `PIX`, `CARTAO_DEBITO`, `CARTAO_CREDITO`. Todas as rotas `/api/**` exigem login (`Authorization: Bearer <token>`).

Erros seguem RFC 7807 (`{status, detail, campos?}`): 400 validação, 404 não encontrado, 422 regra de negócio (ex.: estoque insuficiente, SKU duplicado), 409 integridade.

---

## Como rodar

Pré-requisitos: **JDK 21** e **Node 20+**. (Maven não é necessário — use o `./mvnw`.)

### 1. Backend — modo dev (H2 em memória, sem Supabase)

```bash
cd backend && ./mvnw spring-boot:run
```

API em `http://localhost:8080`. Os dados somem ao reiniciar.

### 2. Backend — com Supabase

1. Crie um projeto em [supabase.com](https://supabase.com).
2. Copie `backend/.env.example` para `backend/.env` (este arquivo é ignorado pelo git).
3. No Supabase, clique em **Connect → Session pooler** e preencha no `.env` o host, o usuário (`postgres.<id-do-projeto>`) e a senha do banco.
4. Rode normalmente — o backend lê o `.env` sozinho e ativa o perfil `supabase`:

```bash
cd backend && ./mvnw spring-boot:run
```

Sem o `backend/.env`, o backend volta ao modo dev (H2). Em deploy, use variáveis de ambiente em vez do arquivo. Os testes automáticos sempre usam H2, mesmo com `.env` presente.

O Flyway cria o schema `loja` e as tabelas na primeira execução. Use o **Session pooler (5432)** ou conexão direta — o *Transaction pooler (6543)* não é compatível com prepared statements do JDBC sem ajustes.

### 3. Login (Supabase Auth)

1. **Supabase → Authentication → Sign In / Providers**: desligue **"Allow new users to sign up"**. Isso é importante: a chave pública do front permite cadastro aberto se essa opção ficar ligada.
2. **Authentication → Users → Add user → Create new user**: crie o e-mail e a senha de cada pessoa que vai usar o sistema (marque "Auto Confirm User").
3. Copie `frontend/.env.example` para `frontend/.env` e preencha `VITE_SUPABASE_URL` e `VITE_SUPABASE_ANON_KEY` (Project Settings → API Keys → chave **anon/publishable**, nunca a `service_role`/secret).
4. No `backend/.env`, `SUPABASE_URL` deve ser a URL do mesmo projeto.

Sem `VITE_SUPABASE_ANON_KEY`, o front roda em **modo local sem login** (só faz sentido com o backend no perfil `dev`).

### 4. Frontend

```bash
cd frontend && npm install && npm run dev
```

Abra `http://localhost:5173`. Em dev, o Vite faz proxy de `/api` para `http://localhost:8080` (mude com `BACKEND_URL=...`). Em produção, defina `VITE_API_URL` no build e `CORS_ALLOWED_ORIGINS` no backend.

### Testes

```bash
cd backend && ./mvnw test
```

Os testes cobrem: cadastro e busca, SKU duplicado, entrada/saída, bloqueio de saldo negativo, orçamento e PDF, exclusão lógica, caixa completo (abrir, vender, estoque insuficiente, cancelar, fechar com diferença) e exigência de login (401 sem token).

---

## Próximos passos sugeridos

- Perfis de usuário (dono × vendedor) e permissões por tela.
- **Converter orçamento em venda** no caixa.
- Relatórios de vendas por período e produto mais vendido.
- Desconto por item/orçamento, dados da loja (CNPJ, logo) no PDF, envio por e-mail/WhatsApp.
- Paginação nas listagens, alerta de estoque mínimo, relatórios.
- Deploy: backend em Render/Railway/Fly (Dockerfile), frontend em Vercel.
