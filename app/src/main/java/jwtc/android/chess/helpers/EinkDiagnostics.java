package jwtc.android.chess.helpers;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileWriter;

/**
 * TEMPORARY diagnostic. Remove once the e-ink button rendering is understood.
 *
 * Text buttons render black on black in e-ink mode while image buttons, whose
 * style is colour-identical, render correctly. Five rounds of inference have
 * each been falsified on the device, so this reports what the widgets actually
 * resolve at runtime instead of what the resources say they should.
 *
 * Writes to the app's external files dir, which is readable over MTP without
 * needing adb.
 */
public class EinkDiagnostics {

    private static final String TAG = "EinkDiagnostics";

    public static void dump(Activity activity, MaterialButton textButton, MaterialButton imageButton) {
        try {
            final StringBuilder sb = new StringBuilder();
            sb.append("einkMode=").append(EinkMode.isEnabled()).append('\n');
            sb.append("activity=").append(activity.getClass().getSimpleName()).append('\n');

            appendThemeStyle(activity, sb, "materialButtonStyle",
                com.google.android.material.R.attr.materialButtonStyle);
            appendThemeStyle(activity, sb, "chessImageButtonStyle",
                jwtc.android.chess.R.attr.chessImageButtonStyle);

            appendButton(sb, "textButton(Show)", textButton);
            appendButton(sb, "imageButton(Retry)", imageButton);

            final File out = new File(activity.getExternalFilesDir(null), "eink-diag.txt");
            try (FileWriter w = new FileWriter(out, false)) {
                w.write(sb.toString());
            }
            Log.i(TAG, "wrote " + out.getAbsolutePath() + "\n" + sb);
        } catch (Exception e) {
            Log.e(TAG, "diagnostic failed", e);
        }
    }

    private static void appendThemeStyle(Activity activity, StringBuilder sb, String label, int attr) {
        final TypedValue tv = new TypedValue();
        final boolean found = activity.getTheme().resolveAttribute(attr, tv, true);
        sb.append(label).append('=');
        if (!found || tv.resourceId == 0) {
            sb.append("UNRESOLVED");
        } else {
            try {
                sb.append(activity.getResources().getResourceEntryName(tv.resourceId));
            } catch (Exception e) {
                sb.append("id:").append(tv.resourceId);
            }
        }
        sb.append('\n');
    }

    private static void appendButton(StringBuilder sb, String label, MaterialButton button) {
        sb.append(label).append(':');
        if (button == null) {
            sb.append(" null\n");
            return;
        }
        final ColorStateList bg = button.getBackgroundTintList();
        final ColorStateList stroke = button.getStrokeColor();
        sb.append(" bgTint=").append(describe(bg, button));
        sb.append(" stroke=").append(describe(stroke, button));
        sb.append(" strokeWidth=").append(button.getStrokeWidth());
        sb.append(" textColor=").append(hex(button.getCurrentTextColor()));
        sb.append(" bgClass=").append(button.getBackground() == null
            ? "null" : button.getBackground().getClass().getSimpleName());
        sb.append(" enabled=").append(button.isEnabled());
        sb.append(" checked=").append(button.isChecked());
        sb.append(" pressed=").append(button.isPressed());
        sb.append('\n');
    }

    private static String describe(ColorStateList csl, View view) {
        if (csl == null) {
            return "null";
        }
        return hex(csl.getColorForState(view.getDrawableState(), csl.getDefaultColor()))
            + "(default " + hex(csl.getDefaultColor()) + ")";
    }

    private static String hex(int color) {
        return String.format("#%08X", color);
    }
}
