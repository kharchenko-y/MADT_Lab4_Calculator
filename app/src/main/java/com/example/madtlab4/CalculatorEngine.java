package com.example.madtlab4;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Independent calculator state machine. No Android imports and no third-party math library.
 * Percent behavior follows the Standard calculator: 200 + 10% -> 200 + 20 = 220.
 */
public final class CalculatorEngine implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final MathContext PRECISION = new MathContext(16, RoundingMode.HALF_UP);
    private static final int MAX_INPUT_DIGITS = 16;
    private static final int MAX_HISTORY = 100;

    public enum Operator {
        ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷");
        private final String symbol;
        Operator(String symbol) { this.symbol = symbol; }
        public String symbol() { return symbol; }
    }

    public enum Error {
        NONE, DIVISION_BY_ZERO, NEGATIVE_SQUARE_ROOT
    }

    public static final class HistoryEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String expression;
        private final String result;
        HistoryEntry(String expression, String result) {
            this.expression = expression;
            this.result = result;
        }
        public String getExpression() { return expression; }
        public String getResult() { return result; }
    }

    private String input = "0";
    private String expression = "";
    private BigDecimal leftOperand;
    private Operator pendingOperator;
    private Operator repeatOperator;
    private BigDecimal repeatOperand;
    private BigDecimal memory;
    private boolean replaceInput;
    // A unary operation may finish calculating the current right operand while
    // still leaving it available to equalsPressed() or chooseOperator().
    // The next digit must replace that result instead of being appended to it.
    private boolean replaceTransformedOnDigit;
    private Error error = Error.NONE;
    private final ArrayList<HistoryEntry> history = new ArrayList<>();

    public String getDisplay() { return input; }
    public String getExpression() { return expression; }
    public Error getError() { return error; }
    public boolean hasMemory() { return memory != null; }
    public String getMemoryValue() { return memory == null ? "0" : format(memory); }
    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void inputDigit(char digit) {
        if (digit < '0' || digit > '9') throw new IllegalArgumentException("Not a digit");
        recoverFromError();
        prepareForNumericEntry();
        if (countDigits(input) >= MAX_INPUT_DIGITS) return;
        if (input.equals("0")) input = String.valueOf(digit);
        else if (input.equals("-0")) input = "-" + digit;
        else input += digit;
        if (pendingOperator == null) expression = "";
    }

    public void inputDecimal() {
        recoverFromError();
        prepareForNumericEntry();
        if (!input.contains(".")) input += ".";
        if (pendingOperator == null) expression = "";
    }

    public void deleteLast() {
        if (error != Error.NONE) { resetCalculation(); return; }
        if (replaceInput) {
            if (pendingOperator == null) clearRepeatOperation();
            input = "0";
            replaceInput = false;
            replaceTransformedOnDigit = false;
            return;
        }
        replaceTransformedOnDigit = false;
        if (input.length() <= 1 || (input.length() == 2 && input.startsWith("-"))) {
            input = "0";
        } else {
            input = input.substring(0, input.length() - 1);
        }
        if (pendingOperator == null) expression = "";
    }

    public void clearEntry() {
        if (error != Error.NONE) { resetCalculation(); return; }
        if (pendingOperator == null) clearRepeatOperation();
        input = "0";
        replaceInput = false;
        replaceTransformedOnDigit = false;
        if (pendingOperator == null) expression = "";
    }

    /** Clears the calculation without clearing the session's history or memory. */
    public void clearAll() {
        resetCalculation();
    }

    public void changeSign() {
        if (error != Error.NONE) return;
        // After "5 +", the displayed 5 is the *first* operand. A sign press
        // starts a new, negative second operand rather than negating that 5.
        if (pendingOperator != null && replaceInput) input = "0";
        if (input.startsWith("-")) input = input.substring(1);
        else input = "-" + input;
        replaceInput = false;
        replaceTransformedOnDigit = false;
        if (pendingOperator == null) expression = "";
    }

    public void chooseOperator(Operator operator) {
        if (operator == null) throw new IllegalArgumentException("Operator is null");
        if (error != Error.NONE) return;
        BigDecimal current = parseInput();
        if (pendingOperator != null && !replaceInput) {
            BigDecimal result = compute(leftOperand, current, pendingOperator);
            if (error != Error.NONE) return;
            leftOperand = result;
            input = format(result);
        } else if (pendingOperator == null) {
            leftOperand = current;
        }
        pendingOperator = operator;
        clearRepeatOperation();
        replaceInput = true;
        replaceTransformedOnDigit = false;
        expression = format(leftOperand) + " " + operator.symbol();
    }

    public void equalsPressed() {
        if (error != Error.NONE) return;
        BigDecimal left;
        BigDecimal right;
        Operator operation;
        if (pendingOperator != null) {
            left = leftOperand;
            right = replaceInput ? leftOperand : parseInput();
            operation = pendingOperator;
        } else if (repeatOperator != null) {
            left = parseInput();
            right = repeatOperand;
            operation = repeatOperator;
        } else {
            return;
        }
        BigDecimal result = compute(left, right, operation);
        if (error != Error.NONE) return;
        expression = format(left) + " " + operation.symbol() + " " + format(right) + " =";
        input = format(result);
        addHistory(expression, input);
        pendingOperator = null;
        leftOperand = null;
        repeatOperator = operation;
        repeatOperand = right;
        replaceInput = true;
        replaceTransformedOnDigit = false;
    }

    /** With a pending binary operator, percent is relative to its left operand. */
    public void percent() {
        if (error != Error.NONE) return;
        BigDecimal current = parseInput();
        // Standard mode: A + B% treats B as a percentage of A,
        // while A × B% treats B as B / 100.
        BigDecimal base = pendingOperator == Operator.ADD || pendingOperator == Operator.SUBTRACT
                ? leftOperand : BigDecimal.ONE;
        BigDecimal result = base.multiply(current, PRECISION)
                .divide(BigDecimal.valueOf(100), PRECISION);
        String unaryExpression = format(current) + "% =";
        input = format(result);
        replaceInput = pendingOperator == null;
        replaceTransformedOnDigit = pendingOperator != null;
        if (pendingOperator == null) {
            expression = unaryExpression;
            addHistory(expression, input);
        } else {
            expression = format(leftOperand) + " " + pendingOperator.symbol() + " "
                    + format(current) + "%";
        }
    }

    public void square() {
        if (error != Error.NONE) return;
        BigDecimal n = parseInput();
        finishUnary("sqr(" + format(n) + ") =", n.multiply(n, PRECISION));
    }

    public void squareRoot() {
        if (error != Error.NONE) return;
        BigDecimal n = parseInput();
        if (n.signum() < 0) {
            setError(Error.NEGATIVE_SQUARE_ROOT);
            return;
        }
        // Math.sqrt is part of the Java standard library, not an external math package.
        BigDecimal result = BigDecimal.valueOf(Math.sqrt(n.doubleValue())).round(PRECISION);
        finishUnary("√(" + format(n) + ") =", result);
    }

    /** 'Fraction calculation' in the provided Windows Standard design is 1/x. */
    public void reciprocal() {
        if (error != Error.NONE) return;
        BigDecimal n = parseInput();
        if (n.signum() == 0) {
            setError(Error.DIVISION_BY_ZERO);
            return;
        }
        finishUnary("1/(" + format(n) + ") =", BigDecimal.ONE.divide(n, PRECISION));
    }

    private void finishUnary(String text, BigDecimal result) {
        input = format(result);
        replaceInput = pendingOperator == null;
        replaceTransformedOnDigit = pendingOperator != null;
        addHistory(text, input);
        expression = pendingOperator == null
                ? text : format(leftOperand) + " " + pendingOperator.symbol();
    }

    public void memoryStore() {
        if (error == Error.NONE) memory = parseInput();
    }
    public void memoryRecall() {
        if (memory != null && error == Error.NONE) {
            input = format(memory);
            replaceInput = false;
            replaceTransformedOnDigit = false;
            if (pendingOperator == null) expression = "";
        }
    }
    public void memoryClear() { memory = null; }
    public void memoryAdd() {
        if (error == Error.NONE) {
            memory = (memory == null ? BigDecimal.ZERO : memory)
                    .add(parseInput(), PRECISION);
        }
    }
    public void memorySubtract() {
        if (error == Error.NONE) {
            memory = (memory == null ? BigDecimal.ZERO : memory)
                    .subtract(parseInput(), PRECISION);
        }
    }

    public void recallHistory(int index) {
        HistoryEntry item = history.get(index);
        resetCalculation();
        input = item.getResult();
        replaceInput = true;
        replaceTransformedOnDigit = false;
    }
    public void clearHistory() { history.clear(); }

    private BigDecimal compute(BigDecimal a, BigDecimal b, Operator operator) {
        switch (operator) {
            case ADD: return a.add(b, PRECISION);
            case SUBTRACT: return a.subtract(b, PRECISION);
            case MULTIPLY: return a.multiply(b, PRECISION);
            case DIVIDE:
                if (b.signum() == 0) {
                    setError(Error.DIVISION_BY_ZERO);
                    return BigDecimal.ZERO;
                }
                return a.divide(b, PRECISION);
            default: throw new IllegalStateException("Unsupported operator");
        }
    }

    private void prepareForNumericEntry() {
        if (!replaceInput && !replaceTransformedOnDigit) return;
        // Typing a new number after '=' must not retain the previous operator
        // for another equals press. Consecutive '=' presses remain repeatable.
        if (pendingOperator == null) clearRepeatOperation();
        input = "0";
        replaceInput = false;
        replaceTransformedOnDigit = false;
    }

    private void clearRepeatOperation() {
        repeatOperator = null;
        repeatOperand = null;
    }

    private BigDecimal parseInput() {
        String value = input.endsWith(".") ? input.substring(0, input.length() - 1) : input;
        return new BigDecimal(value);
    }
    private static int countDigits(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) >= '0' && text.charAt(i) <= '9') total++;
        }
        return total;
    }
    private static String format(BigDecimal n) {
        if (n.signum() == 0) return "0";
        String output = n.stripTrailingZeros().toPlainString();
        if (output.length() > 25) output = n.round(PRECISION).stripTrailingZeros().toEngineeringString();
        return output;
    }
    private void addHistory(String operation, String result) {
        history.add(0, new HistoryEntry(operation, result));
        if (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
    }
    private void setError(Error type) {
        error = type;
        pendingOperator = null;
        leftOperand = null;
        clearRepeatOperation();
        replaceInput = true;
        replaceTransformedOnDigit = false;
        expression = "";
    }
    private void recoverFromError() {
        if (error != Error.NONE) resetCalculation();
    }
    private void resetCalculation() {
        error = Error.NONE;
        input = "0";
        expression = "";
        pendingOperator = null;
        leftOperand = null;
        clearRepeatOperation();
        replaceInput = false;
        replaceTransformedOnDigit = false;
    }
}
