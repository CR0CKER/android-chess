package jwtc.android.chess.helpers;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import jwtc.android.chess.constants.ColorSchemes;
import jwtc.android.chess.constants.PieceSets;

/**
 * E-ink mode: a single master switch that adapts the UI for electrophoretic
 * (e-paper) displays such as the Onyx Boox range.
 *
 * E-ink panels refresh slowly and hold their previous image, so anything that
 * moves or repaints continuously leaves a visible ghost trail rather than an
 * animation. When this mode is on the app avoids continuous repaints entirely:
 * no drag shadow following the finger, no indeterminate progress bars, no
 * pulse or list animations, no ripples, and a high-contrast greyscale board.
 *
 * The flag is read once per activity resume and cached statically, mirroring
 * how {@link jwtc.android.chess.constants.ColorSchemes} and
 * {@link jwtc.android.chess.constants.PieceSets} already share board settings.
 */
public class EinkMode {

    public static final String PREF_KEY = "einkMode";

    /**
     * Vendors whose entire product line is electrophoretic, matched against
     * Build.MANUFACTURER / BRAND / MODEL.
     *
     * Android exposes no way to ask whether a display is e-ink — there is no
     * panel-technology API — so this is a vendor allowlist, not a property of
     * the screen. It is deliberately conservative: mixed-line vendors such as
     * Hisense and TCL ship both e-ink readers and ordinary LCD phones and are
     * left out, because a false positive would hand an LCD user a greyscale
     * board and no animations for no reason. Anything unrecognised defaults
     * off, which is the correct answer for the overwhelming majority of
     * devices.
     */
    private static final String[] EINK_VENDORS = {
        "onyx", "boox", "bigme", "dasung", "meebook", "boyue", "likebook",
        "supernote", "ratta", "moaan", "pocketbook", "remarkable", "inkbook",
    };

    private static Boolean einkHardware = null;

    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Load the flag from preferences. Called from activity onResume, alongside
     * the other appearance preferences.
     */
    public static void load(SharedPreferences prefs) {
        enabled = prefs.getBoolean(PREF_KEY, defaultEnabled());
    }

    /**
     * Whether the mode should start on, for an install that has never been
     * configured. An explicit choice always wins: once the preference exists,
     * this is not consulted again.
     */
    public static boolean defaultEnabled() {
        return isEinkHardware();
    }

    /**
     * Best-effort identification of e-ink hardware by vendor. See
     * {@link #EINK_VENDORS} for why this cannot query the display itself.
     */
    public static boolean isEinkHardware() {
        if (einkHardware == null) {
            final String fingerprint = (Build.MANUFACTURER + ' ' + Build.BRAND + ' ' + Build.MODEL)
                .toLowerCase(Locale.ROOT);
            boolean match = false;
            for (String vendor : EINK_VENDORS) {
                if (fingerprint.contains(vendor)) {
                    match = true;
                    break;
                }
            }
            einkHardware = match;
        }
        return einkHardware;
    }

    /**
     * Used by the preferences screen so the board preview updates live, before
     * the value has been written back to SharedPreferences.
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Force the board appearance settings that e-ink needs, overriding the
     * user's saved colour scheme, piece set and tile effect. Those choices stay
     * in SharedPreferences and take effect again when e-ink mode is turned off.
     *
     * Call after the appearance preferences have been read.
     */
    public static void applyBoardAppearance() {
        if (!enabled) {
            return;
        }
        ColorSchemes.selectedColorScheme = ColorSchemes.EINK;
        ColorSchemes.selectedPattern = 0;     // the tile patterns are alpha gradients
        ColorSchemes.saturationFactor = 1.0f; // desaturation is a no-op on greyscale
        PieceSets.selectedSet = PieceSets.ALPHA; // the only flat, gradient-free set
    }

    /**
     * Disable item change animations, which fade and slide rows on every
     * update. Safe to call with null.
     */
    public static void applyTo(RecyclerView recyclerView) {
        if (enabled && recyclerView != null) {
            recyclerView.setItemAnimator(null);
        }
    }
}
