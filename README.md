# 🚀 README

Aplicação **Spring Boot** desenvolvida com **Java 21 (Eclipse Temurin)**.

## Pré-requisitos
**Java 21**

Baixe e instale o Java 21. Se quiser orientação sobre qual distribuição escolher e como instalar, visite **[whichjdk.com](https://whichjdk.com/)**.

  > Este projeto foi desenvolvido e testado usando **Eclipse Temurin 21**.

**Docker** e **Docker Compose**

Necessários para construir a imagem e subir os serviços. Certifique-se de que o Docker esteja em execução.

**Maven**

Normalmente não é necessário instalar o maven manualmente, pois o projeto inclui o **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

Somente se o wrapper não estiver disponível no seu ambiente, instale o Maven (3.9+).

---

## Clonar o projeto

```bash
git clone git@github.com:FMABr/ducks_demo.git
cd ducks_demo
```

---

## Build da imagem Docker com Spring Boot

Use o **Maven Wrapper** a partir da raiz do projeto:

**Linux/macOS:**

```bash
./mvnw -DskipTests spring-boot:build-image -Dspring-boot.build-image.ducks_app=preco_justo/ducks:1.0.0
```

**Windows:**

```bat
mvnw.cmd -DskipTests spring-boot:build-image -Dspring-boot.build-image.ducks_app=preco_justo/ducks:1.0.0
```

---

## Subir a aplicação

Após a imagem estar disponível localmente:

```bash
docker compose -f full_compose.yaml up
```

* Para rodar em segundo plano:

  ```bash
  docker compose -f full_compose.yaml up -d
  ```
* Para encerrar:

  ```bash
  docker compose -f full_compose.yaml down
  ```

> As portas/variáveis de ambiente expostas são definidas no `full_compose.yaml`.

---

## Cronograma de desenvolvimento

### Configuração inicial e infraestrutura (2h)
- Iniciar projeto Spring Boot através do spring initializr.
- Iniciar uma instância do Postgres com Docker.
- Configurar `application.properties`.

### Modelagem de domínio e banco (2h)
- Definir entidades e relacionamentos.
- Criar constraints e indices.
- Migração inicial com Flyway.

### Entidades, repositórios e mapeamentos (2h)
- Implementar entidades JPA e repositórios.
- Adicionar anotações de validação.

### Endpoints de Patos (2h)
- CRUD.
- Endpoint de patos vendidos.
- Preço computado pela quantidade de filhos.

### CRUD de Clientes (1h)
- CRUD.

### CRUD de Funcionários (1h)
- CRUD.
- Impedir exclusão se possuir vendas.

### Criar e Ler Vendas (3h)
- Impedir revenda.
- Lógica de desconto por cliente.

### Rankings de Funcionários (2h)
- Ranking de funcionários por quantidade de vendas
- Ranking de funcionários por receita.

### Geração de relatório em excel (2h)
- Adicionar e configurar o Apache POI como dependência.
- Preparar consulta dos dados do relatório.
- Lógica de aninhar patos filhos abaixo da mãe. 

### Containerização (2h)
- Plugin do Spring Boot para geração da imagem.
- Compose-file para execução do app e banco de dados.

---

## Modelagem do banco
```mermaid
erDiagram
DUCK {
    BIGSERIAL id PK
    VARCHAR(255) name "NOT NULL"
    BIGINT mother_id "FK -> DUCK.id, NULL, ON DELETE SET NULL"
    TIMESTAMPTZ created_at "NOT NULL DEFAULT now()"
    TIMESTAMPTZ updated_at "NOT NULL DEFAULT now()"
    -- CHECK: mother_id IS NULL OR mother_id <> id
    -- INDEX: idx_duck_mother (mother_id)
}

CUSTOMER {
    BIGSERIAL id PK
    VARCHAR(255) name "NOT NULL"
    BOOLEAN has_sales_discount "NOT NULL DEFAULT FALSE"
    TIMESTAMPTZ created_at "NOT NULL DEFAULT now()"
    TIMESTAMPTZ updated_at "NOT NULL DEFAULT now()"
}

EMPLOYEE {
BIGSERIAL id PK
VARCHAR(255) name "NOT NULL"
VARCHAR(64)  cpf "UNIQUE NOT NULL"
VARCHAR(64)  employee_code "UNIQUE NOT NULL"
TIMESTAMPTZ created_at "NOT NULL DEFAULT now()"
TIMESTAMPTZ updated_at "NOT NULL DEFAULT now()"
}

SALE {
BIGSERIAL id PK
NUMERIC(12,2) total_before_discount "NOT NULL, CHECK >= 0"
NUMERIC(12,2) total_after_discount  "NOT NULL, CHECK >= 0"
BIGINT customer_id "FK -> CUSTOMER.id NOT NULL"
BIGINT employee_id "FK -> EMPLOYEE.id NOT NULL"
TIMESTAMPTZ sale_date  "NOT NULL DEFAULT now()"
TIMESTAMPTZ created_at "NOT NULL DEFAULT now()"
TIMESTAMPTZ updated_at "NOT NULL DEFAULT now()"
-- INDEX: (employee_id, sale_date)
-- INDEX: (customer_id, sale_date)
-- INDEX: (sale_date)
}

SALE_ITEM {
BIGSERIAL id PK
NUMERIC(12,2) price_at_sale "NOT NULL, CHECK >= 0"
BIGINT sale_id "FK -> SALE.id NOT NULL, ON DELETE CASCADE"
BIGINT duck_id "FK -> DUCK.id NOT NULL, UNIQUE"
}

V_SOLD_DUCK {
BIGINT duck_id
VARCHAR(255) duck_name
NUMERIC(12,2) price_at_sale
BIGINT sale_id
TIMESTAMPTZ sale_date
BIGINT customer_id
VARCHAR(255) customer_name
BIGINT employee_id
VARCHAR(255) employee_name
-- VIEW: JOIN sale_item -> duck, sale, customer, employee
}

%% Relationships
DUCK o|--o{ DUCK : "mother (0..1) → children (0..*)"
CUSTOMER ||--o{ SALE : "customer_id"
EMPLOYEE ||--o{ SALE : "employee_id"
SALE ||--|{ SALE_ITEM : "sale_id (1..*)"
DUCK ||--o| SALE_ITEM : "duck_id (sold at most once)"
SALE_ITEM ||--|| V_SOLD_DUCK : "1:1 row in view"

```