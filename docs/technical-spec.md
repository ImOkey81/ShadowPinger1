# AgentGateway ↔ Agent (Mobile App) — Техническое задание

Версия: 2.0

Документ описывает контракт AgentGateway ↔ Mobile Agent, транспорт и протокол сообщений, а также план разработки мобильного агента ShadowPinger (Android).

## Глава 4 — Контракты AgentGW ↔ Agent

Протокол поверх WebSocket для мобильного агента: аутентификация, heartbeat, доставка задач, подтверждения (ack), batching результатов, отмена, ретраи и поведение при разрывах соединения.

Транспорт может быть заменён на gRPC-stream — смысл протокола остаётся тем же.

### Содержание

* Общие правила
* Подключение и аутентификация
* Heartbeat
* Доставка задач
* Статусы задачи
* Результаты (batching)
* Отмена
* Retry и разрывы
* Keepalive ping/pong
* Диаграмма

Примечание: описание TaskSpec см. Глава 1 → Контракт TaskAssign.

### 0. Общие правила

Почему ack на уровне приложения:

* WebSocket гарантирует доставку в сокет, но не гарантирует обработку сообщения агентом и не обеспечивает exactly-once.

Базовый формат сообщений:

```json
{
  "schema_version": 1,
  "type": "string",
  "msg_id": "uuid",
  "sent_at": "ISO8601 UTC",
  "...": "payload"
}
```

* `msg_id` — идентификатор протокольного сообщения. При ретраях `msg_id` не меняется.

Транспорт: WebSocket. Формат сообщений: JSON.

### 1. Подключение и аутентификация

URL подключения:

```
wss://agent-gateway.example.com/ws/v1
```

#### 1.1 agent.hello (Agent → GW)

```json
{
  "schema_version": 1,
  "type": "agent.hello",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:00:00Z",
  "auth": {
    "device_id": "uuid",
    "device_token": "string"
  },
  "agent": {
    "app_version": "1.0.3",
    "platform": "android",
    "model": "Pixel 6",
    "capabilities": {
      "icmp": true
    }
  }
}
```

#### 1.2 gw.hello_ack (GW → Agent)

```json
{
  "schema_version": 1,
  "type": "gw.hello_ack",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:00:00Z",
  "session": {
    "session_id": "uuid",
    "heartbeat_interval_sec": 15,
    "max_inflight_tasks": 1,
    "max_payload_bytes": 200000
  }
}
```

#### 1.3 gw.error (GW → Agent)

```json
{
  "schema_version": 1,
  "type": "gw.error",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:00:00Z",
  "code": "unauthorized",
  "message": "invalid token"
}
```

### 2. Heartbeat

Агент отправляет heartbeat каждые `heartbeat_interval_sec`.

```json
{
  "schema_version": 1,
  "type": "agent.heartbeat",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:00:15Z",
  "device_id": "uuid",
  "status": {
    "battery_pct": 67,
    "online": true
  },
  "sims": [
    { "slot": 1, "operator_id": "25001", "active": true },
    { "slot": 2, "operator_id": "25002", "active": false }
  ]
}
```

Подтверждение от GW:

```json
{
  "schema_version": 1,
  "type": "gw.ack",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:00:15Z",
  "ack": { "msg_id": "uuid" }
}
```

### 3. Доставка задач

#### 3.1 gw.task_assign (GW → Agent)

Payload `task` — это TaskSpec из Главы 1.

```json
{
  "schema_version": 1,
  "type": "gw.task_assign",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:01:00Z",
  "task": {
    "task_id": "uuid",
    "kind": "campaign_subnet_probe",
    "campaign_id": "uuid",
    "adhoc_job_id": null,
    "target_cidr": "203.0.113.0/24",
    "method": "icmp",
    "seed": 123456789,
    "sampling": { "...": "см. Глава 1" },
    "chunking": { "...": "см. Глава 1" },
    "sim_policy": { "...": "см. Глава 1" },
    "result_policy": { "...": "см. Глава 1" }
  }
}
```

#### 3.2 agent.task_ack (Agent → GW)

```json
{
  "schema_version": 1,
  "type": "agent.task_ack",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:01:01Z",
  "task_id": "uuid",
  "ack": {
    "status": "received"
  }
}
```

### 4. Статусы задачи

```json
{
  "schema_version": 1,
  "type": "agent.task_status",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:01:05Z",
  "task_id": "uuid",
  "device_id": "uuid",
  "operator_id": "25001",
  "status": "started",
  "details": {
    "message": "started",
    "error_code": null
  }
}
```

Статусы MVP: `received`, `started`, `progress`, `done`, `failed`, `canceled`.

### 5. Результаты (batching)

#### 5.1 agent.measurements_batch

```json
{
  "schema_version": 1,
  "type": "agent.measurements_batch",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:01:20Z",
  "task": {
    "task_id": "uuid",
    "campaign_id": "uuid",
    "adhoc_job_id": null,
    "device_id": "uuid",
    "operator_id": "25001",
    "target": "203.0.113.0/24"
  },
  "batch": { "seq": 1, "is_final": false },
  "summary": {
    "sampled": 64,
    "replied": 7,
    "loss_rate": 0.890625,
    "rtt_ms_min": 42,
    "rtt_ms_avg": 88,
    "rtt_ms_p95": 140,
    "duration_ms": 18000
  },
  "points": [
    { "ip": "203.0.113.10", "ok": true, "rtt_ms": 52, "attempts": 1 },
    { "ip": "203.0.113.77", "ok": false, "attempts": 2, "fail_reason": "timeout" }
  ]
}
```

#### 5.2 gw.ack

```json
{
  "schema_version": 1,
  "type": "gw.ack",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:01:20Z",
  "ack": { "msg_id": "uuid" }
}
```

### 6. Отмена

```json
{
  "schema_version": 1,
  "type": "gw.task_cancel",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:02:00Z",
  "task_id": "uuid",
  "reason": "campaign_stopped"
}
```

### 7. Retry и разрывы

#### 7.1 Inflight

* `max_inflight_tasks` (MVP = 1).
* Новая задача не выдаётся, пока текущая не завершена.

#### 7.2 Retry на стороне Agent

* Локальная очередь неподтверждённых сообщений.
* Retry с тем же `msg_id`.
* GW идемпотентен по `msg_id`.

#### 7.3 Reconnect + resume

```json
{
  "schema_version": 1,
  "type": "agent.resume",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:03:00Z",
  "device_id": "uuid",
  "in_progress": [
    {
      "task_id": "uuid",
      "last_batch_seq_sent": 3,
      "is_final_sent": false
    }
  ]
}
```

#### 7.4 Exactly-once не гарантируется

Обеспечивается at-least-once + идемпотентность.

### 8. Keepalive ping/pong (опционально)

```json
{
  "schema_version": 1,
  "type": "gw.ping",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:00:30Z"
}
```

```json
{
  "schema_version": 1,
  "type": "agent.pong",
  "msg_id": "uuid",
  "sent_at": "2026-01-28T12:00:30Z",
  "ack": { "msg_id": "uuid" }
}
```

### 9. Диаграмма

Изображение:

```
assets/agentgw-agent-seq.png
```

## План разработки мобильного агента ShadowPinger (Android)

Стек: Kotlin • Hilt • Jetpack Compose • MVVM • Coroutines/Flow • OkHttp • kotlinx.serialization

Формат: 1-недельные спринты • 1–2 разработчика (если больше — задачи легко параллелятся)

### Общие правила проекта (фиксируем до старта)

* Ветвление: trunk-based (main + короткие feature branches).
* Каждый PR: unit tests + скрин логов с реального девайса (для сетевых функций).
* Логи: внутри приложения есть debug-экран с последними 500 строками (иначе вы утонете).
* Definition of Done (для любой фичи):
  * работает на реальном устройстве
  * переживает сворачивание/возврат
  * не падает при отказе в permissions
  * корректно деградирует при отсутствии сети/симки

### Архитектурный каркас (модули/пакеты)

* `transport/` — WebSocket + protocol engine (ack/outbox/dedup/reconnect)
* `agent/` — runner/state machine, планирование, выполнение
* `reachability/` — ping/probe методы + fallback
* `data/` — DataStore/Encrypted prefs, HTTP client, репозитории
* `ui/` — Compose screens + ViewModels (MVVM)
* `core/` (опц) — утилиты: IPv4/CIDR, time, serialization helpers

Критерий: DI-граф без циклов, чистые интерфейсы между слоями.

### Sprint 0 — Bootstrap + «скелет агента»

Цель: проект собирается, есть каркас, можно руками тестировать базовую оболочку.

Задачи:

1. Создать проект + зависимости
   * Compose, Hilt, Coroutines/Flow, kotlinx.serialization, OkHttp
   * Результат: приложение стартует, есть экран Status
   * Приёмка: `./gradlew assembleDebug` и запуск на девайсе

2. Каркас модулей/пакетов
   * пустые интерфейсы, базовые модели, DI-модули
   * Приёмка: сборка без циклов в DI, минимальная навигация в UI

3. DataStore (Preferences/Proto)
   * хранение `device_id`, `device_token`, base config
   * Приёмка: после перезапуска значения сохраняются

4. Debug UI: Status + Logs
   * кольцевой буфер на 500 строк
   * копирование логов в clipboard
   * Приёмка: логи видны без adb, копируются одной кнопкой

Зависимости: нет.

### Sprint 1 — WebSocket транспорт + протокол

Цель: стабильное соединение, базовые сообщения, ack/retry, reconnect.

Задачи:

1. WebSocketClient (OkHttp)
   * connect/disconnect
   * callbacks → Flow (inbound/outbound states)
   * Приёмка: подключается к тестовому endpoint, принимает текстовые сообщения

2. ProtocolEngine
   * hello / hello_ack
   * heartbeat scheduler
   * reconnect с exponential backoff
   * inbound dedup по msg_id (LRU cache 1–5k)
   * outbound outbox + resend по таймеру + обработка `gw.ack`
   * Приёмка: выключили сеть → reconnect → heartbeat снова идут стабильно

3. Модели сообщений (kotlinx.serialization)
   * `gw.task_assign`, `gw.task_cancel`
   * `agent.task_status`
   * `agent.measurements_batch` (пока мок)
   * Приёмка: JSON сериализуется/десериализуется, везде есть `schema_version`

4. Мини-ручной тест в UI
   * Connect / Disconnect / SendTestMessage
   * Приёмка: джун сам может проверить транспорт без внешних тулов

Зависимости: Sprint 0.

### Sprint 2 — Device identity + Foreground runtime

Цель: агент живёт в фоне, безопасно хранит креды, формирует heartbeat payload.

Задачи:

1. Foreground Service “Agent running” + notification
   * Приёмка: агент живёт в фоне 30+ минут, соединение не рвётся из-за убийства процесса

2. Permissions + UX
   * POST_NOTIFICATIONS (Android 13+) только для UX
   * FGS уведомление должно быть всегда (без крашей при deny)
   * Приёмка: при запрете уведомлений агент не падает, показывает warning

3. Хранение device_token
   * EncryptedSharedPreferences / Keystore wrapper
   * Приёмка: токен не лежит в plain preferences

4. Heartbeat payload
   * `device_id`, `app_version`, capabilities (icmp)
   * Приёмка: сервер видит реальную capability матрицу

Зависимости: Sprint 1.

### Sprint 3 — SIM enumeration + cellular binding

Цель: трафик идёт через SIM даже при включенном Wi-Fi, корректный контекст операторов.

Задачи:

1. SimManager
   * SubscriptionManager/TelephonyManager
   * модель `OperatorSim(slot, operatorId(MCCMNC), subId)`
   * Приёмка: на девайсе с 2 SIM отображаются оба оператора + active корректно

2. CellularNetworkBinder
   * requestNetwork(TRANSPORT_CELLULAR + NET_CAPABILITY_INTERNET)
   * bindProcessToNetwork(network) на время выполнения + rollback
   * лог деталей сети
   * Приёмка: при включённом Wi-Fi TCP connect выполняется через cellular (поведение подтверждено логами/маршрутом)

3. Error mapping
   * если cellular недоступна → NO_ROUTE
   * Приёмка: runner не стартует «впустую», статус/ошибка уходят наверх

Зависимости: Sprint 2.

### Sprint 4 — TargetsProvider + курсор кампании

Цель: агент получает список подсетей с сервера, кэширует и продолжает после рестарта.

Задачи:

1. HTTP client для Agent targets
   * GET `/agent/v1/campaigns/{id}/targets?page&size`
   * Приёмка: загрузка минимум 3 страниц подряд

2. TargetsProvider
   * in-memory cache + (опц) disk cache
   * курсор (page/index) в DataStore
   * контроль targets_hash: если изменился → fail кампании `TARGETS_CHANGED`
   * Приёмка: убили приложение → снова запустили → продолжает с сохранённого cursor

Зависимости: Sprint 2 (креды), Sprint 3 (SIM — опционально).

### Sprint 5 — Reachability v2 (ping + fallback цепочка)

Цель: есть единая функция `probe(ip) → (ok/rtt/error_class)`, стабильно работает на реальных девайсах.

Задачи:

1. ICMP_DGRAM Prober (icmp4a предпочтительно)
   * `ProbeAttemptResult(ok, rtt, error_class)`
   * Приёмка: на реальном устройстве 8.8.8.8 отвечает ICMP_DGRAM (если сеть позволяет), RTT есть

2. ICMP_EXEC Prober
   * `/system/bin/ping -c 1 -W <sec>`
   * robust парсинг stdout + exit code
   * Приёмка: если DGRAM недоступен, exec даёт результат

3. TCP_CONNECT Prober
   * порты из TaskSpec (например 443/80/22)
   * измеряем connect RTT
   * Приёмка: ok=true при успешном connect, иначе TIMEOUT/UNREACHABLE

4. ProbingOrchestrator
   * `method_order` + `fallback_rules`
   * confidence: ICMP_* = high, TCP = medium, Inet = low
   * Приёмка: в debug UI видно, каким методом достигли

5. Единый маппинг ошибок
   * всё сводим в ErrorClass
   * Приёмка: нет “Unknown”, всегда понятная причина

Зависимости: Sprint 3 (cellular binding желательно), Sprint 1 (transport).

### Sprint 6 — Sampling/Chunking + CampaignRunner

Цель: агент реально исполняет кампанию: targets → sample → probe → summary.

Задачи:

1. IPv4/CIDR утилиты + unit tests
   * Приёмка: тесты на /32, /31, /24, корректный usable range

2. Seed + deterministic sampling
   * по правилам TaskSpec (FNV-1a 32 + Xorshift32)
   * Приёмка: совпадает с эталонными тест-векторами (первые N IP)

3. Chunking + throttling
   * `pps_limit`, `max_parallel`, `chunk_size`
   * Приёмка: фактический темп не превышает лимит, параллельность соблюдается

4. CampaignRunner state machine
   * Idle → Preparing → RunningSubnet → Done
   * early_stop (`no_reply_after` / `max_seconds`)
   * Приёмка: агент проходит хотя бы 10 подсетей, шлёт partial (если включено) и final summary

Зависимости: Sprint 4 (targets), Sprint 5 (probe).

### Sprint 7 — Batching/acks + stop/resume

Цель: устойчивость: данные не теряются, ack/retry работает, stop корректный.

Задачи:

1. ResultAccumulator
   * sampled/replied
   * RTT min/avg/p95
   * method_used/confidence counts
   * stop_reason/error_code
   * Приёмка: summary корректный при любых stop_reason

2. MeasurementsBatchBuilder
   * seq, is_final, run_id
   * cap на points
   * Приёмка: payload не превышает `max_payload_bytes` из `hello_ack`

3. Отправка с ack/retry
   * если ack не пришёл → resend с тем же `msg_id`
   * Приёмка: сеть выключили на 30 сек → включили → батчи дошли и принялись без дублей

4. campaign_stop
   * остановить выдачу новых subnet
   * завершить текущий chunk best-effort
   * отправить final с stop_reason=canceled
   * Приёмка: stop работает в середине выполнения и не ломает состояние

Зависимости: Sprint 6 + Sprint 1 (outbox/ack).

### Sprint 8 — Adhoc ping + UX/диагностика + hardening

Цель: оператор может «пинг сейчас» + видно, почему не работает. Добавляем предохранители.

Задачи:

1. Adhoc task support
   * ip (лучше только ip в MVP)
   * short window, быстрый ответ
   * Приёмка: adhoc выполняется и шлёт результат

2. UI: Active task details
   * текущая задача, прогресс, stop_reason
   * Приёмка: видно, что выполняется и где затык

3. UI: Probe debug
   * ввод IP → метод/RTT/error_class
   * Приёмка: джун дебажит без adb

4. Hardening
   * battery/thermal/network metered checks
   * локальные safety caps: если сервер прислал “бешеные” параметры → `POLICY_REJECTED`
   * Приёмка: агент отклоняет опасные спеки и корректно репортит

Зависимости: Sprint 7.

## Что отдаём разработчику как «готовое ТЗ» до начала работ

1. JSON контракты agent↔gw + ProbeSpec v2
   * `method_order`, `tcp_ports`, `fallback_rules`

2. Список ErrorClass + mapping rules
   * единый справочник “что значит ошибка”

3. Acceptance checklist (ручные шаги на девайсе)
   * для каждого спринта отдельный список “как проверить”

4. Definition of Done проекта
   * работает через cellular при включенном Wi-Fi
   * ICMP_DGRAM поддержан, fallback цепочка работает
   * reconnect + resend без потерь
   * кампанию можно остановить
   * debug UI показывает причину проблемы

## Мини-заметка

Если ICMP_DGRAM на части устройств не работает (ROM/политики) — это не фейл проекта: у вас есть ICMP_EXEC и TCP_CONNECT. Главное — всегда отправлять `method_used` + `error_class`, чтобы аналитика на бэке была честной (а не “0 ответов = подсеть мертва”).
