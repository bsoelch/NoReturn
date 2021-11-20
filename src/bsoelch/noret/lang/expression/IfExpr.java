package bsoelch.noret.lang.expression;

import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class IfExpr implements Expression {
    final Expression cond;
    final Expression ifVal;
    final Expression elseVal;

    final Type expectedOutput;

    public static Expression create(Expression cond, Expression ifVal, Expression elseVal){
        if(!Type.canCast(cond.expectedType(),Type.Primitive.BOOL,null)){
            throw new TypeError("cannot assign \""+cond.expectedType()+"\" to "+Type.Primitive.BOOL);
        }
        if(cond instanceof ValueExpression){//constant folding
            return ((Boolean)((Value.Primitive)((ValueExpression) cond).value.castTo(Type.Primitive.BOOL)).getValue())?
                    ifVal:elseVal;
        }
        return new IfExpr(cond, ifVal, elseVal);
    }

    private IfExpr(Expression cond, Expression ifVal, Expression elseVal) {
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
    public boolean isBound() {
        //addLater? prevent unnecessary coping if only one value is bound
        return ifVal.isBound()||elseVal.isBound();
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
