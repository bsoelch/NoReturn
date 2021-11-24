package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class IfExpr implements Expression {
    public final Expression cond;
    public final Expression ifVal;
    public final Expression elseVal;

    final Type expectedOutput;

    public static Expression create(Expression cond, Expression ifVal, Expression elseVal){
        cond=TypeCast.create(Type.Primitive.BOOL,cond,true);
        if(cond instanceof ValueExpression){//constant folding
            return ((Boolean)((Value.Primitive)((ValueExpression) cond).value.castTo(Type.Primitive.BOOL)).getValue())?
                    ifVal:elseVal;
        }
        return new IfExpr(cond, ifVal, elseVal);
    }

    private IfExpr(Expression cond, Expression ifVal, Expression elseVal) {
        this.cond = cond;
        expectedOutput=Type.commonSupertype(ifVal.expectedType(),
                elseVal.expectedType());
        this.ifVal = TypeCast.create(expectedOutput,ifVal,true);
        this.elseVal = TypeCast.create(expectedOutput,elseVal,false);
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
    public boolean isBound() {
        //addLater? prevent unnecessary coping if only one value is bound
        return ifVal.isBound()||elseVal.isBound();
    }

    @Override
    public boolean isMutable() {
        return ifVal.isMutable()&&elseVal.isMutable();
    }

    @Override
    public Type expectedType() {
        return expectedOutput;
    }

    @Override
    public String toString() {
        return "IfExpr{" +cond + "?" + ifVal +":" + elseVal + '}';
    }
}
