package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class ValueExpression implements Expression {
    final Value value;

    public static Expression create(Value value){
        return new ValueExpression(value);
    }

    private ValueExpression(Value value) {
        this.value = value;
    }
    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return ValueView.wrap(value);
    }
    @Override
    public Type expectedType() {
        return value.getType();
    }

    @Override
    public String toString() {
        return "ValueExpression{"+value+"}";
    }

    public Value getValue() {
        return value;
    }
    @Override
    public boolean hasValue(ProgramContext context) {
        return true;
    }
    @Override
    public Value getValue(ProgramContext context) {
        return value;
    }
}
