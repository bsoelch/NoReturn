package bsoelch.noret.lang;

import java.util.ArrayList;
import java.util.HashMap;

public class ValDef implements Action {
    final Type type;
    final Expression initValue;

    public ValDef(Type type, Expression initValue) {
        if(Type.canAssign(type,initValue.expectedType(),null)){
            this.type = type;
            this.initValue = initValue;
        }else{
            throw new IllegalArgumentException("Cannot assign "+
                    initValue.expectedType()+" to "+type);
        }
    }

    @Override
    public void execute(Procedure parent,ArrayList<Value> context) {
        Value tmp = initValue.evaluate(parent, context).get().castTo(type);
        tmp.bind(context.size());
        context.add(tmp);
    }
}
