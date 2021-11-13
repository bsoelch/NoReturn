package bsoelch.noret.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Procedure extends Value{
    public interface ProcChild {}

    public static class StaticProcChild implements ProcChild {
        final Procedure value;
        public StaticProcChild(Procedure value) {
            this.value = value;
        }
    }
    public static class DynamicProcChild implements ProcChild {
        final int varId;
        public DynamicProcChild(int varId) {
            this.varId = varId;
        }
    }
    public static final ProcChild RECURSIVE_CALL=new ProcChild() {};

    final Action[] primitives;
    final ProcChild[] children;
    final Expression[][] childArgs;
    final int maxValues;

    public Procedure(Type.Proc type, Action[] primitives, ProcChild[] children,
                     Expression[][] childArgs, int maxValues) {
        super(type);
        this.primitives = primitives;
        this.children = children;
        this.childArgs = childArgs;
        this.maxValues = maxValues;
    }
    /**NOP Procedure with given Type*/
    Procedure(Type.Proc t) {
        super(t);
        primitives=null;
        children =null;
        childArgs =null;
        maxValues =0;
    }
    @Override
    public Procedure castTo(Type t) {
        if(Type.canAssign(t,type,new HashMap<>())){
            return this;
        }else{
            throw new IllegalArgumentException("cannot cast:"+type+" to "+t);
        }
    }
    @Override
    public boolean equals(Object o) {
        return this==o;
    }
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
    @Override
    protected String valueToString() {
        return "[procedure]";
    }

    public Type[] argTypes(){
        return ((Type.Proc)getType()).getArgTypes();
    }

    /**returns true if this procedure does not have any child procedures*/
    public boolean isLeaf(){
        return children.length==0;
    }

    public void run(CallQueue queue,Value[] params){
        if(primitives!=null){
            ArrayList<Value> values=new ArrayList<>(maxValues);
            values.addAll(Arrays.asList(params));
            for(Action a:primitives){
                a.execute(this,values);
            }
            for(int i=0;i<children.length;i++){
                Procedure proc;
                if(children[i] == RECURSIVE_CALL){
                    proc=this;
                }else if(children[i] instanceof StaticProcChild){
                    proc=((StaticProcChild) children[i]).value;
                }else{
                    Value tmp=values.get(
                            ((DynamicProcChild)children[i]).varId);
                    if(tmp==Value.NOP){
                        continue;//skip NOPs
                    }
                    proc=(Procedure) tmp;
                }
                Value[] args=new Value[childArgs[i].length];
                for(int j=0;j<childArgs[i].length;j++){
                    args[j]=childArgs[i][j].evaluate(this,values).get().castTo(proc.argTypes()[j]);
                }
                queue.push(args,proc);
            }
        }
    }

}
