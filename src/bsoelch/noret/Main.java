package bsoelch.noret;

import bsoelch.noret.lang.CallQueue;
import bsoelch.noret.lang.Procedure;
import bsoelch.noret.lang.Type;
import bsoelch.noret.lang.Value;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        if(args.length==0){
            System.out.println("usage: <pathToFile>");
            System.out.println(" runs the no-ret file with the given path");
            return;
        }
        Parser parser=new Parser(new FileReader(args[0]));
        parser.compile(new BufferedWriter(new FileWriter(args[0]+".c")));
        Procedure start=parser.interpret();
        Value[] progArgs;
        if(start.argTypes().length==0){
            progArgs=new Value[0];
        }else{
            progArgs=new Value[args.length-1];
            for(int i=1;i< args.length;i++){
                progArgs[i-1]=Value.createPrimitive(Type.NoRetString.STRING8,args[i]);
            }
            progArgs=new Value[]{
                    new Value.ArrayOrTuple(progArgs,true)
            };
        }
        CallQueue callQueue=new CallQueue();
        callQueue.push(progArgs,start);
        new Thread(new CallQueue.QueueConsumer(callQueue)).start();
        new CallQueue.QueueConsumer(callQueue).run();
    }
}
