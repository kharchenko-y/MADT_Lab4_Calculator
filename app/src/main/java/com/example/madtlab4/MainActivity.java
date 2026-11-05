package com.example.madtlab4;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/** Thin Android UI layer: all arithmetic and history state stay in CalculatorEngine. */
public final class MainActivity extends Activity {
    private static final String STATE_ENGINE = "calculator_engine";

    private CalculatorEngine engine;
    private TextView resultText;
    private TextView expressionText;
    private TextView memoryIndicator;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        engine = state == null ? null : (CalculatorEngine) state.getSerializable(STATE_ENGINE);
        if (engine == null) engine = new CalculatorEngine();
        resultText = findViewById(R.id.result_text);
        expressionText = findViewById(R.id.expression_text);
        memoryIndicator = findViewById(R.id.memory_indicator);
        applySystemBarInsets();
        wireButtons();
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_ENGINE, engine);
        super.onSaveInstanceState(outState);
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.calculator_root);
        final int startLeft = root.getPaddingLeft();
        final int startTop = root.getPaddingTop();
        final int startRight = root.getPaddingRight();
        final int startBottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(startLeft + left, startTop + top,
                    startRight + right, startBottom + bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void wireButtons() {
        wireDigit(R.id.key_0, '0'); wireDigit(R.id.key_1, '1');
        wireDigit(R.id.key_2, '2'); wireDigit(R.id.key_3, '3');
        wireDigit(R.id.key_4, '4'); wireDigit(R.id.key_5, '5');
        wireDigit(R.id.key_6, '6'); wireDigit(R.id.key_7, '7');
        wireDigit(R.id.key_8, '8'); wireDigit(R.id.key_9, '9');
        bind(R.id.key_decimal, () -> engine.inputDecimal());
        bind(R.id.key_sign, () -> engine.changeSign());
        bind(R.id.key_backspace, () -> engine.deleteLast());
        bind(R.id.key_ce, () -> engine.clearEntry());
        bind(R.id.key_c, () -> engine.clearAll());
        bind(R.id.key_plus, () -> engine.chooseOperator(CalculatorEngine.Operator.ADD));
        bind(R.id.key_minus, () -> engine.chooseOperator(CalculatorEngine.Operator.SUBTRACT));
        bind(R.id.key_multiply, () -> engine.chooseOperator(CalculatorEngine.Operator.MULTIPLY));
        bind(R.id.key_divide, () -> engine.chooseOperator(CalculatorEngine.Operator.DIVIDE));
        bind(R.id.key_equals, () -> engine.equalsPressed());
        bind(R.id.key_percent, () -> engine.percent());
        bind(R.id.key_square, () -> engine.square());
        bind(R.id.key_square_root, () -> engine.squareRoot());
        bind(R.id.key_reciprocal, () -> engine.reciprocal());
        bind(R.id.key_mc, () -> engine.memoryClear());
        bind(R.id.key_mr, () -> engine.memoryRecall());
        bind(R.id.key_mp, () -> engine.memoryAdd());
        bind(R.id.key_mm, () -> engine.memorySubtract());
        bind(R.id.key_ms, () -> engine.memoryStore());
        findViewById(R.id.key_mv).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.memory_title)
                        .setMessage(engine.hasMemory()
                                ? getString(R.string.memory_value_format, engine.getMemoryValue())
                                : getString(R.string.memory_empty))
                        .setPositiveButton(R.string.dialog_close, null)
                        .show());
        findViewById(R.id.history_button).setOnClickListener(view -> showHistory());
        findViewById(R.id.menu_button).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.mode_standard)
                        .setMessage(R.string.about_calculator)
                        .setPositiveButton(R.string.dialog_close, null)
                        .show());
    }

    private void wireDigit(int id, char digit) {
        bind(id, () -> engine.inputDigit(digit));
    }

    private void bind(int id, Runnable action) {
        findViewById(id).setOnClickListener(view -> {
            action.run();
            refresh();
        });
    }

    private void refresh() {
        switch (engine.getError()) {
            case DIVISION_BY_ZERO: resultText.setText(R.string.error_division_by_zero); break;
            case NEGATIVE_SQUARE_ROOT: resultText.setText(R.string.error_negative_sqrt); break;
            default: resultText.setText(engine.getDisplay());
        }
        expressionText.setText(engine.getExpression());
        memoryIndicator.setText(engine.hasMemory() ? R.string.memory_indicator : R.string.empty_string);
        ((Button) findViewById(R.id.key_mc)).setEnabled(engine.hasMemory());
        ((Button) findViewById(R.id.key_mr)).setEnabled(engine.hasMemory());
        ((Button) findViewById(R.id.key_mv)).setEnabled(engine.hasMemory());
        int length = engine.getError() == CalculatorEngine.Error.NONE
                ? engine.getDisplay().length() : resultText.getText().length();
        float size = length <= 11 ? 48 : length <= 16 ? 36 : length <= 22 ? 27 : 22;
        resultText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    private void showHistory() {
        List<CalculatorEngine.HistoryEntry> items = engine.getHistory();
        if (items.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.history_title)
                    .setMessage(R.string.history_empty)
                    .setPositiveButton(R.string.dialog_close, null)
                    .show();
            return;
        }
        String[] choices = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            CalculatorEngine.HistoryEntry record = items.get(i);
            choices[i] = getString(R.string.history_item_format,
                    record.getExpression(), record.getResult());
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_title)
                .setItems(choices, (dialog, selected) -> {
                    engine.recallHistory(selected);
                    refresh();
                })
                .setNeutralButton(R.string.clear_history, (dialog, which) -> {
                    engine.clearHistory();
                    refresh();
                })
                .setPositiveButton(R.string.dialog_close, null)
                .show();
    }
}
