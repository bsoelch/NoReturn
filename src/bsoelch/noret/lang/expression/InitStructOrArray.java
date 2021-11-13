package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class InitStructOrArray implements Expression {
    //TODO initialization of non-constant expressions
    public static Expression newArray(ArrayList<Expression> expressions) {
        boolean isConstant=true;
        Value[] values=new Value[expressions.size()];
        for(int i=0;i< expressions.size();i++){
            if(expressions.get(i) instanceof ValueExpression){
                values[i]=((ValueExpression) expressions.get(i)).value;
            }else{
                isConstant=false;
            }
        }
        if(isConstant){
            return new ValueExpression(new Value.Array(values));
        }
        throw new UnsupportedOperationException("unimplemented");
    }
    public static Expression newStruct(ArrayList<Expression> expressions, ArrayList<String> labels) {
        boolean isConstant=true;
        Value[] values=new Value[expressions.size()];
        for(int i=0;i< expressions.size();i++){
            if(expressions.get(i) instanceof ValueExpression){
                values[i]=((ValueExpression) expressions.get(i)).value;
            }else{
                isConstant=false;
            }
        }
        if(isConstant){
            return new ValueExpression(new Value.Struct(values,labels.toArray(new String[0])));
        }
        throw new UnsupportedOperationException("unimplemented");
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        throw new UnsupportedOperationException("unimplemented");
    }

    @Override
    public Type expectedType() {
        throw new UnsupportedOperationException("unimplemented");
    }
}
