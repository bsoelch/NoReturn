package bsoelch.noret.lang.expression;

import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class GetField implements Expression{
    final Expression value;
    final String fieldName;
    final Type type;
    public GetField(Expression value, String fieldName) {
        this.value=value;
        this.fieldName=fieldName;
        Type valType = value.expectedType();
        type=valType.getField(fieldName);
        if(type==null){
            throw new SyntaxError("Type "+valType+" does not have a field \""+fieldName+"\"");
        }
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        ValueView source=value.evaluate(parent, context);
        return new ValueView() {
            @Override
            public Value get() {
                return source.get().getField(fieldName);
            }

            @Override
            public void set(Value newValue) {
                if(!Type.canAssign(type,newValue.getType(),null)){
                    throw new TypeError("cannot assign " + newValue.getType()+" to "+type);
                }
                newValue=newValue.castTo(type);
                source.set(source.get().setField(fieldName,newValue));
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
