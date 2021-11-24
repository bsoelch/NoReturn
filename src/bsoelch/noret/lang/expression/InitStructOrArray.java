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
                if(((ValueExpression) expressions.get(i)).isVarSizeConstant()){
                    isConstant=false;
                }else{
                    values[i]=((ValueExpression) expressions.get(i)).value;
                }
            }else{
                isConstant=false;
            }
        }
        if(isConstant){
            return ValueExpression.create(new Value.Array(values), null);
        }
        throw new UnsupportedOperationException("unimplemented");
    }
    public static Expression newStruct(ArrayList<Expression> expressions, ArrayList<String> labels) {
        boolean isConstant=true;
        Value[] values=new Value[expressions.size()];
        for(int i=0;i< expressions.size();i++){
            if(expressions.get(i) instanceof ValueExpression){
                if(((ValueExpression) expressions.get(i)).isVarSizeConstant()){
                    isConstant=false;
                }else{
                    values[i]=((ValueExpression) expressions.get(i)).value;
                }
            }else{
                isConstant=false;
            }
        }
        if(isConstant){
            return ValueExpression.create(new Value.Struct(values,labels.toArray(new String[0])), null);
        }
        throw new UnsupportedOperationException("unimplemented");
    }

    @Override
    public boolean isBound() {
        return false;
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
