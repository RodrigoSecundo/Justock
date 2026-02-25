CREATE TABLE cliente (
    id_usuario INTEGER PRIMARY KEY AUTO_INCREMENT,
    nome_usuario VARCHAR(255) NOT NULL,
    email_corporativo VARCHAR(255) NOT NULL UNIQUE,
    numero VARCHAR(255),
    senha VARCHAR(255) NOT NULL
);


INSERT INTO cliente (nome_usuario, email_corporativo, numero, senha) VALUES
('Admin Teste', 'admin@test.com', '123456789', '123456');
