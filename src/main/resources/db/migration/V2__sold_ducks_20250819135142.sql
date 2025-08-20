CREATE VIEW v_sold_duck AS
SELECT
  d.id          AS duck_id,
  d.name        AS duck_name,
  si.price_at_sale,
  s.id          AS sale_id,
  s.sale_date,
  c.id          AS customer_id,
  c.name        AS customer_name,
  e.id          AS employee_id,
  e.name        AS employee_name
FROM sale_item si
JOIN duck d      ON d.id = si.duck_id
JOIN sale s      ON s.id = si.sale_id
JOIN customer c  ON c.id = s.customer_id
JOIN employee e  ON e.id = s.employee_id;