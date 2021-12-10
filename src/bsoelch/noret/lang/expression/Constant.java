package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class Constant implements Expression {
    final Value value;
    public final String constId;

    public static Expression create(Value value,String constId){
        if(constId==null||value.getType() instanceof Type.Primitive){
            return ValueExpression.create(value);
        }else{
            return new Constant(value, constId);
        }
    }

    private Constant(Value value, String constId) {
        this.value = value;
        this.constId = constId;
    }

    @Override
    public boolean isBound() {
        return value.isMutable();
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return ValueView.wrap(value);
    }
    @Override
    public Type expectedType() {
        return value.getType();
    }

    @Override
    public String toString() {
        return "Constant{"+constId+":"+value+"}";
    }

    @Override
    public boolean canInline() {
        return true;
    }

    @Override
    public boolean hasValue(ProgramContext context) {
        return true;
    }

    @Override
    public Value getValue(ProgramContext context) {
        return value;
    }
}
