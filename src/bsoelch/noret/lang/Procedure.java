package bsoelch.noret.lang;

import bsoelch.noret.TypeError;

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
        final boolean isOptional;
        public DynamicProcChild(int varId, boolean isOptional) {
            this.varId = varId;
            this.isOptional=isOptional;
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
    public Value castTo(Type t) {
        if(Type.canAssign(t,type,new HashMap<>())){
            if(t instanceof Type.Optional){
                return new Optional((Type.Optional) t,castTo(((Type.Optional)t).content));
            }else{
                return this;
            }
        }else{
            return super.castTo(t);
        }
    }
    @Override
    public boolean isMutable() {
        return false;
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
    public String stringRepresentation() {
        return "[procedure]";
    }

    @Override
    public String toString() {
        return "Procedure:("+type+")";
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
            //addLater run leaves directly
            for(int i=0;i<children.length;i++){
                Procedure proc;
                if(children[i] == RECURSIVE_CALL){
                    proc=this;
                }else if(children[i] instanceof StaticProcChild){
                    proc=((StaticProcChild) children[i]).value;
                }else{
                    Value tmp=values.get(((DynamicProcChild)children[i]).varId);
                    if(((DynamicProcChild)children[i]).isOptional){
                        if(((Optional)tmp).content==Value.NONE){
                            continue;//skip none
                        }else{
                            proc=(Procedure)((Optional)tmp).content;
                        }
                    }else{
                        proc=(Procedure) tmp;
                    }
                }
                Value[] args=new Value[childArgs[i].length];
                for(int j=0;j<childArgs[i].length;j++){
                    args[j]=childArgs[i][j].evaluate(this,values).get().castTo(proc.argTypes()[j]);
                    if(childArgs[i][j].isBound()) {//call by value
                        args[j]=args[j].independentCopy();
                    }
                }
                queue.push(args,proc);
            }
        }
    }

}
