package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class ValueExpression implements Expression {
    final Value value;
    public final String constId;

    public static Expression create(Value value,String constId){
        //TODO own class for constants with dataValues
        return new ValueExpression(value,constId);
    }

    public ValueExpression(Value value, String constId) {
        this.value = value;
        this.constId = constId;
    }

    @Override
    public boolean isBound() {
        return (constId!=null)&&value.isMutable();
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
        return "ValueExpression{"+value+"}";
    }

    public Value getValue() {
        return value;
    }
}
