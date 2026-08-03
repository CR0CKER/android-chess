package jwtc.android.chess.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.material.slider.Slider;

import jwtc.android.chess.R;
import jwtc.android.chess.constants.ColorSchemes;
import jwtc.android.chess.constants.PieceSets;
import jwtc.android.chess.helpers.ActivityHelper;
import jwtc.android.chess.helpers.EinkMode;
import jwtc.android.chess.services.GameApi;
import jwtc.android.chess.views.FixedDropdownView;

public class BoardPreferencesActivity extends ChessBoardActivity {
    private static final String TAG = "BoardPreferences";
    private CheckBox checkBoxCoordinates, checkBoxShowMoves, checkBoxShowCapturedPieces, checkBoxWakeLock, checkBoxFullscreen, checkBoxSound, checkBoxHapticFeedback, checkBoxNightMode, checkBoxEinkMode;
    private Slider sliderSaturation;
    private FixedDropdownView dropDownPieces, dropDownColorScheme, dropDownTileSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chessboard_prefs);

        ActivityHelper.fixPaddings(this, findViewById(R.id.LayoutMain));

        dropDownPieces = findViewById(R.id.DropdownPieceSet);
        dropDownColorScheme = findViewById(R.id.DropdownColorScheme);
        dropDownTileSet = findViewById(R.id.DropdownTileSet);
        checkBoxCoordinates = findViewById(R.id.CheckBoxCoordinates);
        checkBoxShowMoves = findViewById(R.id.CheckBoxShowMoves);
        checkBoxShowCapturedPieces = findViewById(R.id.CheckBoxShowCapturedPieces);
        checkBoxWakeLock = findViewById(R.id.CheckBoxUseWakeLock);
        checkBoxFullscreen = findViewById(R.id.CheckBoxFullscreen);
        checkBoxSound = findViewById(R.id.CheckBoxUseSound);
        checkBoxHapticFeedback = findViewById(R.id.CheckBoxUseHapticFeedback);
        checkBoxNightMode = findViewById(R.id.CheckBoxForceNightMode);
        checkBoxEinkMode = findViewById(R.id.CheckBoxEinkMode);
        sliderSaturation = findViewById(R.id.SliderSaturation);

        dropDownPieces.setItems(getResources().getStringArray(R.array.piecesetarray));
        dropDownPieces.setOnItemClickListener((parent, view, position, id) -> {
            PieceSets.selectedSet = position;
            EinkMode.applyBoardAppearance();
            rebuildBoard();
        });

        dropDownColorScheme.setItems(getResources().getStringArray(R.array.colorschemes));
        dropDownColorScheme.setOnItemClickListener((parent, view, position, id) -> {
            ColorSchemes.selectedColorScheme = position;
            EinkMode.applyBoardAppearance();
            chessBoardView.invalidateSquares();
        });

        dropDownTileSet.setItems(getResources().getStringArray(R.array.tileArray));
        dropDownTileSet.setOnItemClickListener((parent, view, position, id) -> {
            ColorSchemes.selectedPattern = position;
            EinkMode.applyBoardAppearance();
            chessBoardView.invalidateSquares();
        });

        checkBoxCoordinates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ColorSchemes.showCoords = isChecked;
            chessBoardView.invalidateSquares();
        });

        checkBoxEinkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed() || isChecked == EinkMode.isEnabled()) {
                // Fired by setChecked while restoring state, not by the user.
                return;
            }
            EinkMode.setEnabled(isChecked);
            // Persist before recreating: the new instance reads the preference in
            // onCreate to pick its theme, and this activity's onPause would
            // otherwise be the only thing that writes it.
            getPrefs().edit().putBoolean(EinkMode.PREF_KEY, isChecked).commit();
            // The theme decides the button, switch and pane styling and is chosen
            // in onCreate, so this screen has to come back up to restyle itself.
            recreate();
        });

        sliderSaturation.addOnChangeListener((s, value, fromUser) -> {
            ColorSchemes.saturationFactor = value;
            EinkMode.applyBoardAppearance();
            chessBoardView.invalidateSquares();
        });

        gameApi = new GameApi();

        afterCreate();
        View boardAreaLayout = findViewById(R.id.board_area);
        if (boardAreaLayout == null) {
            boardAreaLayout = findViewById(R.id.includeboard);
        }
        initBoardLayoutSizing(findViewById(R.id.LayoutMain), boardAreaLayout, findViewById(R.id.play_controls), null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getPrefs();

        jni.newGame();

        checkBoxCoordinates.setChecked(prefs.getBoolean("showCoords", false));
        // Must match ChessBoardActivity, which reads this with a default of false.
        // With the two disagreeing, merely opening this screen showed the box
        // ticked and wrote showMoves=true back on the way out, silently turning
        // the move dots on for someone who never touched the setting.
        checkBoxShowMoves.setChecked(prefs.getBoolean("showMoves", false));
        checkBoxShowCapturedPieces.setChecked(prefs.getBoolean("showCapturedPieces", true));
        checkBoxWakeLock.setChecked(prefs.getBoolean("wakeLock", false));
        checkBoxFullscreen.setChecked(prefs.getBoolean("fullScreen", false));
        checkBoxSound.setChecked(prefs.getBoolean("moveSounds", false));
        checkBoxHapticFeedback.setChecked(prefs.getBoolean("useHapticFeedback", false));
        checkBoxNightMode.setChecked(prefs.getBoolean("nightMode", false));
        checkBoxEinkMode.setChecked(EinkMode.isEnabled());

        // Show the user's own choices even while e-ink mode overrides them, so
        // they are still there to come back to.
        dropDownPieces.setSelection(Integer.parseInt(prefs.getString("pieceset", "0")));
        dropDownColorScheme.setSelection(Integer.parseInt(prefs.getString("colorscheme", "0")));
        dropDownTileSet.setSelection(Integer.parseInt(prefs.getString("squarePattern", "0")));

        sliderSaturation.setValue(prefs.getFloat("squareSaturation", 1.0f));

        setAppearanceControlsEnabled(!EinkMode.isEnabled());

        rebuildBoard();
    }

    private void setAppearanceControlsEnabled(boolean enabled) {
        dropDownPieces.setEnabled(enabled);
        dropDownColorScheme.setEnabled(enabled);
        dropDownTileSet.setEnabled(enabled);
        sliderSaturation.setEnabled(enabled);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = this.getPrefs().edit();

        Log.d(TAG, "onPause " + dropDownPieces.getSelectedItemPosition());

        editor.putString("pieceset", "" + dropDownPieces.getSelectedItemPosition());
        editor.putString("colorscheme", "" + dropDownColorScheme.getSelectedItemPosition());
        editor.putString("squarePattern", "" + dropDownTileSet.getSelectedItemPosition());
        editor.putBoolean("showCoords", checkBoxCoordinates.isChecked());
        editor.putBoolean("showMoves", checkBoxShowMoves.isChecked());
        editor.putBoolean("showCapturedPieces", checkBoxShowCapturedPieces.isChecked());
        editor.putBoolean("wakeLock", checkBoxWakeLock.isChecked());
        editor.putBoolean("fullScreen", checkBoxFullscreen.isChecked());
        editor.putBoolean("moveSounds", checkBoxSound.isChecked());
        editor.putBoolean("useHapticFeedback", checkBoxHapticFeedback.isChecked());
        editor.putBoolean("nightMode", checkBoxNightMode.isChecked());
        editor.putBoolean(EinkMode.PREF_KEY, checkBoxEinkMode.isChecked());
        editor.putFloat("squareSaturation", sliderSaturation.getValue());

        editor.commit();
    }

    @Override
    public boolean requestMove(int from, int to) {
        return false;
    }

}
