package com.topzurdo.launcher.config;

/**
 * JVM profile presets: по объёму RAM (LIGHT/MEDIUM/HEAVY) и по сценарию (PvP/Building/Low PC).
 */
public enum JvmProfile {

    LIGHT("Лёгкий", "-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xmx2G"),
    MEDIUM("Средний", "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 "
        + "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch "
        + "-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M "
        + "-XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 "
        + "-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 "
        + "-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem "
        + "-XX:MaxTenuringThreshold=1"),
    HEAVY("Мощный", "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 "
        + "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch "
        + "-XX:G1NewSizePercent=40 -XX:G1MaxNewSizePercent=50 -XX:G1HeapRegionSize=16M "
        + "-XX:G1ReservePercent=25 -XX:G1MixedGCCountTarget=8 -XX:MaxTenuringThreshold=1"),
    PVP("PvP (плавность)", "-XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=adaptive "
        + "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch"),
    BUILDING("Строительство (стабильность)", "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 "
        + "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch "
        + "-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M "
        + "-XX:G1ReservePercent=20 -XX:G1MixedGCCountTarget=4 -XX:MaxTenuringThreshold=1"),
    LOW_PC("Слабый ПК (выживание)", "-XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:G1NewSizePercent=20 "
        + "-XX:G1MaxNewSizePercent=30 -XX:+DisableExplicitGC -XX:+AlwaysPreTouch");

    private final String displayName;
    private final String jvmArgs;

    JvmProfile(String displayName, String jvmArgs) {
        this.displayName = displayName;
        this.jvmArgs = jvmArgs;
    }

    public String getDisplayName() { return displayName; }
    public String getJvmArgs() { return jvmArgs; }

    public static JvmProfile fromId(String id) {
        if (id == null || id.isEmpty()) return MEDIUM;
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
