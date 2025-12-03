FROM postgres:18-alpine

# PostgreSQL уже работает от пользователя postgres (rootless)
# Копируем скрипт инициализации
COPY init-db.sql /docker-entrypoint-initdb.d/

# Копируем кастомный pg_hba.conf для ограничения доступа postgres извне
COPY pg_hba.conf /tmp/pg_hba.conf

# Устанавливаем права на скрипты для пользователя postgres
RUN chown postgres:postgres /docker-entrypoint-initdb.d/init-db.sql && \
    chmod 644 /docker-entrypoint-initdb.d/init-db.sql && \
    chown postgres:postgres /tmp/pg_hba.conf && \
    chmod 644 /tmp/pg_hba.conf

# Скрипт для замены pg_hba.conf после инициализации
COPY setup-pg_hba.sh /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/setup-pg_hba.sh

