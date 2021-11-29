package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class StringCompare implements Expression {

    //TODO implement StringCompare
    public static Expression create(Expression left, OperatorType op, Expression right) {
        throw new UnsupportedOperationException("unimplemented");
    }

    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return null;
    }

    @Override
    public Type expectedType() {
        return null;
    }

    @Override
    public boolean isMutable() {
        return Expression.super.isMutable();
    }
}
