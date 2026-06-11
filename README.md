<div align="center">

# ⚔️ Silentium

### Advanced Anti-Cheat for Paper 1.21.x

[![Paper](https://img.shields.io/badge/Paper-1.21.x-00AA00?style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgo=)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

</div>

---

## 🇷🇺 Русский

### О плагине

**Silentium** — античит плагин для серверов на базе **Paper 1.21.x**, разработанный для обнаружения широкого спектра читов: от очевидных (полёт, спидхак) до скрытых (невидимые изменения скорости, прицеливание с автонаводкой).

Плагин делится на три категории проверок:

| Категория | Описание |
|-----------|----------|
| 🔴 **Blatant** | Явные читы, видимые невооружённым глазом |
| 🟡 **Ghost** | Скрытые читы, сложно определяемые визуально |
| 🟢 **Anarchy** | Читы, специфичные для серверов без правил |

---

### Проверки

#### 🔴 Blatant — явные читы

| Проверка | Что обнаруживает |
|----------|-----------------|
| **Fly** | Полёт, высокий прыжок, зависание в воздухе |
| **Speed** | Скорость выше допустимой (с учётом элитры, рывка, ботинок Скорости Душ) |
| **KillAura** | Атаки сквозь блоки, подозрительные углы удара |
| **Reach** | Дистанция атаки больше стандартной |
| **NoFall** | Отмена урона от падения |
| **AutoClicker** | Скорость кликов выше возможной вручную |
| **AntiKnockback** | Уклонение от отброса |
| **Scaffold** | Автоматическая установка блоков под ногами |

#### 🟡 Ghost — скрытые читы

| Проверка | Что обнаруживает |
|----------|-----------------|
| **GhostReach** | Тихая дистанция атаки (чуть больше нормы) |
| **AimAssist** | Резкие, нечеловеческие повороты камеры во время боя |
| **GhostTarget** | **Honeypot:** невидимый моб-приманка — авто-аим cheata автоматически наводится на него |
| **Velocity** | Частичное поглощение отброса |
| **Timer** | Ускорение игрового времени (Timer hack) |
| **GhostAutoClicker** | Клики в нечеловеческом, но умеренном темпе |

#### 🟢 Anarchy — специфичные читы

| Проверка | Что обнаруживает |
|----------|-----------------|
| **XRay** | Добыча руды по подозрительным паттернам |
| **FastPlace** | Слишком быстрая установка блоков |
| **Phase** | Движение сквозь блоки |

---

### Команды

> Основная команда: `/silentium` · Псевдонимы: `/sac`, `/anticheat`

| Команда | Описание |
|---------|----------|
| `/sac help` | Список всех команд |
| `/sac status` | Статус плагина и активных проверок |
| `/sac alerts` | Включить/выключить оповещения для себя |
| `/sac check <игрок>` | Показать нарушения игрока |
| `/sac vl <игрок>` | Показать VL (уровень нарушений) |
| `/sac spec <игрок>` | Войти в режим наблюдения |
| `/sac whitelist <add/remove/list>` | Управление белым списком |
| `/sac reload` | Перезагрузить конфигурацию |
| `/sac version` | Версия плагина |

---

### Разрешения

| Разрешение | Описание | По умолчанию |
|------------|----------|--------------|
| `silentium.admin` | Полный доступ ко всем командам | OP |
| `silentium.mod` | Доступ модератора: оповещения + команды | `false` |
| `silentium.alerts` | Только получение оповещений | `false` |
| `silentium.bypass` | Обход всех проверок | `false` |

---

### Установка

1. Скачай JAR-файл из [Releases](../../releases)
2. Положи в папку `plugins/` на сервере
3. Перезапусти сервер
4. Настрой файлы в `plugins/Silentium/`

### Требования

- **Сервер:** Paper 1.21.x (или форк)
- **Java:** 21 или выше
- **Совместимость:** PlugmanX для горячей перезагрузки

---

## 🇬🇧 English

### About

**Silentium** is an anti-cheat plugin for **Paper 1.21.x** servers, designed to detect a wide range of cheats — from obvious hacks (fly, speed) to subtle ones (soft velocity, aim-assist).

Checks are split into three categories:

| Category | Description |
|----------|-------------|
| 🔴 **Blatant** | Obvious cheats, visible to the naked eye |
| 🟡 **Ghost** | Subtle cheats, hard to spot visually |
| 🟢 **Anarchy** | Cheats specific to no-rules environments |

---

### Checks

#### 🔴 Blatant

| Check | Detects |
|-------|---------|
| **Fly** | Flight, high jump, mid-air hovering |
| **Speed** | Horizontal speed above the physics limit (accounts for elytra, sprint, Soul Speed boots) |
| **KillAura** | Attacks through blocks, suspicious hit angles |
| **Reach** | Attack distance beyond the server-side limit |
| **NoFall** | Cancelling fall damage |
| **AutoClicker** | Click rates beyond human capability |
| **AntiKnockback** | Ignoring or absorbing knockback |
| **Scaffold** | Automated block-placement under the player's feet |

#### 🟡 Ghost

| Check | Detects |
|-------|---------|
| **GhostReach** | Slightly extended attack range (ghost variety) |
| **AimAssist** | Inhuman camera snaps during combat |
| **GhostTarget** | **Honeypot:** an invisible entity is placed near every player — aim-assist cheats silently lock onto it, triggering a detection |
| **Velocity** | Partial knockback absorption |
| **Timer** | Client-side tick speed manipulation |
| **GhostAutoClicker** | Moderate but inhuman click patterns |

#### 🟢 Anarchy

| Check | Detects |
|-------|---------|
| **XRay** | Suspicious ore-mining patterns consistent with X-Ray |
| **FastPlace** | Block placement faster than humanly possible |
| **Phase** | Movement through solid blocks |

---

### Commands

> Main command: `/silentium` · Aliases: `/sac`, `/anticheat`

| Command | Description |
|---------|-------------|
| `/sac help` | Show all commands |
| `/sac status` | Plugin status and active checks |
| `/sac alerts` | Toggle alerts for yourself |
| `/sac check <player>` | Show a player's violations |
| `/sac vl <player>` | Show player violation level |
| `/sac spec <player>` | Enter spectator mode on a player |
| `/sac whitelist <add/remove/list>` | Manage the whitelist |
| `/sac reload` | Reload configuration files |
| `/sac version` | Plugin version |

---

### Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `silentium.admin` | Full access to all commands | OP |
| `silentium.mod` | Moderator access: alerts + commands | `false` |
| `silentium.alerts` | Receive alerts only | `false` |
| `silentium.bypass` | Bypass all checks | `false` |

---

### Installation

1. Download the JAR from [Releases](../../releases)
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Configure the files inside `plugins/Silentium/`

### Requirements

- **Server:** Paper 1.21.x (or a fork)
- **Java:** 21 or higher
- **Hot-reload:** PlugmanX compatible

---

<div align="center">

Made by **IlSuBb**

</div>
