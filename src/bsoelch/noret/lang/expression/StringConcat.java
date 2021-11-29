package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class StringConcat implements Expression {

    //TODO implement StringConcat
    public static Expression appendEnd(Expression str, Expression val) {
        throw new UnsupportedOperationException("unimplemented");
    }
    public static Expression appendStart(Expression val, Expression str) {
        throw new UnsupportedOperationException("unimplemented");
    }
    public static Expression concat(Expression str1, Expression str2) {
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
    public boolean canAssignTo() {
        return Expression.super.canAssignTo();
    }
}
