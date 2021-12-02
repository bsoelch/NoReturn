package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;
import java.util.Arrays;

public class InitStructOrArray implements Expression {
    //TODO initialization of non-constant expressions
    public static Expression newTuple(ArrayList<Expression> expressions) {
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
            return ValueExpression.create(new Value.ArrayOrTuple(values,false), null);
        }else{
            ArrayList<TupleConcat.Section> parts=new ArrayList<>(values.length);
            int i0=0;
            for(int i=0;i< values.length;i++){
                if(values[i]==null){
                    if(i>i0) {
                        parts.add(new TupleConcat.Section(
                                ValueExpression.create(new Value.ArrayOrTuple(Arrays.copyOfRange(values, i0, i), false), null),
                                false,i-i0,false
                        ));
                    }
                    parts.add(new TupleConcat.Section(expressions.get(i),false,1,true));
                    i0=i+1;
                }
            }
            return TupleConcat.createTuple(parts);
        }
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
