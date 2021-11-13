package bsoelch.noret;

import bsoelch.noret.lang.CallQueue;
import bsoelch.noret.lang.Procedure;
import bsoelch.noret.lang.Value;

import java.io.FileReader;
import java.io.IOException;

public class Main {
    //TODO option to compile code to C

    public static void main(String[] args) throws IOException {
        Parser parser=new Parser();//TODO input program-name through arguments
        Procedure main=parser.parse(new FileReader(System.getProperty("user.dir")+"/test.noret"));
        Value[] progArgs;
        if(main.argTypes().length==0){
            progArgs=new Value[0];
        }else{
            progArgs=new Value[]{
                    //TODO pass arguments to main
            };
            progArgs=new Value[]{
                    new Value.Array(progArgs)
            };
        }
        CallQueue callQueue=new CallQueue();
        callQueue.push(progArgs,main);
        new Thread(new CallQueue.QueueConsumer(callQueue)).start();
        new CallQueue.QueueConsumer(callQueue).run();
    }
}
