# Диаграммы проектирования

## Варианты использования

```mermaid
flowchart LR
    User([Пользователь]) --> Register([Зарегистрироваться])
    User --> Login([Войти])
    User --> Browse([Просмотреть задачи])
    User --> Create([Создать задачу])
    User --> Edit([Изменить задачу])
    User --> Complete([Завершить задачу])
    User --> Delete([Удалить задачу])
    User --> Search([Найти задачу])
    User --> Theme([Переключить тему])
    User --> Logout([Выйти])
    Search --> History([Просмотреть и очистить историю])
```

Операции с задачами и поиском доступны после входа. В каждом серверном
варианте использования проверяется токен и принадлежность данных
пользователю.

## Логическая модель БД

```mermaid
erDiagram
    APP_USERS ||--o{ TASKS : owns
    APP_USERS {
        varchar firebase_uid PK
        timestamptz created_at
    }
    TASKS {
        uuid id PK
        varchar owner_uid FK
        varchar title
        varchar description
        timestamptz due_at
        varchar priority
        varchar status
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
    }
```

История поиска — отдельная локальная таблица Room с полями `ownerUid`,
`query`, `selectedAt`; она не синхронизируется с PostgreSQL.

## Классы и зависимости

```mermaid
classDiagram
    class Task {
        +UUID id
        +String title
        +String description
        +Instant? dueAt
        +Priority priority
        +TaskStatus status
    }
    class TaskRepository {
        <<interface>>
        +getTasks(filter) TaskPage
        +getTask(id) Task
        +createTask(input) Task
        +updateTask(id,input) Task
        +deleteTask(id)
    }
    class TaskUseCases
    class TaskListViewModel
    class TaskEditorViewModel
    class RemoteTaskRepository
    class TaskApi
    class SearchHistoryRepository
    class TaskService
    class TaskDao

    TaskListViewModel --> TaskUseCases
    TaskEditorViewModel --> TaskUseCases
    TaskUseCases --> TaskRepository
    RemoteTaskRepository ..|> TaskRepository
    RemoteTaskRepository --> TaskApi
    TaskService --> TaskDao
    TaskDao --> Task
    TaskListViewModel --> SearchHistoryRepository
```

Схема обобщает логические классы и зависимости компонентов; имена
обозначают роли в архитектуре, а не точные названия типов в исходниках.

## Функциональная модель A0 для IDEF0

Функция A0 — «Управлять личными задачами пользователя».

| Сторона блока | Потоки |
|---|---|
| Входы | Действия пользователя, данные задачи, поисковый запрос |
| Управление | Правила валидации, ограничения доступа, требования курсовой |
| Выходы | Список и детали задач, результат поиска, сообщения об ошибках |
| Механизмы | Android/Compose, Firebase Authentication, Ktor, PostgreSQL |

```mermaid
flowchart LR
    INPUT[Действия и данные пользователя] --> A0["A0 Управлять личными задачами"]
    CONTROL[Правила и требования] --> A0
    A0 --> OUTPUT[Задачи, результаты и ошибки]
    MECH[Android · Firebase Auth · Ktor · PostgreSQL] --> A0
```

Декомпозиция A0: A1 «Подтвердить пользователя», A2 «Обработать задачу»,
A3 «Найти задачи», A4 «Отобразить результат». Приведённая схема
показывает потоки функции A0; отдельный чертёж IDEF0 в состав
документации проекта не входит.
