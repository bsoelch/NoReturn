package bsoelch.noret.lang;

import bsoelch.noret.NoRetRuntimeError;
import bsoelch.noret.Parser;

import java.util.ArrayList;

public class Assertion implements Action {
    public final Expression expr;
    public final String assertMsg;

    public Assertion(Expression expr,String assertMsg) {
        this.expr=expr;
        this.assertMsg=assertMsg;
    }
    @Override
    public void execute(Procedure parent, ArrayList<Value> context) {
        Value value = expr.evaluate(parent, context).get();
        if(!(Boolean)((Value.Primitive)value).getValue()){
            throw new NoRetRuntimeError("assertion Failed: "+assertMsg);
        }
    }

    @Override
    public String toString() {
        return "Assert{"+expr+"}(" +assertMsg+')';
    }
}
