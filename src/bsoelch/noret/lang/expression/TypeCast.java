package bsoelch.noret.lang.expression;

import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class TypeCast implements Expression{
    final Expression value;
    final Type type;

    public TypeCast(Type castType, Expression value) {
        this.value = value;
        if(!Type.canCast(castType,value.expectedType(),null)){
            throw new TypeError("Values of type "+value.expectedType()+ " cannot be cast to "+castType);
        }
        //typecasts are evaluated at runtime
        type=castType;
    }


    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        ValueView source=value.evaluate(parent, context);
        return new ValueView() {
            @Override
            public Value get() {
                return source.get().castTo(type);
            }

            @Override
            public void set(Value newValue) {
                //TODO type handling
                source.set(newValue);
            }
        };
    }

    @Override
    public boolean canSet() {
        return value.canSet();
    }

    @Override
    public Type expectedType() {
        return type;
    }
}
