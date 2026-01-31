package com.topzurdo.launcher.util;

/**
 * OS / runtime utilities (memory, paths).
 */
public final class OSUtils {

    /**
     * Total system memory in MB, or 0 if unavailable.
     */
    public static long getTotalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean os =
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
                long total = sun.getTotalMemorySize();
                return total > 0 ? total / (1024 * 1024) : 0;
            }
        } catch (Throwable ignored) { }
        return Runtime.getRuntime().maxMemory() > 0
            ? Runtime.getRuntime().maxMemory() / (1024 * 1024) * 4
            : 0;
    }

    private OSUtils() {}
}
