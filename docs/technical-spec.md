# Полное техническое задание (Android Client + Backend Contracts)

Версия: 1.1

Платформа: Android

Тип приложения: Mobile Agent / Distributed Network Probe

## 1. Назначение приложения

ShadowPinger — мобильное Android-приложение, работающее как управляемый агент распределённого сетевого тестирования.

Приложение выполняет ICMP-проверку доступности IP-адресов в заданных подсетях через реальные мобильные SIM-карты устройства и передаёт результаты на backend.

Приложение не является пользовательским инструментом, а управляется backend-системой через Kafka.

## 2. Основные ограничения и допущения

### 2.1 Ограничения Android

* Использование raw socket запрещено.
* Фоновая работа ограничена Doze / App Standby.
* Для стабильной работы используется Foreground Service.
* Сетевые операции должны учитывать:
  * battery state
  * network state
  * mobile data availability

### 2.2 Сетевой метод (КЛЮЧЕВО)

* Используется ICMP Echo (Ping).
* Реализация через SOCK_DGRAM + IPPROTO_ICMP.
* Root не требуется.
* Работает в Android sandbox.
* Поддерживается большинством мобильных операторов.
* Целевые хосты: VPS-серверы (ICMP доступен).
* ❗ TCP / UDP НЕ используются и НЕ допускаются.

## 3. Реализация ICMP (ОБЯЗАТЕЛЬНО)

### 3.1 Используемая библиотека

icmp4a (Kotlin / Java)

Характеристики:

* SOCK_DGRAM
* Без root
* Open Source
* Проверена в production
* Аналогично тому, как работают публичные Ping-приложения

### 3.2 Пример использования (reference)

```kotlin
val icmp = Icmp4a()
val status = icmp.ping("8.8.8.8")

if (status.isReachable) {
    val latency = status.timeMs
}
```

Требования к использованию:

* один ICMP Echo = один измеряемый IP
* latency измеряется как RTT
* таймаут считается host_status = down
* повторные попытки — через retries

## 4. Архитектура (High-Level)

```
Android App (Agent)
 ├─ UI Layer
 │   ├─ Registration
 │   ├─ Authorization
 │   ├─ Settings
 │   └─ Agent Status
 │
 ├─ Core Layer
 │   ├─ State Machine
 │   ├─ SIM Manager
 │   ├─ Job Executor
 │   ├─ ICMP Ping Engine
 │   ├─ Metrics Collector
 │   └─ Local Storage
 │
 ├─ Network Layer
 │   ├─ Kafka Consumer (jobs)
 │   └─ Kafka Producer (metrics/results)
 │
 └─ OS Integration
     ├─ Foreground Service
     ├─ Battery / Network Monitor
     └─ Permissions Manager
```

## 5. Пользовательские экраны (UI)

### 5.1 Экран регистрации

Назначение: создание аккаунта.

Поля:

* login / email
* password
* кнопка «Зарегистрироваться»

Логика:

* запрос на backend
* при успехе → экран авторизации

### 5.2 Экран авторизации

Поля:

* login
* password

Логика:

* получение auth-token
* сохранение token в secure storage
* переход в настройки

### 5.3 Экран настроек (КРИТИЧЕСКИЙ)

#### 5.3.1 Разрешения

* Foreground service
* Ignore battery optimizations
* Mobile network access
* SIM access

#### 5.3.2 SIM-карты

* список всех SIM
* для каждой:
  * локальное имя
  * UID SIM
  * оператор:
    * BEELINE
    * MTS
    * MEGAFON
    * TELE2
    * RTC

❗ Пока все SIM не сопоставлены, агент НЕ активируется.

### 5.4 Экран состояния агента

Отображает:

* state
* активную SIM
* job_id
* прогресс
* IP tested
* последние ошибки

## 6. State Machine клиента

```
INIT
 ↓
REGISTERED
 ↓
AUTHORIZED
 ↓
PERMISSIONS_GRANTED
 ↓
SIMS_MAPPED
 ↓
KAFKA_REGISTERED
 ↓
IDLE
 ↓
TESTING
 ↓
REPORTING
 ↓
IDLE
```

* state сохраняется локально
* восстанавливается после kill / reboot
* все переходы логируются

## 7. Идентификация устройства

* hwid — UUID v4
* генерируется при первом запуске
* immutable
* используется во всех сообщениях

## 8. Kafka: входящие задания

Топик: `subnet_jobs`

```json
{
  "job_id": "uuid",
  "created_at": "2026-01-10T12:00:00Z",
  "ttl_seconds": 3600,
  "subnets": ["5.188.140.0/22"],
  "mobile_operators": ["BEELINE", "MTS"],
  "ping_config": {
    "method": "icmp",
    "timeout_ms": 1500,
    "retries": 1,
    "concurrency": 64,
    "sampling_ratio": 1.0
  }
}
```

## 9. Алгоритм выполнения задания

### 9.1 Общая логика

Для каждого оператора:

* выбрать SIM
* для каждой подсети:
  * вычислить диапазон IP
  * применить sampling
  * разбить на чанки
  * выполнить ICMP ping для каждого IP

### 9.2 Асинхронная модель

```
SupervisorJob
 ├─ OperatorScope
 │   ├─ SubnetScope
 │   │   ├─ IcmpPingJob(IP)
```

* Kotlin Coroutines
* Semaphore для ограничения concurrency
* один ICMP запрос = одна coroutine

## 10. Внутренние структуры данных

### 10.1 Представление IP

* IPv4 → Int
* CIDR → base + mask

### 10.2 Структуры

```
OperatorStats
 └─ Map<Subnet, SubnetStats>

SubnetStats:
  BitSet tested
  BitSet up
  LongArray latency
```

## 11. Метрики и телеметрия

### 11.1 Heartbeat

Kafka topic: `subnet_heartbeats`

Период: 30–60 сек

```json
{
  "hwid": "uuid-device",
  "timestamp": "2026-01-10T12:10:00Z",
  "state": "TESTING",
  "battery_level": 0.74,
  "network_type": "MOBILE",
  "active_sim": "BEELINE",
  "current_job_id": "uuid",
  "progress": {
    "subnets_total": 4,
    "subnets_completed": 1,
    "ips_tested": 256
  }
}
```

Backend использует:

* мониторинг живых агентов
* контроль зависших job
* автоматический retry job

### 11.2 Execution Metrics

```json
{
  "job_id": "uuid",
  "hwid": "uuid-device",
  "operator": "MTS",
  "subnet": "5.188.140.0/22",
  "metrics": {
    "ips_total": 1022,
    "ips_tested": 512,
    "ips_up": 480,
    "avg_latency_ms": 21.3,
    "p95_latency_ms": 54.1,
    "timeouts": 12,
    "errors": 3
  },
  "timestamp": "2026-01-10T12:20:00Z"
}
```

## 12. Частичные результаты

```json
{
  "job_id": "uuid",
  "hwid": "uuid-device",
  "operator": "BEELINE",
  "subnet": "5.188.140.0/22",
  "chunk_id": 7,
  "range": {
    "from": 3232235777,
    "to": 3232235839
  },
  "results": [
    {
      "ip": 3232235781,
      "status": "up",
      "latency": 12
    }
  ]
}
```

Требования backend:

* chunk_id может приходить в любом порядке
* возможны дубликаты
* backend обязан дедуплицировать (job_id, hwid, subnet, ip)

## 13. Финальный результат

Топик: `subnet_results_final`

```json
{
  "job_id": "uuid",
  "hwid": "uuid-device",
  "finished_at": "2026-01-10T12:30:31Z",
  "summary": {
    "total_ips_tested": 4096,
    "total_ips_up": 3820,
    "avg_latency_ms": 18.2
  },
  "operators": {
    "BEELINE": {
      "subnets": {
        "5.188.140.0/22": {
          "available_hosts": [
            "5.188.140.110",
            "5.188.140.111"
          ],
          "total_available_hosts": 2
        }
      }
    }
  }
}
```

## 14. Ошибки

Топик: `client_error_report`

```json
{
  "hwid": "uuid-device",
  "job_id": "uuid",
  "error_type": "NETWORK_TIMEOUT",
  "message": "SIM lost connectivity",
  "fatal": false,
  "timestamp": "2026-01-10T12:18:00Z"
}
```

## 15. Backend требования

Backend обязан:

* дедуплицировать (job_id, hwid, operator, ip)
* поддерживать idempotency
* хранить raw + aggregated
* иметь TTL для job
* уметь закрывать job по таймауту

## 16. Хранение данных (рекомендация)

Типы данных:

* Raw ping results (миллионы записей)
* Aggregated metrics
* Job state
* Device state

### 16.2 Рекомендуемые структуры хранения

Raw data:

* ClickHouse / ScyllaDB / Bigtable
* Партиционирование:
  * job_id
  * operator
  * subnet

Aggregates:

* PostgreSQL / ClickHouse materialized views

## 17. Безопасность

* HMAC подпись заданий
* rate limits
* автоматическая остановка при:
  * low battery
  * loss of mobile network

## 18. Не входит в v1

* TCP / UDP
* IPv6
* iOS
* Root-доступ

## 19. Итог

* Используется реальный ICMP
* Поведение идентично публичным Ping-приложениям
* Подходит для VPS
* Архитектура чистая
