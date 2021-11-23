package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class ValueExpression implements Expression {
    final Value value;
    final boolean isConst;

    public ValueExpression(Value value, boolean isConst) {
        this.value = value;
        this.isConst = isConst;
    }

    @Override
    public boolean isBound() {
        return isConst&&value.isMutable();
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
}
