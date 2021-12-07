package bsoelch.noret.lang.expression;

import bsoelch.noret.Parser;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class IfExpr implements Expression {
    public final Expression cond;
    public final Expression ifVal;
    public final Expression elseVal;

    final Type expectedOutput;

    public static Expression create(Expression cond, Expression ifVal, Expression elseVal, Parser.ParserContext context){
        cond=TypeCast.create(Type.Primitive.BOOL,cond, context);
        if(cond instanceof ValueExpression){//constant folding
            return ((Boolean)((Value.Primitive)((ValueExpression) cond).value.castTo(Type.Primitive.BOOL)).getValue())?
                    ifVal:elseVal;
        }
        Type expectedOut=Type.commonSupertype(ifVal.expectedType(),elseVal.expectedType());
        ifVal = TypeCast.create(expectedOut,ifVal, context);
        elseVal = TypeCast.create(expectedOut,elseVal, context);
        return new IfExpr(cond,expectedOut, ifVal, elseVal);
    }

    private IfExpr(Expression cond,Type expectedOut, Expression ifVal, Expression elseVal) {
        this.cond = cond;
        expectedOutput=expectedOut;
        this.ifVal = ifVal;
        this.elseVal = elseVal;
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
    public boolean canAssignTo() {
        return ifVal.canAssignTo()&&elseVal.canAssignTo();
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
