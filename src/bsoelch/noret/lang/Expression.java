package bsoelch.noret.lang;

import bsoelch.noret.ProgramContext;

import java.util.ArrayList;

public interface Expression {
    /**returns true if the value returned by evaluate is bound to a variable*/
    boolean isBound();
    ValueView evaluate(Procedure parent, ArrayList<Value> context);
    Type expectedType();
    default boolean canAssignTo() {
        return false;
    }
    /**true f the value of this element is known at compile time*/
    boolean hasValue(ProgramContext context);
    /**returns the value of Expression in the given context,
     * throws a RuntimeException if this expression cannot be evaluated at compile time*/
    Value getValue(ProgramContext context);
}
