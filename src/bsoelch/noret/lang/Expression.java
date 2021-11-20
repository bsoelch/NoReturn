package bsoelch.noret.lang;

import java.util.ArrayList;

public interface Expression {
    /**returns true if the value returned by evaluate is bound to a variable*/
    boolean isBound();
    ValueView evaluate(Procedure parent, ArrayList<Value> context);
    Type expectedType();
    default boolean canSet() {
        return false;
    }
}
