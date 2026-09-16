/*
 * MoonLight — Android / Pojav / Zalith 兼容检测
 */
package wtf.moonlight.utils.android;

/**
 * 检测安卓启动器环境，用于关闭 Discord RPC、系统托盘等桌面-only 功能。
 */
public final class AndroidCompat {
    private static final boolean ANDROID = detect();

    private AndroidCompat() {}

    private static boolean detect() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            String vendor = System.getProperty("java.vendor", "").toLowerCase();
            String vm = System.getProperty("java.vm.name", "").toLowerCase();
            String runtime = System.getProperty("java.runtime.name", "").toLowerCase();
            // Pojav / Boardwalk / Zalith 常见特征
            if (os.contains("android")) return true;
            if (vendor.contains("android") || vendor.contains("termux")) return true;
            if (vm.contains("dalvik") || runtime.contains("android")) return true;
            // 部分启动器把 os.name 设成 Linux，但带这些属性
            if (System.getProperty("pojav.path.minecraft") != null) return true;
            if (System.getProperty("pojav.path.private.account") != null) return true;
            if (System.getenv("POJAV_RENDERER") != null) return true;
            if (System.getenv("LIBGL_ES") != null) return true;
            if (System.getenv("POJAV_NATIVEDIR") != null) return true;
            // Zalith
            if (System.getenv("ZALITH_VERSION_CODE") != null) return true;
            // MobileGlues / FCL
            if (System.getProperty("org.lwjgl.opengl.libname", "").toLowerCase().contains("mobileglues")) return true;
            if (System.getProperty("org.lwjgl.opengl.libname", "").toLowerCase().contains("libgl")) {
                String gl = System.getProperty("org.lwjgl.opengl.libname", "").toLowerCase();
                if (gl.contains("gles") || gl.contains("egl")) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isAndroid() {
        return ANDROID;
    }

    /** 桌面 Discord / Tray 等 */
    public static boolean isDesktopExtrasAllowed() {
        return !ANDROID;
    }
}
