package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class ValueExpression implements Expression {
    final Value value;

    public ValueExpression(Value value) {
        this.value = value;
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return ValueView.wrap(value);
    }
    @Override
    public Type expectedType() {
        return value.getType();
    }
}
