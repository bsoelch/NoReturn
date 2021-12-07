package bsoelch.noret.lang.expression;

import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class GetIndex implements Expression{
    public final Expression value;
    public final Expression index;
    final Type type;

    public static Expression create(Expression value, Expression index){
        Type valType = value.expectedType();
        Type indType = index.expectedType();
        Type type;
        if(valType instanceof Type.NoRetString){
            if(Type.canAssign(Type.Numeric.SIZE, indType,null)){
                type = ((Type.NoRetString) valType).charType;
            }else if(Type.canAssign(valType, indType,null)){
                type = Type.Numeric.SIZE;//index of substring
            }else{
                throw new TypeError("Invalid type for string index:"+
                        indType+ " string indices have to be unsigned integers or strings of the same type");
            }
        }else {
            //TODO addArray-access for tuples
            if(valType instanceof Type.Array){
                if(Type.canAssign(Type.Numeric.SIZE, indType,null)){
                    type =((Type.Array) valType).content;
                }else{
                    throw new TypeError("Invalid type for array index:"+
                            indType+ " Array indices have to be unsigned integers");
                }
            }else{
                throw new TypeError("Invalid type for array access: \"" +
                        valType +"\" only arrays,tuples and string support array-access");
            }
        }
        if(value instanceof ValueExpression&&index instanceof ValueExpression){//constant folding
            //set index is not supported for constants
            return ValueExpression.create(((ValueExpression) value).value.getAtIndex(((ValueExpression) index).value), null);
        }
        return new GetIndex(value, index,type);
    }

    private GetIndex(Expression value, Expression index,Type valType) {
        this.value=value;
        this.index=index;
        this.type=valType;
    }

    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        ValueView source=value.evaluate(parent, context);
        Value indexValue=index.evaluate(parent, context).get();
        return new ValueView() {
            @Override
            public Value get() {
                return source.get().getAtIndex(indexValue);
            }

            @Override
            public void set(Value newValue) {
                if(!Type.canAssign(type,newValue.getType(),null)){
                    throw new TypeError("cannot assign " +newValue.getType()+" to "+type);
                }
                newValue=newValue.castTo(type);
                source.set(source.get().setAtIndex(indexValue,newValue));
            }

        };
    }
    @Override
    public boolean canAssignTo() {
        return value.canAssignTo();
    }

    @Override
    public Type expectedType() {
        return type;
    }

    @Override
    public String toString() {
        return "GetIndex{"+value +"[" + index +"]}";
    }
}
