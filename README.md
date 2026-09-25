# Сервис заявок библиотеки

Spring Boot 3.2.5 / Java 17. Предметная область - библиотека, основной объект - событие, роль - менеджер, обязательный сценарий - оформление заказа.

## Архитектура

В проекте намеренно используются **две системы хранения**, потому что данные имеют разные свойства:

- **PostgreSQL** - постоянные бизнес-данные: события и оформленные заказы. Для заказа важна реляционная целостность и транзакция изменения количества свободных мест + создания заказа.
- **Etcd** - KV/coordination-данные лабораторной работы:
  - временные заявки (draft) с lease;
  - настройки пользователя;
  - атомарный счётчик просмотров;
  - optimistic concurrency через revision;
  - watch для инвалидирования кэша.
- **Caffeine** - локальный кэш настроек пользователя.

Таким образом, Etcd не используется искусственно для данных, которым нужна реляционная модель.

## Запуск

Требуется Docker, Docker Compose, JDK 17 и Maven.

1. Запустить PostgreSQL и Etcd:

```bash
docker compose up -d
```

2. Проверить контейнеры:

```bash
docker compose ps
docker exec library-etcd etcdctl endpoint health
```

3. Запустить приложение:

```
mvn spring-boot:run
```

По умолчанию приложение использует:

- PostgreSQL: `jdbc:postgresql://localhost:5433/library`;
- Etcd: `http://localhost:2379`;
- HTTP: `http://localhost:8080`;

После запуска откройте `http://localhost:8080/`. Встроенный фронтенд менеджера
реализован на статических ресурсах Spring Boot (`src/main/resources/static`) и
использует REST API приложения. Он поддерживает вход по Basic Auth, просмотр
событий и счётчиков, оформление заказа, временные заявки с lease, отмену
заказов и редактирование пользовательских настроек.

Переменные окружения:

```text
ETCD_ENDPOINT
```

Параметры PostgreSQL задаются в `spring.datasource` файла
`src/main/resources/application.yml`; переменные `DB_URL`, `DB_USERNAME` и
`DB_PASSWORD` текущая конфигурация не подставляет автоматически.

## Модель данных Etcd

Используется иерархическое пространство имён:

| Ключ | Значение | Назначение |
|---|---|---|
| `/library/drafts/{draftId}` | JSON OrderDraft | временная заявка |
| `/library/idx/drafts-by-event/{eventId}/{draftId}` | пустое значение | индекс заявок события |
| `/library/users/{login}/settings` | JSON UserSettings | настройки пользователя |
| `/library/events/{eventId}/views` | число | счётчик просмотров |

JSON выбран для небольших самостоятельных документов: схема понятна Java-коду и не требует множества KV-ключей на поля объекта. Индексы вынесены в отдельные ключи, потому что Etcd предоставляет операции по диапазону ключей, а не реляционные JOIN.

## Lease

Временная заявка создаётся с Etcd lease. TTL задаётся:

```yaml
library:
  kv:
    draft-ttl-seconds: 300
```

При истечении lease Etcd автоматически удаляет связанные ключи. Продление выполняется через lease keep-alive.

## Кэш

`UserSettingsService` использует Caffeine поверх Etcd:

1. первый запрос читает настройки из Etcd;
2. последующие запросы берутся из локального кэша;
3. запись обновляет Etcd и кэш;
4. Etcd watch на `/library/users/` инвалидирует соответствующую запись кэша при внешнем изменении.

Статистика доступна через:

```
GET /api/settings/cache-stats
```

## Атомарный счётчик

Счётчик просмотров хранится в Etcd:

```
/library/events/{eventId}/views
```

Инкремент выполняется как compare-and-set по `modRevision`. Поэтому две параллельные записи не должны потерять обновление.
В пользовательском интерфейсе просмотр засчитывается при открытии формы оформления
заказа или формы временной заявки. Обновление списка событий само по себе просмотром
не считается.

В эксперименте доступны два варианта:

```
POST /api/experiments/counter?threads=20&perThread=100
```

Он сравнивает наивную последовательность read-write с CAS.

## Постоянные бизнес-данные

PostgreSQL содержит таблицы:

- `events`;
- `orders`.

Оформление заказа выполняется в транзакции PostgreSQL:

1. строка события блокируется `SELECT ... FOR UPDATE`;
2. проверяется число свободных мест;
3. уменьшается `free_seats`;
4. создаётся запись заказа;
5. транзакция фиксируется целиком.

Это позволяет исследовать параллельные записи без риска overbooking. Версия события хранится в поле `version` и используется для проверки изменений при REST-обновлении через `If-Match`.

## Обязательный сценарий «Оформление заказа»

```
POST /api/orders
```

Пример тела запроса:

```json
{
  "eventId": "ID_СОБЫТИЯ",
  "readerCard": "LIB-001",
  "readerName": "Иван Иванов",
  "seats": 2
}
```

Доступ к `/api/**` защищён HTTP Basic, роль - `MANAGER`.

Демо-пользователи текущего проекта:

```
ivanova / pass
petrov / pass
```

## Исследование конкуренции

Для счётчика:

```
POST /api/experiments/counter?threads=20&perThread=100
```

Сравниваются:

- naive read→write;
- CAS по revision.

Для заказов:

```
POST /api/experiments/orders?attempts=50&seats=20
```

Проверяются:

- число подтверждённых заказов;
- число отклонённых;
- остаток мест;
- наличие overbooking;
- время выполнения;
- число зафиксированных конфликтов.

## Persistence / Restore

Сохранение snapshot:

```bash
docker exec library-etcd etcdctl --endpoints=http://127.0.0.1:2379 snapshot save /tmp/etcd-snapshot.db
docker cp library-etcd:/tmp/etcd-snapshot.db ./data/etcd-snapshot.db
```

Для восстановления остановите приложение и восстановите snapshot штатной командой `etcdutl snapshot restore` в новый data-dir, затем запустите Etcd с этим каталогом.

## Основные API

- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events`
- `PUT /api/events/{id}`
- `POST /api/orders`
- `GET /api/orders`
- `POST /api/orders/{id}/cancel`
- `POST /api/drafts`
- `GET /api/drafts/{id}`
- `POST /api/drafts/{id}/extend`
- `DELETE /api/drafts/{id}`
- `GET/PUT/DELETE /api/settings/me`
- `GET /api/settings/cache-stats`
- `GET /api/admin/keys`
- `GET /api/admin/status`

## Что демонстрирует работа

1. Развёрнут отдельный Etcd.
2. Java/Spring Boot подключается к Etcd через Jetcd.
3. KV-модель обоснована и использует prefix/index.
4. В Etcd хранятся временные заявки, настройки и счётчик.
5. Lease используется для временных заявок.
6. Caffeine используется как кэш.
7. Счётчик просмотров обновляется атомарно.
8. PostgreSQL обеспечивает persistence бизнес-данных.
9. Параллельные записи исследуются через CAS и транзакционную блокировку PostgreSQL.
10. Сделан вывод о разделении ответственности: Etcd удобен для небольших KV-данных, TTL, watch и атомарных compare-and-set, а PostgreSQL — для долговечных связанных бизнес-данных.

## Структура проекта

```
Spring Boot
├── PostgreSQL
│   ├── events
│   └── orders
│
├── Etcd / Jetcd
│   ├── drafts + lease
│   ├── user settings
│   └── view counters + CAS
│
└── Caffeine
    └── user settings cache
```
