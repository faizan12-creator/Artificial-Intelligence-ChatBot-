package AIchatbot;

/**
 * AppTheme — All color constants for dark and light mode.
 * Call AppTheme.toggle() to switch themes.
 */
public class AppTheme {

    public static boolean isDark = true;

    public static String C_BG, C_SURFACE, C_SURFACE2, C_SURFACE3;
    public static String C_BORDER, C_BORDER2;
    public static String C_USER_BUBBLE, C_BOT_BUBBLE;
    public static String C_ACCENT, C_ACCENT2, C_ACCENT3;
    public static String C_GREEN, C_RED, C_RED_DIM, C_YELLOW;
    public static String C_TEXT, C_TEXT_SEC, C_TEXT_MUTED;
    public static String C_CODE_BG, C_CODE_TEXT, C_SEND;

    // Static block to initialize defaults on load
    static {
        applyDark();
    }

    public static void toggle() {
        isDark = !isDark;
        if (isDark) applyDark(); else applyLight();
    }

    public static void applyDark() {
        C_BG          = "#09090c";
        C_SURFACE     = "#101014";
        C_SURFACE2    = "#17171d";
        C_SURFACE3    = "#1e1e27";
        C_BORDER      = "#22222c";
        C_BORDER2     = "#2b2b38";
        C_USER_BUBBLE = "#1d4ed8";
        C_BOT_BUBBLE  = "#111118";
        C_ACCENT      = "#f59e0b";
        C_ACCENT2     = "#7c3aed";
        C_ACCENT3     = "#06b6d4";
        C_GREEN       = "#22c55e";
        C_RED         = "#ef4444";
        C_RED_DIM     = "#7f1d1d";
        C_YELLOW      = "#eab308";
        C_TEXT        = "#f1f0ef";
        C_TEXT_SEC    = "#9ca3af";
        C_TEXT_MUTED  = "#4b5563";
        C_CODE_BG     = "#080810";
        C_CODE_TEXT   = "#e2e8f0";
        C_SEND        = "#7c3aed";
    }

    public static void applyLight() {
        C_BG          = "#f5f5f7";
        C_SURFACE     = "#ffffff";
        C_SURFACE2    = "#f0f0f5";
        C_SURFACE3    = "#e5e5ea";
        C_BORDER      = "#d1d1d6";
        C_BORDER2     = "#c7c7cc";
        C_USER_BUBBLE = "#1d4ed8";
        C_BOT_BUBBLE  = "#ffffff";
        C_ACCENT      = "#d97706";
        C_ACCENT2     = "#7c3aed";
        C_ACCENT3     = "#0891b2";
        C_GREEN       = "#16a34a";
        C_RED         = "#dc2626";
        C_RED_DIM     = "#fee2e2";
        C_YELLOW      = "#ca8a04";
        C_TEXT        = "#111827";
        C_TEXT_SEC    = "#374151";
        C_TEXT_MUTED  = "#6b7280";
        C_CODE_BG     = "#f1f5f9";
        C_CODE_TEXT   = "#1e293b";
        C_SEND        = "#7c3aed";
    }

    /** Returns inline CSS string for Scene stylesheet */
    public static String buildCSS() {
        return
                ".text-area .content{-fx-background-color:" + C_SURFACE2 + ";}" +
                        ".text-area{-fx-background-color:" + C_SURFACE2 + ";-fx-background-radius:12;}" +
                        ".text-area .scroll-pane{-fx-background-color:transparent;}" +
                        ".text-area .scroll-pane .viewport{-fx-background-color:transparent;}" +
                        ".scroll-bar:vertical .track{-fx-background-color:" + C_SURFACE2 + ";}" +
                        ".scroll-bar:vertical .thumb{-fx-background-color:" + C_BORDER2 + ";-fx-background-radius:4;}" +
                        ".scroll-bar{-fx-background-color:transparent;}" +
                        ".combo-box-popup .list-view{-fx-background-color:" + C_SURFACE2 + ";}" +
                        ".combo-box-popup .list-cell{-fx-text-fill:" + C_TEXT + ";-fx-background-color:" + C_SURFACE2 + ";}" +
                        ".combo-box-popup .list-cell:hover{-fx-background-color:" + C_SURFACE3 + ";}";
    }
} // Yeh hai wo last bracket jo poori class ko band karega