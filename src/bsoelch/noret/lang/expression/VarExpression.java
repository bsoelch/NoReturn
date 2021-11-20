package bsoelch.noret.lang.expression;

import bsoelch.noret.TypeError;
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
    public boolean isBound() {
        return true;
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
                    throw new TypeError("cannot assign " +newValue.getType()+" to "+varType);
                }
                newValue=newValue.castTo(varType);
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
