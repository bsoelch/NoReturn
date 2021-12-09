package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.SyntaxError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class StringCompare implements Expression {
    public final Expression left,right;
    public final OperatorType op;

    public static Expression create(Expression left, OperatorType op, Expression right, ProgramContext context) {
        if(!(left.expectedType() instanceof Type.NoRetString&&right.expectedType() instanceof Type.NoRetString)){
            throw new SyntaxError("StringCompare can only compare strings");
        }else{
            if(left.hasValue(context)&&right.hasValue(context)){
                return ValueExpression.create(compareValues((Value.StringValue) left.getValue(context),
                        op,(Value.StringValue) right.getValue(context)));
            }//addLater compare string concat blocks
            return new StringCompare(left, op, right);
        }
    }

    public StringCompare(Expression left, OperatorType op, Expression right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
    @Override
    public boolean isBound() {
        return false;
    }

    private static Value compareValues(Value.StringValue lVal,OperatorType op, Value.StringValue rVal) {
        int cmp = lVal.compareTo(rVal);
        switch (op){
            case GT:
                return Value.createPrimitive(Type.Primitive.BOOL, cmp > 0);
            case GE:
                return Value.createPrimitive(Type.Primitive.BOOL, cmp >= 0);
            case NE:
                return Value.createPrimitive(Type.Primitive.BOOL, cmp != 0);
            case EQ:
                return Value.createPrimitive(Type.Primitive.BOOL, cmp == 0);
            case LE:
                return Value.createPrimitive(Type.Primitive.BOOL, cmp <= 0);
            case LT:
                return Value.createPrimitive(Type.Primitive.BOOL, cmp < 0);
            default:
                throw new SyntaxError(op+" is no comparison operator");
        }
    }
    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        Value lVal = left.evaluate(parent, context).get();
        Value rVal = right.evaluate(parent, context).get();
        return ValueView.wrap(compareValues((Value.StringValue) lVal,op, (Value.StringValue) rVal));
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
        return false;//all possible compile-time evaluations are done on initialization
    }

    @Override
    public Value getValue(ProgramContext context) {
        throw new RuntimeException(this+" cannot be evaluated at compile time");
    }
}
