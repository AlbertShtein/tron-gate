-- Создание базы данных trongate
CREATE DATABASE trongate;

-- Подключение к базе данных trongate
\c trongate

-- Создание таблицы wallet
CREATE TABLE wallet (
    id UUID NOT NULL PRIMARY KEY,
    private BYTEA,
    sign BYTEA
);

-- Создание пользователя trongate с правами read и insert
CREATE USER trongate WITH PASSWORD 'trongate_password';

-- Предоставление прав на подключение к базе данных
GRANT CONNECT ON DATABASE trongate TO trongate;

-- Предоставление прав на использование схемы public
GRANT USAGE ON SCHEMA public TO trongate;

-- Предоставление прав SELECT и INSERT на таблицу wallet
GRANT SELECT, INSERT ON TABLE wallet TO trongate;

-- Создание пользователя read с правами только на чтение
CREATE USER read WITH PASSWORD 'read_password';

-- Предоставление прав на подключение к базе данных
GRANT CONNECT ON DATABASE trongate TO read;

-- Предоставление прав на использование схемы public
GRANT USAGE ON SCHEMA public TO read;

-- Предоставление права только SELECT на таблицу wallet
GRANT SELECT ON TABLE wallet TO read;

