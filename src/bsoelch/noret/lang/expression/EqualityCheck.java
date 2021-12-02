package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class EqualityCheck implements Expression {
    public final Expression left,right;
    public final boolean neq;

    public static Expression create(Expression left, boolean neq, Expression right) {
        if(!(Type.canCast(left.expectedType(),right.expectedType(),null)||
                Type.canCast(right.expectedType(),left.expectedType(),null))){
            return new ValueExpression(Value.createPrimitive(Type.Primitive.BOOL, neq),null);
        }else{
            if(left instanceof ValueExpression&&right instanceof ValueExpression){
                return new ValueExpression(Value.createPrimitive(Type.Primitive.BOOL,(((ValueExpression) left).value
                        .equals(((ValueExpression) right).value))^neq),null);
            }//addLater compare string/tuple concat blocks
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
}
