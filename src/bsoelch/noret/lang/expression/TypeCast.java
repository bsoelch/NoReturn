package bsoelch.noret.lang.expression;

import bsoelch.noret.Parser;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class TypeCast implements Expression{
    public final Expression value;
    public final Type type;

    /**
     * @param context ParserContext of the Parser that parsed this expression*/
    public static Expression create(Type castType, Expression value, Parser.ParserContext context){
        if(!Type.canCast(castType,value.expectedType(),null)){
            throw new TypeError("Values of type "+value.expectedType()+ " cannot be cast to "+castType);
        }
        if(value instanceof ValueExpression){
            Value value1 = ((ValueExpression) value).value.castTo(castType);
            assert value1.getType().equals(castType);
            return ValueExpression.create(value1, null);
        }
        if(value.expectedType().equals(castType)){
            return value;
        }else{
            if(castType instanceof Type.AnyType){
                context.addRuntimeType(value.expectedType(),false);
            }
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
