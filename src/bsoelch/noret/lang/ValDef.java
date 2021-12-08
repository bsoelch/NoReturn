package bsoelch.noret.lang;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.expression.TypeCast;

import java.util.ArrayList;

public class ValDef implements Action {
    final Type type;
    final Expression initValue;

    public ValDef(Type type, Expression initValue, ProgramContext context) {
        if(Type.canAssign(type,initValue.expectedType(),null)){
            this.type = type;
            //ensure values are cast to the correct type
            this.initValue =TypeCast.create(type,initValue, context);
        }else{
            throw new TypeError("Cannot assign "+initValue.expectedType()+" to "+type);
        }
    }

    @Override
    public void execute(Procedure parent,ArrayList<Value> context) {
        Value tmp = initValue.evaluate(parent, context).get().castTo(type);
        context.add(tmp);
    }

    @Override
    public String toString() {
        return "ValDef["+type+"]{" + initValue +'}';
    }

    public Type getType() {
        return type;
    }
    public Expression getInitValue() {
        return initValue;
    }
}
