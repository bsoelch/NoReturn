package bsoelch.noret.lang;

import java.util.ArrayList;

public interface Action {
    void execute(Procedure parent,ArrayList<Value> context);
}
