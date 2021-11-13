package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class IfExpr implements Expression {
    final Expression cond;
    final Expression ifVal;
    final Expression elseVal;

    final Type expectedOutput;

    public IfExpr(Expression cond, Expression ifVal, Expression elseVal) {
        this.cond = cond;
        this.ifVal = ifVal;
        this.elseVal = elseVal;
        expectedOutput=Type.commonSupertype(ifVal.expectedType(),
                elseVal.expectedType());
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        if(Operations.asBool(cond.evaluate(parent, context).get())){
            return ifVal.evaluate(parent, context);
        }else{
            return elseVal.evaluate(parent, context);
        }
    }

    @Override
    public boolean canSet() {
        return ifVal.canSet()&&elseVal.canSet();
    }

    @Override
    public Type expectedType() {
        return expectedOutput;
    }
}
