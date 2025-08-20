CREATE TABLE duck (
  id BIGSERIAL PRIMARY KEY,
  
  name  VARCHAR(255)  NOT NULL,

  mother_id BIGINT NULL,
  
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_duck_mother FOREIGN KEY (mother_id) REFERENCES duck(id) ON DELETE SET NULL,
  CONSTRAINT chk_duck_not_self_mother CHECK (mother_id IS NULL OR mother_id <> id)
);
CREATE INDEX idx_duck_mother ON duck(mother_id);


CREATE TABLE customer (
  id BIGSERIAL PRIMARY KEY,

  name               VARCHAR(255) NOT NULL,
  has_sales_discount BOOLEAN      NOT NULL DEFAULT FALSE,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE employee (
  id BIGSERIAL PRIMARY KEY,

  name          VARCHAR(255) NOT NULL,
  cpf           VARCHAR(64)  NOT NULL,
  employee_code VARCHAR(64)  NOT NULL,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_employee_cpf  UNIQUE (cpf),
  CONSTRAINT uq_employee_code UNIQUE (employee_code)
);


CREATE TABLE sale (
  id BIGSERIAL PRIMARY KEY,

  total_before_discount NUMERIC(12,2) NOT NULL CHECK (total_before_discount >= 0),
  total_after_discount  NUMERIC(12,2) NOT NULL CHECK (total_after_discount >= 0),

  customer_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,

  sale_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),


  CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE RESTRICT,
  CONSTRAINT fk_sale_employee FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE RESTRICT
);
CREATE INDEX idx_sale_employee_date ON sale(employee_id, sale_date);
CREATE INDEX idx_sale_customer_date ON sale(customer_id, sale_date);
CREATE INDEX idx_sale_date          ON sale(sale_date);


CREATE TABLE sale_item (
  id BIGSERIAL PRIMARY KEY,

  price_at_sale NUMERIC(12,2) NOT NULL CHECK (price_at_sale >= 0),
  sale_id       BIGINT        NOT NULL REFERENCES sale(id) ON DELETE CASCADE,
  duck_id       BIGINT        NOT NULL REFERENCES duck(id),

  CONSTRAINT uq_saleitem_duck UNIQUE (duck_id)
);
