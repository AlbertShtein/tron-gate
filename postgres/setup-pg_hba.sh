#!/bin/bash
set -e

# Заменяем pg_hba.conf после инициализации БД
# Этот скрипт выполняется после создания БД, но PGDATA уже существует
if [ -f /tmp/pg_hba.conf ] && [ -n "$PGDATA" ] && [ -d "$PGDATA" ]; then
    cp /tmp/pg_hba.conf "$PGDATA/pg_hba.conf"
    chown postgres:postgres "$PGDATA/pg_hba.conf"
    chmod 600 "$PGDATA/pg_hba.conf"
    echo "pg_hba.conf updated to restrict external connections for postgres user"
else
    echo "Warning: Could not update pg_hba.conf (file or PGDATA not found)"
fi

