package bsoelch.noret.lang;

import java.util.ArrayDeque;

public class CallQueue {
    private static class Frame{
        final Value[] args;
        final Procedure proc;
        private Frame(Value[] args, Procedure proc) {
            this.args = args;
            this.proc = proc;
        }
    }
    public static class QueueConsumer implements Runnable{
        final CallQueue queue;
        public QueueConsumer(CallQueue queue) {
            this.queue = queue;
        }

        public void run(){
            while(true){
                if(queue.runIfAvailable()){
                    if(queue.queue.isEmpty()){
                        synchronized (queue.lock){
                            try {
                                queue.lock.wait();
                            } catch (InterruptedException ignored) {}
                        }
                    }
                }else{
                    break;
                }
            }
            synchronized (queue.lock){
                queue.lock.notify();//notify all currently running QueueConsumer
            }
        }
    }

    private final ArrayDeque<Frame> queue=new ArrayDeque<>();
    public final Object lock=new Object();
    private int running=0;

    public void push(Value[] args,Procedure proc){
        assert(proc!=null);
        synchronized (queue){
            queue.addLast(new Frame(args,proc));
        }
        synchronized (lock){
            lock.notify();
        }
    }

    public boolean runIfAvailable(){
        Frame frame;
        synchronized (queue){
            running++;
            frame=queue.pollFirst();
        }
        if(frame!=null){
            frame.proc.run(this,frame.args);
        }
        synchronized (queue) {
            return ((--running) + queue.size()) > 0;
        }
    }
}
