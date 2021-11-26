package bsoelch.noret.lang;

import bsoelch.noret.Parser;

import java.util.Scanner;
import java.util.function.BiConsumer;

public class Native {//TODO? make part of compiler
    public static void addProcsTo(Parser.ParserContext procNames) {
        Type.Generic genericA = new Type.Generic("a");
        procNames.declareProcedure("readLine",new NativeProcedure(
                new Type.Proc(new Type[]{genericA,new Type.Proc(new Type[]{Type.NoRetString.STRING8,genericA})}),
                (queue,params)-> queue.push(new Value[]{Value.createPrimitive(Type.NoRetString.STRING8,new Scanner(System.in).nextLine())
                        ,params[0]},(Procedure) params[1]),3));
        //addLater modifier-procedures for log targets
    }

    public static class NativeProcedure extends Procedure{
        final BiConsumer<CallQueue,Value[]> onRun;
        NativeProcedure(Type.Proc t, BiConsumer<CallQueue,Value[]> onRun,int maxValues) {
            super(t,maxValues);
            this.onRun=onRun;
            //TODO compiler information
        }

        @Override
        public boolean isNative() {
            return true;
        }

        @Override
        public void run(CallQueue queue, Value[] params) {
            onRun.accept(queue, params);
        }
    }

}
