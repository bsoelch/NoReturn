package bsoelch.noret.lang;

import java.util.HashMap;
import java.util.Scanner;

public class Native {
    public static void addProcsTo(HashMap<String, Procedure> procNames) {
        //addLater better handling of native procedures
        Type.Generic genericA = new Type.Generic("a");
        procNames.put("readLine",new Procedure(
                new Type.Proc(new Type[]{genericA,new Type.Proc(new Type[]{Type.NoRetString.STRING8,genericA})})){
            @Override
            public void run(CallQueue queue, Value[] params) {
                queue.push(new Value[]{Value.createPrimitive(Type.NoRetString.STRING8,new Scanner(System.in).nextLine())
                        ,params[0]},(Procedure) params[1]);
            }
        });
        //addLater modifier for log paths
    }

}
