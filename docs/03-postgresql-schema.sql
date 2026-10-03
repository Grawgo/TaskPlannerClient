-- Логическая схема для PostgreSQL 13+.
-- Учётные данные обрабатывает Firebase Authentication; в этой БД хранится UID.

CREATE TABLE app_users (
    firebase_uid VARCHAR(128) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_uid VARCHAR(128) NOT NULL
        REFERENCES app_users(firebase_uid) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL
        CHECK (length(btrim(title)) BETWEEN 1 AND 120),
    description VARCHAR(2000) NOT NULL DEFAULT '',
    due_at TIMESTAMPTZ,
    priority VARCHAR(6) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status VARCHAR(11) NOT NULL DEFAULT 'TODO'
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Частые запросы: список пользователя, фильтр по статусу и сроку.
CREATE INDEX tasks_owner_status_due_idx
    ON tasks (owner_uid, status, due_at, id);

-- Сортировка по последнему изменению и стабильная пагинация.
CREATE INDEX tasks_owner_updated_idx
    ON tasks (owner_uid, updated_at DESC, id DESC);

-- Поиск выполняется по title/description с параметризованным ILIKE.
-- Отдельный полнотекстовый индекс не используется.
