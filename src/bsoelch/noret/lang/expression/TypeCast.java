package bsoelch.noret.lang.expression;

import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class TypeCast implements Expression{
    public final Expression value;
    public final Type type;

    /**
     * @param evalConstants if false evaluation of constants is disabled (for ensuring the value in valDefs
     *                      and assignments is cast to the correct type)
     * */
    public static Expression create(Type castType, Expression value, boolean evalConstants){
        if(!Type.canCast(castType,value.expectedType(),null)){
            throw new TypeError("Values of type "+value.expectedType()+ " cannot be cast to "+castType);
        }
        //TODO? check if evalConstants flag is necessary
        if(value instanceof ValueExpression&&evalConstants){
            return ValueExpression.create(((ValueExpression) value).value.castTo(castType), null);
        }
        if(value.expectedType().equals(castType)){
            return value;
        }else{
            return new TypeCast(castType,value);
        }
    }
    private TypeCast(Type castType, Expression value) {
        this.value = value;
        //typecasts are evaluated at runtime
        type=castType;
    }

    @Override
    public boolean isBound() {
        return value.isBound();
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        ValueView source=value.evaluate(parent, context);
        return ValueView.wrap(source.get().castTo(type));
    }

    @Override
    public boolean canAssignTo() {
        return false;
    }

    @Override
    public Type expectedType() {
        return type;
    }

    @Override
    public String toString() {
        return "TypeCast{" +type+ ":" + value +'}';
    }
}
