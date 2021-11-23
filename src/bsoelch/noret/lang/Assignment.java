package bsoelch.noret.lang;

import java.util.ArrayList;

public class Assignment implements Action {
    final Expression target;
    final Expression expr;

    public Assignment(Expression target, Expression expr) {
        this.target = target;
        this.expr = expr;
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
