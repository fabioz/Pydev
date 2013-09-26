package org.python.pydev.shared_core.utils;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;

public class PlatformUtils {

    /**
     * Currently we only care for: windows, mac os or linux (if we need some other special support,
     * this could be improved).
     */
    public static Integer platform;

    public static int WINDOWS = 1;

    public static int MACOS = 2;

    public static int LINUX = 3;

    static {
        try {
            String os = Platform.getOS();
            if (os.equals(Constants.OS_WIN32)) {
                platform = WINDOWS;
            } else if (os.equals(Constants.OS_MACOSX)) {
                platform = MACOS;
            } else {
                platform = LINUX;
            }

        } catch (NullPointerException e) {
            String env = System.getProperty("os.name").toLowerCase();
            if (env.indexOf("win") != -1) {
                platform = WINDOWS;
            } else if (env.startsWith("mac os")) {
                platform = MACOS;
            } else {
                platform = LINUX;
            }
        }
    }

    /**
     * @return whether we are in windows or not
     */
    public static boolean isWindowsPlatform() {
        return platform == WINDOWS;
    }

    /**
     * @return whether we are in MacOs or not
     */
    public static boolean isMacOsPlatform() {
        return platform == MACOS;
    }

    /**
     * @return whether we are in Linux or not
     */
    public static boolean isLinuxPlatform() {
        return platform == LINUX;
    }

}
