package jwtc.android.chess.helpers;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.util.TypedValue;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.MaterialShapeDrawable;

import java.io.File;
import java.io.FileWriter;

/**
 * TEMPORARY diagnostic. Remove once the e-ink button rendering is understood.
 *
 * The previous round measured the buttons' properties and found them correct:
 * backgroundTint resolved white for the current state on a button that renders
 * black. So the properties are not the question — what the widget actually
 * paints is. This renders each button into a bitmap and samples it, and walks
 * the background drawable tree, so the reported tint can be compared against
 * the pixels it supposedly produces.
 *
 * Writes to the app's external files dir, readable over MTP without adb.
 */
public class EinkDiagnostics {

    private static final String TAG = "EinkDiagnostics";

    public static void dump(Activity activity, MaterialButton broken, MaterialButton working) {
        try {
            final StringBuilder sb = new StringBuilder();
            sb.append("einkMode=").append(EinkMode.isEnabled()).append('\n');

            appendThemeColor(activity, sb, "colorPrimary");
            appendThemeColor(activity, sb, "colorSurface");
            appendThemeColor(activity, sb, "colorOnSurface");

            appendButton(sb, "BROKEN(text)", broken);
            appendButton(sb, "WORKING(icon)", working);

            final File out = new File(activity.getExternalFilesDir(null), "eink-diag.txt");
            try (FileWriter w = new FileWriter(out, false)) {
                w.write(sb.toString());
            }
            Log.i(TAG, "wrote " + out.getAbsolutePath() + "\n" + sb);
        } catch (Exception e) {
            Log.e(TAG, "diagnostic failed", e);
        }
    }

    /**
     * Resolved at runtime by name: R classes are non-transitive, so the library
     * attributes are absent from the app's compile-time R even though they are
     * present in the merged resource table.
     */
    private static void appendThemeColor(Activity activity, StringBuilder sb, String name) {
        sb.append(name).append('=');
        final int attr = activity.getResources()
            .getIdentifier(name, "attr", activity.getPackageName());
        if (attr == 0) {
            sb.append("NO_SUCH_ATTR\n");
            return;
        }
        final TypedValue tv = new TypedValue();
        sb.append(activity.getTheme().resolveAttribute(attr, tv, true) ? hex(tv.data) : "UNRESOLVED");
        sb.append('\n');
    }

    private static void appendButton(StringBuilder sb, String label, MaterialButton b) {
        sb.append("--- ").append(label).append(" ---\n");
        if (b == null) {
            sb.append("null\n");
            return;
        }
        final ColorStateList bg = b.getBackgroundTintList();
        sb.append("size=").append(b.getWidth()).append('x').append(b.getHeight()).append('\n');
        sb.append("bgTintNow=").append(bg == null ? "null"
            : hex(bg.getColorForState(b.getDrawableState(), bg.getDefaultColor()))).append('\n');
        sb.append("textColorNow=").append(hex(b.getCurrentTextColor())).append('\n');
        sb.append("strokeWidth=").append(b.getStrokeWidth()).append('\n');
        sb.append("insets t/b=").append(b.getInsetTop()).append('/').append(b.getInsetBottom()).append('\n');
        sb.append("drawableState=").append(stateNames(b.getDrawableState())).append('\n');
        sb.append("bgTree:\n");
        describeDrawable(sb, b.getBackground(), 1);
        sb.append("pixels: ").append(samplePixels(b)).append('\n');
    }

    /** Render the view offscreen and read back what it actually paints. */
    private static String samplePixels(MaterialButton b) {
        final int w = b.getWidth();
        final int h = b.getHeight();
        if (w <= 0 || h <= 0) {
            return "not laid out";
        }
        final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        b.draw(new Canvas(bmp));
        final String out = "centre=" + hex(bmp.getPixel(w / 2, h / 2))
            + " inset4=" + hex(bmp.getPixel(4, h / 2))
            + " edge0=" + hex(bmp.getPixel(0, h / 2))
            + " quarter=" + hex(bmp.getPixel(w / 4, h / 4));
        bmp.recycle();
        return out;
    }

    private static void describeDrawable(StringBuilder sb, Drawable d, int depth) {
        final String pad = new String(new char[depth * 2]).replace('\0', ' ');
        if (d == null) {
            sb.append(pad).append("null\n");
            return;
        }
        sb.append(pad).append(d.getClass().getSimpleName());
        if (d instanceof LayerDrawable) {
            sb.append(" layers=").append(((LayerDrawable) d).getNumberOfLayers());
        }
        // The class names never showed where the black comes from; the shapes'
        // own fill and stroke do.
        if (d instanceof MaterialShapeDrawable) {
            final MaterialShapeDrawable msd = (MaterialShapeDrawable) d;
            sb.append(" fill=").append(msd.getFillColor() == null ? "null"
                : hex(msd.getFillColor().getDefaultColor()));
            sb.append(" strokeCol=").append(msd.getStrokeColor() == null ? "null"
                : hex(msd.getStrokeColor().getDefaultColor()));
            sb.append(" strokeW=").append(msd.getStrokeWidth());
            sb.append(" tint=").append(msd.getTintList() == null ? "null"
                : hex(msd.getTintList().getDefaultColor()));
            sb.append(" alpha=").append(msd.getAlpha());
            sb.append(" bounds=").append(msd.getBounds().width()).append('x')
                .append(msd.getBounds().height());
        }
        sb.append('\n');
        if (depth > 4) {
            return;
        }
        if (d instanceof LayerDrawable) {
            final LayerDrawable ld = (LayerDrawable) d;
            for (int i = 0; i < ld.getNumberOfLayers(); i++) {
                describeDrawable(sb, ld.getDrawable(i), depth + 1);
            }
        } else if (d instanceof InsetDrawable) {
            describeDrawable(sb, ((InsetDrawable) d).getDrawable(), depth + 1);
        } else if (d instanceof DrawableContainer) {
            final Drawable cur = d.getCurrent();
            if (cur != null && cur != d) {
                describeDrawable(sb, cur, depth + 1);
            }
        }
    }

    private static String stateNames(int[] state) {
        final StringBuilder sb = new StringBuilder();
        for (int s : state) {
            if (s == android.R.attr.state_enabled) sb.append("enabled ");
            else if (s == android.R.attr.state_checked) sb.append("CHECKED ");
            else if (s == android.R.attr.state_pressed) sb.append("PRESSED ");
            else if (s == android.R.attr.state_focused) sb.append("focused ");
            else if (s == android.R.attr.state_selected) sb.append("selected ");
            else if (s == -android.R.attr.state_enabled) sb.append("!enabled ");
        }
        return sb.length() == 0 ? "(none)" : sb.toString().trim();
    }

    private static String hex(int color) {
        return String.format("#%08X", color);
    }
}
