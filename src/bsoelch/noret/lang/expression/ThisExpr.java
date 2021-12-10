package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class ThisExpr implements Expression {
    final Type.Proc type;

    @Override
    public boolean isBound() {
        return true;
    }

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

    @Override
    public String toString() {
        return "this";
    }

    @Override
    public boolean canInline() {
        return true;
    }
    @Override
    public boolean hasValue(ProgramContext context) {
        return false;//all possible compile-time evaluations are done on initialization
    }

    @Override
    public Value getValue(ProgramContext context) {
        throw new RuntimeException(this+" cannot be evaluated at compile time");
    }
}
