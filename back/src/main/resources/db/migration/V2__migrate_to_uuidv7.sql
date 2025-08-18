CREATE EXTENSION IF NOT EXISTS "plpgsql";

CREATE OR REPLACE FUNCTION uuid_generate_v7()
RETURNS uuid
AS $$
DECLARE
  unix_ts_ms bytea;
  uuid_bytes bytea;
  unix_ts_ms_val bigint;
BEGIN
  -- Obtener el timestamp en milisegundos desde la época Unix
  unix_ts_ms_val := floor(extract(epoch FROM now()) * 1000);

  -- Convertir el timestamp a bytes
  unix_ts_ms := set_byte('\x0000000000000000'::bytea, 0, (unix_ts_ms_val >> 40) & 255);
  unix_ts_ms := set_byte(unix_ts_ms, 1, (unix_ts_ms_val >> 32) & 255);
  unix_ts_ms := set_byte(unix_ts_ms, 2, (unix_ts_ms_val >> 24) & 255);
  unix_ts_ms := set_byte(unix_ts_ms, 3, (unix_ts_ms_val >> 16) & 255);
  unix_ts_ms := set_byte(unix_ts_ms, 4, (unix_ts_ms_val >> 8) & 255);
  unix_ts_ms := set_byte(unix_ts_ms, 5, unix_ts_ms_val & 255);

  -- Generar 10 bytes de datos aleatorios
  uuid_bytes := gen_random_bytes(10);

  -- Combinar los bytes para formar el UUID
  -- Los primeros 6 bytes son el timestamp
  -- Los bytes 6 y 7 (los 4 bits más significativos del byte 6) son la versión V7
  -- Los bytes 8 y 9 (los 2 bits más significativos del byte 8) son la variante
  uuid_bytes := set_byte(uuid_bytes, 0, (get_byte(unix_ts_ms, 0) & 255));
  uuid_bytes := set_byte(uuid_bytes, 1, (get_byte(unix_ts_ms, 1) & 255));
  uuid_bytes := set_byte(uuid_bytes, 2, (get_byte(unix_ts_ms, 2) & 255));
  uuid_bytes := set_byte(uuid_bytes, 3, (get_byte(unix_ts_ms, 3) & 255));
  uuid_bytes := set_byte(uuid_bytes, 4, (get_byte(unix_ts_ms, 4) & 255));
  uuid_bytes := set_byte(uuid_bytes, 5, (get_byte(unix_ts_ms, 5) & 255));
  uuid_bytes := set_byte(uuid_bytes, 6, (get_byte(uuid_bytes, 6) & '\x0f'::integer) | '\x70'::integer);
  uuid_bytes := set_byte(uuid_bytes, 8, (get_byte(uuid_bytes, 8) & '\x3f'::integer) | '\x80'::integer);

  -- Retornar el UUID
  RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- Primero eliminamos las restricciones de llave foránea
ALTER TABLE installments DROP CONSTRAINT IF EXISTS installments_loan_id_fkey;
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_customer_id_fkey;

-- Modificamos el tipo de las columnas id y las referencias a UUID
ALTER TABLE customers 
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE UUID USING uuid_generate_v7();

ALTER TABLE loans 
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE UUID USING uuid_generate_v7(),
    ALTER COLUMN customer_id TYPE UUID USING uuid_generate_v7();

ALTER TABLE installments 
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE UUID USING uuid_generate_v7(),
    ALTER COLUMN loan_id TYPE UUID USING uuid_generate_v7();

ALTER TABLE error_logs 
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE UUID USING uuid_generate_v7();

-- Restauramos las restricciones de llave foránea
ALTER TABLE loans 
    ADD CONSTRAINT loans_customer_id_fkey 
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;

ALTER TABLE installments 
    ADD CONSTRAINT installments_loan_id_fkey 
    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE;

-- Finalmente establecemos los nuevos valores por defecto
ALTER TABLE customers ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE loans ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE installments ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE error_logs ALTER COLUMN id SET DEFAULT uuid_generate_v7();
