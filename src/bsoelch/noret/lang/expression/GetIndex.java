package bsoelch.noret.lang.expression;

import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class GetIndex implements Expression{
    final Expression value;
    final Expression index;
    final Type type;

    public static Expression create(Expression value, Expression index){
        Type valType = value.expectedType();
        Type indType = index.expectedType();
        Type type;
        if(valType == Type.Primitive.STRING){
            if(Type.canAssign(Type.Numeric.UINT64, indType,null)){
                type = Type.Numeric.UINT8;//utf8 char-code
            }else if(Type.canAssign(Type.Numeric.STRING, indType,null)){
                type = Type.Numeric.UINT64;//index of string
            }else{
                throw new TypeError("Invalid type for string index:"+
                        indType+ " string indices have to be unsigned integers or strings");
            }
        }else {
            if(valType instanceof Type.Array){
                if(Type.canAssign(Type.Numeric.UINT64, indType,null)){
                    type =((Type.Array) valType).content;
                }else{
                    throw new TypeError("Invalid type for array index:"+
                            indType+ " Array indices have to be unsigned integers");
                }
            }else{
                throw new TypeError("Invalid type for array/dictionary access: \"" +
                        valType +"\" only dictionary, arrays and string support dict-access");
            }
        }
        if(value instanceof ValueExpression&&index instanceof ValueExpression){//constant folding
            //set index is not supported for constants
            return new ValueExpression(((ValueExpression) value).value.getAtIndex(((ValueExpression) index).value));
        }
        return new GetIndex(value, index,type);
    }

    private GetIndex(Expression value, Expression index,Type valType) {
        this.value=value;
        this.index=index;
        this.type=valType;
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
    public boolean canSet() {
        return value.canSet();
    }

    @Override
    public Type expectedType() {
        return type;
    }
}
