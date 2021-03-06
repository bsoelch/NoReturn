package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;

public class TupleConcat implements Expression {

    //TODO join constant values
    private static void addToList(Expression expr, ArrayList<Section> tmp,boolean wrap,ProgramContext context) {
        if(wrap){
            tmp.add(new Section(expr,false,1,true));
        }else if (expr instanceof TupleConcat) {
            tmp.addAll(((TupleConcat) expr).sections);
        } else {
            if (expr.expectedType() instanceof Type.Array){
                if(expr.hasValue(context)){
                    tmp.add(new Section(expr, false, ((Value.ArrayOrTuple) expr.getValue(context)).elements().length, false));
                }else{//addLater slices
                    tmp.add(new Section(expr, true, -1, false));
                }
            } else{
                throw new TypeError("unexpected Type in Array concatenation "+expr.expectedType()+" expected Array");
            }
        }
    }
    public static Expression pushEnd(Expression tpl, Expression val,ProgramContext context) {
        ArrayList<Section> tmp=new ArrayList<>();
        addToList(tpl, tmp,false,context);
        addToList(val, tmp,true,context);
        return new TupleConcat(tmp);
    }
    public static Expression pushStart(Expression val, Expression tpl,ProgramContext context) {
        ArrayList<Section> tmp=new ArrayList<>();
        addToList(val, tmp,true,context);
        addToList(tpl,tmp,false,context);
        return new TupleConcat(tmp);
    }
    public static Expression concat(Expression tpl1, Expression tpl2,ProgramContext context) {
        ArrayList<Section> tmp=new ArrayList<>();
        addToList(tpl1, tmp,false,context);
        addToList(tpl2, tmp,false,context);
        return new TupleConcat(tmp);
    }
    static Expression createTuple(ArrayList<Section> parts) {
        return new TupleConcat(parts);
    }


    static class Section{
        final Expression expr;
        final boolean varSize;
        final long size;
        final boolean wrap;
        Section(Expression expr, boolean varSize, long size, boolean wrap) {
            this.expr = expr;
            this.varSize = varSize;
            this.size = size;
            this.wrap = wrap;
        }
    }

    private final Type type;
    private final ArrayList<Section> sections;

    private TupleConcat(ArrayList<Section> fromSections) {
        this.sections =fromSections;
        if(sections.stream().noneMatch(t->t.varSize)){
            Stream<Type[]> elements=sections.stream().map(s -> s.expr.expectedType())
                    .map(t->(t instanceof Type.Tuple?((Type.Tuple) t).getElements():new Type[]{t}));
            Type[] collect=new Type[elements.mapToInt(x->x.length).sum()];
            int off=0;
            for(Iterator<Type[]> streamItr=elements.iterator();streamItr.hasNext();){
                Type[] next=streamItr.next();
                System.arraycopy(next,0,collect,off,next.length);
                off+= next.length;
            }
            type=new Type.Tuple(null,collect);
        }else{
            type= new Type.Array(sections.stream().map(s -> s.expr.expectedType()).reduce(Type.Union.EMPTY,Type::commonSupertype));
        }
    }


    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        Stream<Value> elements=sections.stream().flatMap(s->Stream.of(s.wrap?new Value[]{s.expr.evaluate(parent, context).get()}:
                ((Value.ArrayOrTuple)s.expr.evaluate(parent, context).get()).elements()));
        if(type instanceof Type.Array){
            elements=elements.map(e->e.castTo(((Type.Array) type).content));
        }
        return ValueView.wrap(new Value.ArrayOrTuple(type,elements.toArray(Value[]::new)));
    }

    @Override
    public Type expectedType() {
        return type;
    }

    @Override
    public boolean canInline() {
        return false;
    }

    //addLater pre-evaluation of TupleConcat
    @Override
    public boolean hasValue(ProgramContext context) {
        return false;//all possible compile-time evaluations are done on initialization
    }

    @Override
    public Value getValue(ProgramContext context) {
        throw new RuntimeException(this+" cannot be evaluated at compile time");
    }
}
