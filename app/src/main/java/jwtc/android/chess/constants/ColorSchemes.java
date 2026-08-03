package jwtc.android.chess.constants;

import androidx.core.graphics.ColorUtils;

import jwtc.android.chess.R;

public class ColorSchemes {
    /**
     * Per scheme: [0] dark square, [1] light square, [2] selected square,
     * [3] last-move / check highlight (drawn over the square, so translucent
     * for the colour schemes), [4] coordinate label background,
     * [5] coordinate label text.
     */
    private static int[][] colorScheme = new int[10][6];

    /**
     * Index of the greyscale scheme used by e-ink mode. Must stay last: the
     * saved "colorscheme" preference is a positional index, so schemes may
     * only be appended, never inserted.
     */
    public static final int EINK = 9;

    private static final int DEFAULT_HIGHLIGHT = 0x66ffff00;
    private static final int DEFAULT_COORD_BACKGROUND = 0x99ffffff;
    private static final int DEFAULT_COORD_TEXT = 0x99000000;

    public static int selectedColorScheme = 0;
    public static boolean showCoords = false;
    public static boolean isRotated = false; // not ideal
    public static int selectedPattern = 0;
    public static float saturationFactor = 1.0f;

    static {
        colorScheme[0][0] = 0xeeFAAE2F;
        colorScheme[0][1] = 0xeeFFCC78;
        colorScheme[0][2] = 0xffFFE1B0;
        colorScheme[0][3] = 0xffFFE1B0;

        colorScheme[1][0] = 0xee629EFC;
        colorScheme[1][1] = 0xee93BBFA;
        colorScheme[1][2] = 0xffCDEDF7;
        colorScheme[1][3] = 0xffCDEDF7;

        colorScheme[2][0] = 0xee488C1D;
        colorScheme[2][1] = 0xee71B04A;
        colorScheme[2][2] = 0xffB0E092;
        colorScheme[2][3] = 0xffB0E092;

        colorScheme[3][0] = 0xee444444;
        colorScheme[3][1] = 0xee777777;
        colorScheme[3][2] = 0xffCCCCCC;
        colorScheme[3][3] = 0xffCCCCCC;

        colorScheme[4][0] = 0xeeAD5F2F;
        colorScheme[4][1] = 0xeeBD8562;
        colorScheme[4][2] = 0xffFFC5A1;
        colorScheme[4][3] = 0xffFFC5A1;

        colorScheme[5][0] = 0xeeEB573D;
        colorScheme[5][1] = 0xeeF77159;
        colorScheme[5][2] = 0xffFFAB9C;
        colorScheme[5][3] = 0xffFFAB9C;

        colorScheme[6][0] = 0xeeFC9432;
        colorScheme[6][1] = 0xeeFCB26D;
        colorScheme[6][2] = 0xffFFC894;
        colorScheme[6][3] = 0xffFFC894;

        colorScheme[7][0] = 0xeeF55FE3;
        colorScheme[7][1] = 0xeeFA7FEB;
        colorScheme[7][2] = 0xffFFB3F6;
        colorScheme[7][3] = 0xffFFB3F6;

        colorScheme[8][0] = 0xee805ad5;
        colorScheme[8][1] = 0xeeac8eed;
        colorScheme[8][2] = 0xffFCD2F7;
        colorScheme[8][3] = 0xffFCD2F7;

        // Greyscale scheme for e-ink displays.
        //
        // Squares are opaque (the colour schemes above are 0xee, which washes
        // out on a reflective panel) and sit at levels an e-ink controller can
        // hold cleanly. The dark square is deliberately mid-grey rather than
        // near-black: Alpha black pieces are solid #101010 with no light
        // outline, so they would disappear against a black square.
        // Kept in step with @color/einkBoardDark, which tints switch tracks and
        // selected toggles so "active" reads the same everywhere.
        colorScheme[EINK][0] = 0xff9e9e9e; // dark square
        colorScheme[EINK][1] = 0xffffffff; // light square
        colorScheme[EINK][2] = 0xff4d4d4d; // selected square
        colorScheme[EINK][3] = 0x40000000; // last-move wash, darkens either square
        colorScheme[EINK][4] = 0xffffffff; // coordinate background, opaque
        colorScheme[EINK][5] = 0xff000000; // coordinate text, opaque

        // The nine colour schemes all shared one hardcoded highlight and one
        // hardcoded pair of coordinate colours; keep those values so their
        // appearance is unchanged.
        for (int i = 0; i < EINK; i++) {
            colorScheme[i][3] = DEFAULT_HIGHLIGHT;
            colorScheme[i][4] = DEFAULT_COORD_BACKGROUND;
            colorScheme[i][5] = DEFAULT_COORD_TEXT;
        }
    }

    public static int getLight() {
        return desaturateColor(colorScheme[selectedColorScheme][1], ColorSchemes.saturationFactor);
    }

    public static int getDark() {
        return desaturateColor(colorScheme[selectedColorScheme][0], ColorSchemes.saturationFactor);
    }

    public static int getHightlightColor() {
        return colorScheme[selectedColorScheme][3];
    }

    public static int getCoordBackgroundColor() {
        return colorScheme[selectedColorScheme][4];
    }

    public static int getCoordTextColor() {
        return colorScheme[selectedColorScheme][5];
    }

    public static int getSelectedColor() {
        return colorScheme[selectedColorScheme][2];
    }

    public static int getSelectedPatternDrawable() {
        switch (selectedPattern) {
            case 1:
                return R.drawable.square_single_shade;
            case 2:
                return R.drawable.square_double_shade;
            case 3:
                return R.drawable.diagonal_stripes;
        }

        return 0;
    }

    public static int desaturateColor(int color, float factor) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);

        // Reduce saturation
        hsl[1] = hsl[1] * factor;

        return ColorUtils.HSLToColor(hsl);
    }
}
