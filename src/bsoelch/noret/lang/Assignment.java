package bsoelch.noret.lang;

import bsoelch.noret.SyntaxError;
import bsoelch.noret.lang.expression.TypeCast;

import java.util.ArrayList;

public class Assignment implements Action {
    public final Expression target;
    public final Expression expr;

    public Assignment(Expression target, Expression expr) {
        this.target = target;
        if(!target.canAssignTo()){
            throw new SyntaxError(target+" cannot be modified");
        }
        //ensure values are cast to the correct type
        this.expr = TypeCast.create(target.expectedType(),expr, false);
    }

    @Override
    public void execute(Procedure parent,ArrayList<Value> context) {
        Value tmp= expr.evaluate(parent, context).get();
        if(expr.isBound()){//unbind value
            tmp=tmp.independentCopy();
        }
        target.evaluate(parent, context).set(tmp);
    }

    @Override
    public String toString() {
        return "Assignment:{" + target +"=" + expr+"}";
    }
}
