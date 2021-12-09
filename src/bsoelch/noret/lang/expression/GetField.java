package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class GetField implements Expression{
    public final Expression value;
    public final String fieldName;
    final Type type;

    public static Expression create(Expression value, String fieldName, ProgramContext context){
        Type valType = value.expectedType();
        Type type=valType.getField(fieldName);
        if(type==null){
            throw new SyntaxError("Type "+valType+" does not have a field \""+fieldName+"\"");
        }else if(Type.FIELD_NAME_TYPE.equals(fieldName)){
            context.addRuntimeType(valType,true);
            if(! (valType instanceof Type.AnyType)){//evaluate .type expressions at compile time
                return ValueExpression.create(new Value.TypeValue(valType));
            }
        }
        if((value.hasValue(context)&&!type.needsExternalData)&&!value.canAssignTo()){//constant folding
            //set field is not supported for constants
            return ValueExpression.create(value.getValue(context).getField(fieldName));
        }
        return new GetField(value,fieldName,type);
    }

    private GetField(Expression value, String fieldName,Type fieldType) {
        this.value=value;
        this.fieldName=fieldName;
        this.type=fieldType;
    }

    @Override
    public boolean isBound() {
        return true;
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
    public boolean canAssignTo() {
        return value.canAssignTo()&&type.isMutableFlied(fieldName);
    }

    @Override
    public Type expectedType() {
        return type;
    }

    @Override
    public String toString() {
        return "GetField{"+value + "." + fieldName+"}" ;
    }

    @Override
    public boolean hasValue(ProgramContext context) {
        return value.hasValue(context);
    }

    @Override
    public Value getValue(ProgramContext context) {
        return value.getValue(context).getField(fieldName);
    }
}
