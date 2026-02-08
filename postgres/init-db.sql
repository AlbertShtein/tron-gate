-- Создание базы данных trongate
CREATE DATABASE trongate;

-- Подключение к базе данных trongate
\c trongate

-- Создание таблицы wallet
CREATE TABLE wallet (
    address BYTEA NOT NULL PRIMARY KEY,
    private BYTEA,
    sign BYTEA,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы block_cursor
CREATE TABLE block_cursor (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    block_number BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы wallet_event
CREATE TABLE wallet_event (
    id UUID NOT NULL PRIMARY KEY,
    wallet_address VARCHAR(42) NOT NULL,
    tx_hash VARCHAR(64) NOT NULL,
    block_number BIGINT NOT NULL,
    block_timestamp BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    direction VARCHAR(3) NOT NULL,
    from_address VARCHAR(42) NOT NULL,
    to_address VARCHAR(42) NOT NULL,
    amount NUMERIC(38,0) NOT NULL,
    token_address VARCHAR(42),
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_wallet_event_tx_wallet_dir UNIQUE (tx_hash, wallet_address, direction)
);

CREATE INDEX idx_wallet_event_wallet_address ON wallet_event(wallet_address);

-- Создание пользователя trongate с правами read и insert
CREATE USER trongate WITH PASSWORD 'trongate_password';

-- Предоставление прав на подключение к базе данных
GRANT CONNECT ON DATABASE trongate TO trongate;

-- Предоставление прав на использование схемы public
GRANT USAGE ON SCHEMA public TO trongate;

-- Предоставление прав SELECT, INSERT на таблицу wallet
GRANT SELECT, INSERT ON TABLE wallet TO trongate;

-- Предоставление прав на таблицы сканера
GRANT SELECT, INSERT, UPDATE ON TABLE block_cursor TO trongate;
GRANT SELECT, INSERT ON TABLE wallet_event TO trongate;

-- Создание пользователя read с правами только на чтение
CREATE USER read WITH PASSWORD 'read_password';

-- Предоставление прав на подключение к базе данных
GRANT CONNECT ON DATABASE trongate TO read;

-- Предоставление прав на использование схемы public
GRANT USAGE ON SCHEMA public TO read;

-- Предоставление права только SELECT на таблицы
GRANT SELECT ON TABLE wallet TO read;
GRANT SELECT ON TABLE block_cursor TO read;
GRANT SELECT ON TABLE wallet_event TO read;
