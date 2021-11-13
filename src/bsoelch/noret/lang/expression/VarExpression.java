package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class VarExpression implements Expression {
    final Type varType;
    final int id;
    public VarExpression(Type varType,int id) {
        this.id=id;
        this.varType=varType;
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return new ValueView() {
            @Override
            public Value get() {
                return context.get(id);
            }

            @Override
            public void set(Value newValue) {
                if(!Type.canAssign(varType,newValue.getType(),null)){
                    throw new IllegalArgumentException("unresolved Type-Error cannot assign " +
                            newValue.getType()+" to "+varType);
                }
                newValue=newValue.castTo(varType);
                newValue.bind(id);
                context.set(id,newValue);
            }

        };
    }

    @Override
    public boolean canSet() {
        return true;
    }
    @Override
    public Type expectedType() {
        return varType;
    }

}
