package bsoelch.noret.lang.expression;

import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;

public class TupleConcat implements Expression {

    private static void addToList(Expression tpl, ArrayList<Section> tmp) {
        if (tpl instanceof TupleConcat) {
            tmp.addAll(((TupleConcat) tpl).sections);
        } else {
            if (tpl.expectedType() instanceof Type.Tuple) {
                tmp.add(new Section(tpl, false, ((Type.Tuple) tpl.expectedType()).getElements().length, false));
            } else if (tpl.expectedType() instanceof Type.Array){
                if(tpl instanceof ValueExpression){
                    tmp.add(new Section(tpl, false, ((Value.ArrayOrTuple)((ValueExpression)tpl).value).elements().length, false));
                }else{//addLater slices
                    tmp.add(new Section(tpl, true, -1, false));
                }
            } else{
                throw new TypeError("unexpected Type in TupleConcat "+tpl.expectedType()+" expected Array or Tuple");
            }
        }
    }
    public static Expression pushEnd(Expression tpl, Expression val) {
        ArrayList<Section> tmp=new ArrayList<>();
        addToList(tpl, tmp);
        tmp.add(new Section(val,false,1,true));
        return new TupleConcat(tmp);
    }
    public static Expression pushStart(Expression val, Expression tpl) {
        ArrayList<Section> tmp=new ArrayList<>();
        tmp.add(new Section(val,false,1,true));
        addToList(tpl,tmp);
        return new TupleConcat(tmp);
    }
    public static Expression concat(Expression tpl1, Expression tpl2) {
        ArrayList<Section> tmp=new ArrayList<>();
        addToList(tpl1, tmp);
        addToList(tpl2, tmp);
        return new TupleConcat(tmp);
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

    //TODO join constants
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
            type= new Type.Array(sections.stream().map(s -> s.expr.expectedType()).reduce(Type.EMPTY_TYPE,Type::commonSupertype));
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

}
