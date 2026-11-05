package com.example.madtlab4;

import static org.junit.Assert.*;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class CalculatorEngineTest {
    private static void digits(CalculatorEngine engine, String text) {
        for (char ch : text.toCharArray()) {
            if (ch == '.') engine.inputDecimal();
            else engine.inputDigit(ch);
        }
    }
    private static CalculatorEngine calculate(String a, CalculatorEngine.Operator operator, String b) {
        CalculatorEngine engine = new CalculatorEngine();
        digits(engine, a);
        engine.chooseOperator(operator);
        digits(engine, b);
        engine.equalsPressed();
        return engine;
    }

    @Test public void initialZero() { assertEquals("0", new CalculatorEngine().getDisplay()); }
    @Test public void enteringDigits() {
        CalculatorEngine e = new CalculatorEngine(); digits(e,"123"); assertEquals("123",e.getDisplay());
    }
    @Test public void leadingZerosNotAccumulated() {
        CalculatorEngine e = new CalculatorEngine(); digits(e,"0007"); assertEquals("7",e.getDisplay());
    }
    @Test public void decimalPointAcceptedOnce() {
        CalculatorEngine e = new CalculatorEngine(); digits(e,"0.5"); e.inputDecimal();
        assertEquals("0.5",e.getDisplay());
    }
    @Test public void decimalAddNoBinaryFloatingError() {
        assertEquals("0.3",calculate("0.1",CalculatorEngine.Operator.ADD,"0.2").getDisplay());
    }
    @Test public void addition() {
        assertEquals("20",calculate("12",CalculatorEngine.Operator.ADD,"8").getDisplay());
    }
    @Test public void subtractionIntoNegative() {
        assertEquals("-5",calculate("7",CalculatorEngine.Operator.SUBTRACT,"12").getDisplay());
    }
    @Test public void multiplication() {
        assertEquals("42",calculate("6",CalculatorEngine.Operator.MULTIPLY,"7").getDisplay());
    }
    @Test public void division() {
        assertEquals("0.125",calculate("1",CalculatorEngine.Operator.DIVIDE,"8").getDisplay());
    }
    @Test public void repeatingDecimalRounded() {
        String result = calculate("1",CalculatorEngine.Operator.DIVIDE,"3").getDisplay();
        assertTrue(result.startsWith("0.333333"));
    }
    @Test public void divisionByZeroShowsError() {
        CalculatorEngine e=calculate("8",CalculatorEngine.Operator.DIVIDE,"0");
        assertEquals(CalculatorEngine.Error.DIVISION_BY_ZERO,e.getError());
    }
    @Test public void zeroOverZeroShowsError() {
        CalculatorEngine e=calculate("0",CalculatorEngine.Operator.DIVIDE,"0");
        assertEquals(CalculatorEngine.Error.DIVISION_BY_ZERO,e.getError());
    }
    @Test public void digitRecoversAfterDivisionError() {
        CalculatorEngine e=calculate("8",CalculatorEngine.Operator.DIVIDE,"0");
        e.inputDigit('9'); assertEquals(CalculatorEngine.Error.NONE,e.getError());
        assertEquals("9",e.getDisplay());
    }
    @Test public void emptyEqualsIsNoOp() {
        CalculatorEngine e=new CalculatorEngine(); e.equalsPressed();
        assertEquals("0",e.getDisplay()); assertTrue(e.getHistory().isEmpty());
    }
    @Test public void repeatEquals() {
        CalculatorEngine e=calculate("2",CalculatorEngine.Operator.ADD,"3");
        e.equalsPressed(); assertEquals("8",e.getDisplay());
        assertEquals(2,e.getHistory().size());
    }
    @Test public void immediateEqualsRepeatsLeftOperand() {
        CalculatorEngine e=new CalculatorEngine(); e.inputDigit('3');
        e.chooseOperator(CalculatorEngine.Operator.ADD); e.equalsPressed();
        assertEquals("6",e.getDisplay());
    }
    @Test public void operatorsAreLeftAssociative() {
        CalculatorEngine e = new CalculatorEngine(); digits(e,"2");
        e.chooseOperator(CalculatorEngine.Operator.ADD); digits(e,"3");
        e.chooseOperator(CalculatorEngine.Operator.MULTIPLY); digits(e,"4"); e.equalsPressed();
        assertEquals("20",e.getDisplay());
    }
    @Test public void changingPendingOperator() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"8");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        e.chooseOperator(CalculatorEngine.Operator.MULTIPLY);
        digits(e,"4");e.equalsPressed();assertEquals("32",e.getDisplay());
    }
    @Test public void signToggle() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"12");e.changeSign();
        assertEquals("-12",e.getDisplay());e.changeSign();assertEquals("12",e.getDisplay());
    }
    @Test public void negativeZeroCanBeEntered() {
        CalculatorEngine e=new CalculatorEngine();e.changeSign();digits(e,"2");
        assertEquals("-2",e.getDisplay());
    }
    @Test public void backspaceRemovesLastDigit() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"123");e.deleteLast();
        assertEquals("12",e.getDisplay());
    }
    @Test public void backspaceAtSingleDigitReturnsZero() {
        CalculatorEngine e=new CalculatorEngine();e.inputDigit('7');e.deleteLast();
        assertEquals("0",e.getDisplay());
    }
    @Test public void backspaceHandlesNegativeNumbers() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"12");e.changeSign();
        e.deleteLast();assertEquals("-1",e.getDisplay());
        e.deleteLast();assertEquals("0",e.getDisplay());
    }
    @Test public void clearEntryPreservesPendingOperation() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"8");e.clearEntry();digits(e,"2");e.equalsPressed();
        assertEquals("7",e.getDisplay());
    }
    @Test public void clearAllResetsPendingOperation() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);e.clearAll();
        digits(e,"9");e.equalsPressed();assertEquals("9",e.getDisplay());
    }
    @Test public void square() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"12");e.square();
        assertEquals("144",e.getDisplay());
    }
    @Test public void newDigitAfterStandaloneUnaryStartsNewInput() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"5");e.square();
        e.inputDigit('7');assertEquals("7",e.getDisplay());
    }
    @Test public void newDigitAfterStandalonePercentStartsNewInput() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"25");e.percent();
        e.inputDigit('8');assertEquals("8",e.getDisplay());
    }
    @Test public void squareRoot() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"81");e.squareRoot();
        assertEquals("9",e.getDisplay());
    }
    @Test public void negativeSquareRootIsError() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"4");e.changeSign();e.squareRoot();
        assertEquals(CalculatorEngine.Error.NEGATIVE_SQUARE_ROOT,e.getError());
    }
    @Test public void squareRootZero() {
        CalculatorEngine e=new CalculatorEngine();e.squareRoot();assertEquals("0",e.getDisplay());
    }
    @Test public void reciprocal() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"8");e.reciprocal();
        assertEquals("0.125",e.getDisplay());
    }
    @Test public void reciprocalZeroShowsError() {
        CalculatorEngine e=new CalculatorEngine();e.reciprocal();
        assertEquals(CalculatorEngine.Error.DIVISION_BY_ZERO,e.getError());
    }
    @Test public void simplePercent() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"25");e.percent();
        assertEquals("0.25",e.getDisplay());
    }
    @Test public void percentRelativeToLeft() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"200");
        e.chooseOperator(CalculatorEngine.Operator.ADD);digits(e,"10");e.percent();e.equalsPressed();
        assertEquals("220",e.getDisplay());
    }
    @Test public void percentageInMultiplication() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"200");
        e.chooseOperator(CalculatorEngine.Operator.MULTIPLY);digits(e,"10");e.percent();e.equalsPressed();
        assertEquals("20",e.getDisplay());
    }
    @Test public void unaryWithinPendingBinary() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);digits(e,"9");
        e.squareRoot();e.equalsPressed();assertEquals("8",e.getDisplay());
    }
    @Test public void historyNewestFirst() {
        CalculatorEngine e=calculate("12",CalculatorEngine.Operator.ADD,"8");
        e.square();assertEquals(2,e.getHistory().size());
        assertEquals("sqr(20) =",e.getHistory().get(0).getExpression());
        assertEquals("20",e.getHistory().get(1).getResult());
    }
    @Test public void historyPreservedByClearAll() {
        CalculatorEngine e=calculate("12",CalculatorEngine.Operator.ADD,"8");
        e.clearAll();assertEquals(1,e.getHistory().size());
    }
    @Test public void historyRecall() {
        CalculatorEngine e=calculate("12",CalculatorEngine.Operator.ADD,"8");
        e.clearAll();e.recallHistory(0);assertEquals("20",e.getDisplay());
    }
    @Test public void historyClearsOnDemand() {
        CalculatorEngine e=calculate("12",CalculatorEngine.Operator.ADD,"8");
        e.clearHistory();assertTrue(e.getHistory().isEmpty());
    }
    @Test public void memoryStoreRecall() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"12");e.memoryStore();e.clearAll();
        assertTrue(e.hasMemory());e.memoryRecall();assertEquals("12",e.getDisplay());
    }
    @Test public void memoryAddsAndSubtracts() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"12");e.memoryStore();
        e.clearAll();digits(e,"8");e.memoryAdd();
        e.clearAll();digits(e,"3");e.memorySubtract();
        e.clearAll();e.memoryRecall();assertEquals("17",e.getDisplay());
        e.memoryClear();assertFalse(e.hasMemory());
    }
    @Test public void memoryPlusOnEmptyMemory() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"5");e.memoryAdd();
        e.clearAll();e.memoryRecall();assertEquals("5",e.getDisplay());
    }
    @Test public void memoryMinusOnEmptyMemory() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"5");e.memorySubtract();
        e.clearAll();e.memoryRecall();assertEquals("-5",e.getDisplay());
    }
    @Test public void maxInputDigits() {
        CalculatorEngine e=new CalculatorEngine();digits(e,"12345678901234567890");
        assertEquals("1234567890123456",e.getDisplay());
    }
    @Test public void serializationPreservesStateAndHistory() throws Exception {
        CalculatorEngine before=calculate("12",CalculatorEngine.Operator.ADD,"8");
        before.memoryStore();
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(ObjectOutputStream out=new ObjectOutputStream(buffer)){out.writeObject(before);}
        CalculatorEngine after;
        try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            after=(CalculatorEngine)in.readObject();
        }
        assertEquals("20",after.getDisplay());
        assertEquals(1,after.getHistory().size());
        assertTrue(after.hasMemory());
    }

    // Code Review regression tests (two corrected state-transition defects).
    @Test public void signAfterOperatorAppliesToSecondOperand() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        e.changeSign(); digits(e,"2"); e.equalsPressed();
        assertEquals("3",e.getDisplay());
    }
    @Test public void signAfterOperatorWorksWithDecimal() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        e.changeSign(); digits(e,"0.2"); e.equalsPressed();
        assertEquals("4.8",e.getDisplay());
    }
    @Test public void twiceChangingSignAfterOperatorReturnsPositiveOperand() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        e.changeSign(); e.changeSign(); digits(e,"2"); e.equalsPressed();
        assertEquals("7",e.getDisplay());
    }
    @Test public void rightOperandReplacementAfterSquareRoot() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"9"); e.squareRoot(); digits(e,"2"); e.equalsPressed();
        assertEquals("7",e.getDisplay());
    }
    @Test public void rightOperandReplacementAfterSquare() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"9"); e.square(); digits(e,"2"); e.equalsPressed();
        assertEquals("7",e.getDisplay());
    }
    @Test public void rightOperandReplacementAfterPercent() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"200");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"10"); e.percent(); digits(e,"2"); e.equalsPressed();
        assertEquals("202",e.getDisplay());
    }
    @Test public void decimalStartsNewRightOperandAfterUnary() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"9"); e.squareRoot(); digits(e,"0.5"); e.equalsPressed();
        assertEquals("5.5",e.getDisplay());
    }
    @Test public void transformedOperandIsStillUsableByEquals() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"9"); e.squareRoot(); e.equalsPressed();
        assertEquals("8",e.getDisplay());
    }
    @Test public void transformedOperandIsStillUsableByNextOperator() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"9"); e.squareRoot();
        e.chooseOperator(CalculatorEngine.Operator.MULTIPLY);
        digits(e,"2"); e.equalsPressed();
        assertEquals("16",e.getDisplay());
    }
    @Test public void newNumberAfterEqualsDoesNotRepeatPriorOperation() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"3"); e.equalsPressed(); digits(e,"4"); e.equalsPressed();
        assertEquals("4",e.getDisplay());
        assertEquals(1,e.getHistory().size());
    }
    @Test public void startingWithDecimalAfterEqualsDoesNotRepeatOperation() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"3"); e.equalsPressed(); digits(e,"0.5"); e.equalsPressed();
        assertEquals("0.5",e.getDisplay());
    }
    @Test public void pressingEqualsTwiceStillRepeatsOperation() {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"3"); e.equalsPressed(); e.equalsPressed();
        assertEquals("11",e.getDisplay());
    }
    @Test public void serializationAfterUnaryRetainsNextDigitReplacement() throws Exception {
        CalculatorEngine e=new CalculatorEngine(); digits(e,"5");
        e.chooseOperator(CalculatorEngine.Operator.ADD);
        digits(e,"9"); e.squareRoot();
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(ObjectOutputStream out=new ObjectOutputStream(buffer)){out.writeObject(e);}
        try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))){
            CalculatorEngine restored=(CalculatorEngine)in.readObject();
            digits(restored,"2"); restored.equalsPressed();
            assertEquals("7",restored.getDisplay());
        }
    }
}
