package com.example.michal.asgchrono;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;

/**
 * A {@link android.preference.Preference} that displays a number picker as a dialog.
 */
public class NumberPickerPreference extends DialogPreference {

    // allowed range
    public static final int MAX_VALUE = 100;

    private SeekBar seekBar;
    private int value;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateDialogView() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        seekBar = new SeekBar(getContext());
        seekBar.setLayoutParams(layoutParams);
        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(seekBar);

        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        seekBar.setMax(MAX_VALUE);
        seekBar.setProgress(getValue());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            seekBar.clearFocus();
            int newValue = seekBar.getProgress();
            if (callChangeListener(newValue)) {
                setValue(newValue);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue)
            setValue(getPersistedInt(0));
        else
            setValue((Integer)defaultValue);
    }

    public void setValue(int value) {
        this.value = value;
        persistInt(this.value);
    }

    public int getValue() {
        return this.value;
    }
}