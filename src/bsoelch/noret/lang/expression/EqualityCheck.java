package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class EqualityCheck implements Expression {
    public final Expression left,right;
    public final boolean neq;

    public static Expression create(Expression left, boolean neq, Expression right,ProgramContext context) {
        if(!(Type.canCast(left.expectedType(),right.expectedType(),null)||
                Type.canCast(right.expectedType(),left.expectedType(),null))){
            return ValueExpression.create(Value.createPrimitive(Type.Primitive.BOOL, neq));
        }else{
            if(left.hasValue(context)&&right.hasValue(context)){
                return ValueExpression.create(Value.createPrimitive(Type.Primitive.BOOL,(left.getValue(context)
                        .equals(right.getValue(context)))^neq));
            }//addLater compare string/tuple concat blocks
            //addLater compile time evaluate if expressions are equal/different
            return new EqualityCheck(left, neq, right);
        }
    }

    public EqualityCheck(Expression left, boolean neq, Expression right) {
        this.left = left;
        this.neq = neq;
        this.right = right;
    }
    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        Value lVal = left.evaluate(parent, context).get();
        Value rVal = right.evaluate(parent, context).get();
        return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL, lVal.equals(rVal)^neq));
    }


    @Override
    public Type expectedType() {
        return Type.Primitive.BOOL;
    }

    @Override
    public boolean canAssignTo() {
        return false;
    }

    @Override
    public boolean hasValue(ProgramContext context) {
        return false;
    }

    @Override
    public Value getValue(ProgramContext context) {
        throw new RuntimeException(this+" cannot be evaluated at compile time");
    }
}
