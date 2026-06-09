DO $$
DECLARE
    sequence_name text;
    next_value bigint;
BEGIN
    sequence_name := pg_get_serial_sequence('pedido', 'id_pedido');

    IF sequence_name IS NULL THEN
        RETURN;
    END IF;

    SELECT COALESCE(MAX(id_pedido), 0) + 1
      INTO next_value
      FROM pedido;

    EXECUTE format('SELECT setval(%L, %s, false)', sequence_name, next_value);
END $$;
