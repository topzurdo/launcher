package com.topzurdo.launcher;

/**
 * Launcher-wide constants: messages, defaults, UI colors.
 */
public final class Constants {

    public static final String DEFAULT_SERVER = "mc.funtime.su";
    public static final int DEFAULT_PORT = 25565;
    public static final int DEFAULT_RAM_MB = 4096;
    public static final int MIN_RAM_MB = 1024;
    public static final int MAX_RAM_MB = 32768;

    public static final String HEX_BG = "#1a1a1f";
    public static final String HEX_BG_DARK = "#121218";
    public static final String HEX_BORDER_SUBTLE = "#2a2a32";
    public static final String HEX_TEXT = "#e8e8ec";

    public static final class Messages {
        public static final String WAITING = "Ваш приватный опыт ожидает";
        public static final String READY = "Готов к запуску";
        public static final String VERSIONS_EMPTY = "Версии не загружены";
        public static final String SERVER_LOADING = "Проверка...";
        public static final String SERVER_ONLINE = "Сервер онлайн";
        public static final String SERVER_OFFLINE = "Сервер офлайн";
        public static final String SERVER_ERROR_RETRY = "Ошибка — повторите";
        public static final String SETTINGS_SAVED = "Настройки сохранены.";
        public static final String SETTINGS_RESET = "Настройки сброшены.";
        public static final String ERROR_GAME_RUNNING = "Игра уже запущена";
        public static final String ERROR_GAME_RUNNING_DETAILS = "Закройте клиент Minecraft и попробуйте снова.";
    }

    public static final class Particles {
        public static final int BURST_COUNT = 24;
    }

    private Constants() {}
}
