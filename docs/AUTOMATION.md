# Автоматизация сборки и развёртывания TopZurdo

## Обзор

- **Хеширование:** SHA-256 по исходникам мода (`mod/src/**/*.java`, `*.json`) для инкрементальной сборки.
- **Сборка:** Gradle `:mod:build` только при изменении хеша (или по флагу `--force`).
- **Развёртывание:** копирование JAR в папку `mods/` Minecraft, удаление старой версии мода.

## Настройка окружения

### 1. Папка mods

Создайте `scripts/deploy.config` (скопируйте из `scripts/deploy.config.example`):

```ini
# Путь к папке mods Minecraft (профиль лаунчера или .minecraft)
MODS_DIR=C:\Users\<user>\AppData\Roaming\.minecraft\mods
# или профиль лаунчера:
# MODS_DIR=C:\Games\TopZurdo\profiles\1.16.5-fabric\mods
```

Либо задайте переменную окружения:

```bat
set MODS_DIR=C:\path\to\minecraft\mods
```

### 2. Gradle

В корне проекта:

```bat
gradlew.bat :mod:build
```

Задачи в `mod/build.gradle`:

- `modSourceHash` — записать SHA-256 исходников в `mod/build/mod-source.sha256`.
- `printModJarPath` — вывести путь к собранному JAR (для скриптов).

## Скрипты (Python 3)

### build_deploy.py

Одна сборка и деплой:

```bat
cd C:\Users\1\Desktop\TopZurdo
python scripts\build_deploy.py
```

- Считает хеш исходников; если хеш изменился (или передан `--force`) — запускает `gradlew.bat :mod:build`.
- Копирует `mod/build/libs/topzurdo-*.jar` в `MODS_DIR`, предварительно удаляя старые `topzurdo-*.jar`.

Опции:

- `--force` — всегда собирать и деплоить.
- `--build-only` — только сборка, без копирования в mods.
- `--hash-only` — только вывести текущий хеш исходников.

### watch_build_deploy.py

Следит за изменениями в `mod/src` и при изменении запускает `build_deploy.py`:

```bat
python scripts\watch_build_deploy.py
```

- По умолчанию использует `watchdog` (если установлен: `pip install watchdog`).
- `--poll` — режим опроса без watchdog.

## Интеграция с лаунчером

Чтобы лаунчер не требовал удаления папки Minecraft при обновлении мода:

1. Лаунчер должен запускать клиент с каталогом игры (gameDir), где лежит своя папка `mods`.
2. В `deploy.config` укажите этот каталог: `MODS_DIR=<gameDir>\mods`.
3. После сборки скрипт копирует новый JAR и удаляет старый; при следующем запуске клиента из лаунчера подхватится новая версия.

Рекомендуется в лаунчере не трогать весь каталог игры, а только обновлять файлы в `mods/` (например, через этот скрипт или отдельную кнопку «Обновить мод»).

## Тестовый сценарий

1. Настроить `deploy.config` или `MODS_DIR`.
2. `python scripts\build_deploy.py --force` — первая сборка и деплой.
3. Изменить любой `.java` в `mod/src`, снова `python scripts\build_deploy.py` — должна пройти только пересборка и копирование.
4. Запустить Minecraft (или лаунчер) и убедиться, что в меню модов отображается актуальная версия TopZurdo.
