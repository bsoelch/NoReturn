package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class TupleConcat implements Expression {
    //TODO implement TupleConcat
    public static Expression pushEnd(Expression tpl, Expression val) {
        throw new UnsupportedOperationException("unimplemented");
    }
    public static Expression pushStart(Expression val, Expression tpl) {
        throw new UnsupportedOperationException("unimplemented");
    }
    public static Expression concat(Expression tpl1, Expression tpl2) {
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
