package jwtc.android.chess.helpers;

import android.content.SharedPreferences;

import androidx.recyclerview.widget.RecyclerView;

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
     * On by default in this fork: it exists to be run on e-ink hardware, and a
     * fresh install that comes up in the colour theme is a trap — the stock
     * filled buttons render on greyscale as dark blocks that look like a
     * styling bug rather than a mode that is simply switched off.
     *
     * Upstream would default this to false.
     */
    public static final boolean DEFAULT_ENABLED = true;

    private static boolean enabled = DEFAULT_ENABLED;

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Load the flag from preferences. Called from activity onResume, alongside
     * the other appearance preferences.
     */
    public static void load(SharedPreferences prefs) {
        enabled = prefs.getBoolean(PREF_KEY, DEFAULT_ENABLED);
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
