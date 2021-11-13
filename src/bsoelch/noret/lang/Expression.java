package bsoelch.noret.lang;

import java.util.ArrayList;

public interface Expression {
    ValueView evaluate(Procedure parent, ArrayList<Value> context);
    Type expectedType();
    default boolean canSet() {
        return false;
    }
}
