package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class ThisExpr implements Expression {
    final Type.Proc type;
    public ThisExpr(Type.Proc procType) {
        type = procType;
    }
    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return ValueView.wrap(parent);
    }
    @Override
    public Type expectedType() {
        return type;
    }
}
