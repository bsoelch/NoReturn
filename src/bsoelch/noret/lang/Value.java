package bsoelch.noret.lang;

import bsoelch.noret.NoRetRuntimeError;
import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Value {
    /**argument to bind values to a global variable*/
    public static final int GLOBAL_VAR = -1;

    public static final Value NONE= new Value(Type.NONE_TYPE) {
        @Override
        public boolean equals(Object o) {
            return o==this;
        }
        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
        @Override
        public Value castTo(Type t) {
            if(t==Type.NONE_TYPE||t== Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else if(t instanceof Type.Optional){
                //TODO OptionalValue
                throw new UnsupportedOperationException("Unimplemented");
            }else{
                throw new TypeError("Cannot cast none to \""+t+"\"");
            }
        }
        @Override
        protected String valueToString() {
            return "none";
        }
    };
    public static final Value NOP= new Value(Type.NOP_TYPE) {
        @Override
        public boolean equals(Object o) {
            return o==this;
        }
        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
        @Override
        public Value castTo(Type t) {
            if(t==Type.NOP_TYPE||t== Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else if(t instanceof Type.Proc){
                return new Procedure((Type.Proc) t);
            }else{
                throw new TypeError("Cannot cast NOP to\""+t+"\"");
            }
        }
        @Override
        protected String valueToString() {
            return "NOP";
        }
    };
    public static final Value TRUE = createPrimitive(Type.Primitive.BOOL,true) ;
    public static final Value FALSE = createPrimitive(Type.Primitive.BOOL,false) ;

    /**wrapper of HashMap< String, V > that does not allow replacing entries*/
    private static class FieldMap<V>{
        private final HashMap<String,V> map=new HashMap<>();
        V get(String key){
            return map.get(key);
        }
        void put(String key,V value){
            if(map.put(key,value)!=null){
                throw new RuntimeException("element \""+key+"\" already exists");
            }
        }
    }

    /**only for internal use in Value types*/
    protected final FieldMap<Supplier<Value>> getters=new FieldMap<>();
    /**only for internal use in Value types
     * input of setter: new Value of field
     * output of setter: Value with modified field */
    protected final FieldMap<Function<Value,Value>> setters=new FieldMap<>();
    protected final Type type;

    private final HashSet<Integer> boundTo=new HashSet<>();
    //addLater meta-Evaluate Mode for Compiler:
    // Values are only stored by their types
    // all Array accesses are successful
    // all value bindings are stored
    //TODO ensure that all proc arguments are independent (excluding shared values)

    public Value(Type type){
        this.type=type;
        getters.put(Type.FIELD_NAME_TYPE,()->
                createPrimitive(Type.Primitive.TYPE,type));
    }
    public final Type getType() {
        return type;
    }

    public void bind(int id){
        boundTo.add(id);
    }
    public boolean isBound(){
        return boundTo.size()>0;
    }
    public boolean isBoundExcept(int ignoredId){
        if(boundTo.contains(ignoredId)){
            return boundTo.size()>1;
        }else{
            return boundTo.size()>0;
        }
    }
    public Value independentCopy(int ignoredId){
        return this;
    }

    public abstract Value castTo(Type t);
    protected abstract String valueToString();

    /**equals on values is used for the == and != operators*/
    @Override
    public abstract boolean equals(Object o);

    public final Value getField(String fieldId) {
        Supplier<Value> get= getters.get(fieldId);
        if(get==null){
            throw new SyntaxError("Field \""+fieldId+"\" of "+getType()+" cannot be read");
        }
        return get.get();
    }
    public Value setField(String fieldId, Value value) {
        Function<Value,Value> set= setters.get(fieldId);
        if(set==null){
            throw new SyntaxError("Field \""+fieldId+ "\" of "+getType()+" cannot be modified");
        }
        return set.apply(value);
    }

    //addLater? make IndexOperators return optional
    public Value getAtIndex(Value index) {
        throw new SyntaxError("Index access is not supported" + " for "+getType());
    }
    public Value setAtIndex(Value index,Value value) {
        throw new SyntaxError("Index access is not supported" + " for "+getType());
    }

    public Value getRange(Value off,Value to) {
        throw new SyntaxError("Range access is not supported" +" for "+getType());
    }
    public Value setRange(Value off,Value to,Value value) {
        throw new SyntaxError("Range access is not supported" +" for "+getType());
    }

    public static Value createPrimitive(Type.Primitive type, Object value){
        //addLater? typeCheck value
        if(type== Type.Primitive.ANY){
            throw new TypeError("Cannot create instances of Type \""+Type.Primitive.ANY+"\"");
        }
        if(type== Type.Primitive.STRING){
            return new StringValue((String)value);
        }else if(type instanceof Type.Numeric){
            return new NumericValue((Type.Numeric) type,value);
        }else{
            return new Primitive(type,value);
        }
    }
    public static class Primitive extends Value{
        final Object value;
        private Primitive(Type.Primitive type, Object value) {
            super(type);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public Primitive castTo(Type t) {
            if(t==type||t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else{
                throw new TypeError("Cannot cast "+type+" to "+t);
            }
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Primitive)) return false;
            Primitive primitive = (Primitive) o;
            return Objects.equals(type, primitive.type)&&
                    Objects.equals(value, primitive.value);
        }
        @Override
        public int hashCode() {
            return Objects.hash(type,value);
        }
        @Override
        protected String valueToString() {
            return value.toString();
        }
    }
    public static class NumericValue extends Primitive{
        private NumericValue(Type.Numeric type, Object value) {
            super(type,value);
        }

        @Override
        public NumericValue castTo(Type t) {
            if(t==type||t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;//addLater? extract default casts
            }
            if(t instanceof Type.Numeric){
                if(((Type.Numeric) t).isFloat){
                    switch (((Type.Numeric) t).level){
                        case 2:
                            return (NumericValue) createPrimitive((Type.Numeric)t,
                                    ((Number)value).floatValue());
                        case 3:
                            return (NumericValue) createPrimitive((Type.Numeric)t,
                                    ((Number)value).doubleValue());
                        default:
                            throw new SyntaxError("exceeded maximum number capacity");
                    }
                }else{
                    switch (((Type.Numeric) t).level){
                        case 0:
                            return (NumericValue) createPrimitive((Type.Numeric)t,
                                    ((Number)value).byteValue());
                        case 1:
                            return (NumericValue) createPrimitive((Type.Numeric)t,
                                    ((Number)value).shortValue());
                        case 2:
                            return (NumericValue) createPrimitive((Type.Numeric)t,
                                    ((Number)value).intValue());
                        case 3:
                            return (NumericValue) createPrimitive((Type.Numeric)t,
                                    ((Number)value).longValue());
                        default:
                            throw new SyntaxError("exceeded maximum number capacity");
                    }
                }
            }else {
                throw new TypeError("Cannot cast "+type+" to "+t);
            }
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Primitive)) return false;
            Primitive primitive = (Primitive) o;
            return Objects.equals(type, primitive.type)&&
                    Objects.equals(value, primitive.value);
        }
        @Override
        public int hashCode() {
            return Objects.hash(type,value);
        }
    }
    public static class StringValue extends Value{
        final String value;
        private StringValue(String value) {
            super(Type.Primitive.STRING);
            this.value=value;
            getters.put(Type.FIELD_NAME_LENGTH,
                    ()->createPrimitive(Type.Numeric.UINT64,value.length()));
        }
        @Override
        public Value independentCopy(int ignoredId){
            if(isBoundExcept(ignoredId)) {
                return createPrimitive(Type.Primitive.STRING, value);
            }else{
                return this;
            }
        }
        @Override
        public Value castTo(Type t) {
            if(t== Type.Primitive.STRING||t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else if(t instanceof Type.Array&&
                    Type.canCast(((Type.Array)t).content, Type.Numeric.UINT8,null)){
                //TODO cast String to Array
                throw new UnsupportedOperationException("Unimplemented");
            }else{
                throw new TypeError("Cannot cast "+Type.Primitive.STRING+" to "+t);
            }
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StringValue)) return false;
            StringValue that = (StringValue) o;
            return Objects.equals(value, that.value);
        }
        @Override
        public int hashCode() {
            return value.hashCode();
        }
        @Override
        protected String valueToString() {
            return value;
        }

        @Override
        public Value getAtIndex(Value index) {
            //TODO String.getAtIndex
            throw new UnsupportedOperationException("Unimplemented");
        }
        @Override
        public Value setAtIndex(Value index, Value value) {
            //TODO String.setAtIndex
            throw new UnsupportedOperationException("Unimplemented");
        }

        @Override
        public Value getRange(Value off, Value to) {
            //TODO String.getRange
            throw new UnsupportedOperationException("Unimplemented");
        }

        @Override
        public Value setRange(Value off, Value to, Value value) {
            //TODO String.setRange
            throw new UnsupportedOperationException("Unimplemented");
        }
    }

    //addLater? primitiveArrays

    //TODO separate Arrays and Structs
    public static class Array extends Value{
        final Value[] elements;

        private static Type typeFromElements(Value[] elements) {
            Type type=Type.EMPTY_TYPE;
            for (Value element : elements) {
                type = Type.commonSupertype(type, element.type);
            }
            return new Type.Array(type);
        }
        public Array(Value[] elements) {
            this(typeFromElements(elements),elements);
        }
        public Array(Type type,Value[] elements) {
            super(type);
            this.elements=elements;
            getters.put(Type.FIELD_NAME_LENGTH,()->createPrimitive(Type.Numeric.UINT64,
                    elements.length));
        }

        @Override
        public void bind(int id) {
            super.bind(id);
            for(Value v:elements){
                v.bind(id);
            }
        }
        @Override
        public boolean isBound() {
            for(Value v:elements){
                if(v.isBound())
                    return true;
            }
            return super.isBound();
        }
        @Override
        public boolean isBoundExcept(int ignoredId) {
            for(Value v:elements){
                if(v.isBoundExcept(ignoredId))
                    return true;
            }
            return super.isBoundExcept(ignoredId);
        }
        @Override
        public Value independentCopy(int ignoredId){
            if(isBoundExcept(ignoredId)){
                Value[] newElements=new Value[elements.length];
                for(int i=0;i< newElements.length;i++){
                    newElements[i]=elements[i].independentCopy(ignoredId);
                }
                return new Array(newElements);
            }else{
                return this;
            }
        }

        @Override
        public Array castTo(Type t) {
            if(t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else if(t instanceof Type.Array){
                if(Type.canCast(((Type.Array) t).content,
                    ((Type.Array)type).content,null)){
                    //addLater in-place calculation if possible
                    Value[] newElements=new Value[elements.length];
                    for(int i=0;i<elements.length;i++){
                        newElements[i]=elements[i].
                                castTo(((Type.Array) t).content);
                    }
                    return new Array(t,newElements);
                }else{
                    throw new TypeError("Cannot cast type:"+type+ " to "+t);
                }
            }
            //addLater? uint8[] -> String
            throw new TypeError("Cannot cast "+type+" to "+t);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Struct)) return false;
            Struct struct = (Struct) o;
            return Objects.equals(type,struct.type)&&
                    Arrays.equals(elements, struct.elements);
        }
        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + Arrays.hashCode(elements);
            return result;
        }
        @Override
        protected String valueToString() {
            StringBuilder str=new StringBuilder("{");
            for(int i=0;i< elements.length;i++){
                if(i>0){
                    str.append(',');
                }
                str.append(elements[i].valueToString());
            }
            return str.append('}').toString();
        }

        @Override
        public Value getAtIndex(Value index) {
            //long since indices are internally uint64
            long lIndex=(Long)((NumericValue)index.castTo(Type.Numeric.UINT64)).value;
            if(lIndex<0||lIndex>=elements.length){
                throw new NoRetRuntimeError("index out of Bounds:"+lIndex);
            }else{
                return elements[(int)lIndex];
            }
        }
        @Override
        public Array setAtIndex(Value index, Value value) {
            //long since indices are internally uint64
            long lIndex=(Long)((NumericValue)index.castTo(Type.Numeric.UINT64)).value;
            if(lIndex<0||lIndex>=elements.length){
                throw new NoRetRuntimeError("index out of Bounds:"+lIndex);
            }else{
                //TODO create independent copy if necessary
                elements[(int)lIndex]=value.castTo(((Type.Array)type).content);
                return this;
            }
        }

        @Override
        public Value getRange(Value off, Value to) {
            //TODO Array.getRange
            throw new UnsupportedOperationException("unimplemented");
        }

        @Override
        public Array setRange(Value off, Value to, Value value) {
            //TODO Array.setRange
            throw new UnsupportedOperationException("unimplemented");
        }
    }
    public static class Struct extends Value{
        //TODO replace with Map name->element
        final Value[] elements;
        final String[] names;
        private static Type typeFromElements(Value[] elements,String[] names) {
            Type[] types=new Type[elements.length];
            for(int i=0;i<types.length;i++){
                types[i]=elements[i].type;
            }
            return new Type.Struct(types,names);
        }
        public Struct(Value[] elements,String[] names) {
            super(typeFromElements(elements,names));
            this.elements=elements;
            this.names=names;
            initGetters();
            initSetters();
        }
        private void initGetters() {
            for(int i=0;i<names.length;i++){
                int j=i;
                getters.put(names[i],()->elements[j]);
            }
        }
        private void initSetters() {
            for(int i=0;i<names.length;i++){
                int j=i;
                setters.put(names[i],(v)-> {
                    //TODO! create independent copy  of Struct if necessary
                    elements[j]=v;
                    return this;
                });
            }
        }
        @Override
        public void bind(int id) {
            super.bind(id);
            for(Value v:elements){
                v.bind(id);
            }
        }
        @Override
        public boolean isBound() {
            for(Value v:elements){
                if(v.isBound())
                    return true;
            }
            return super.isBound();
        }
        @Override
        public boolean isBoundExcept(int ignoredId) {
            for(Value v:elements){
                if(v.isBoundExcept(ignoredId))
                    return true;
            }
            return super.isBoundExcept(ignoredId);
        }
        @Override
        public Value independentCopy(int ignoredId){
            if(isBoundExcept(ignoredId)){
                Value[] newElements=new Value[elements.length];
                for(int i=0;i< newElements.length;i++){
                    newElements[i]=elements[i].independentCopy(ignoredId);
                }
                return new Struct(newElements,names);
            }else{
                return this;
            }
        }

        @Override
        public Struct castTo(Type t) {
            if(t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else if(t instanceof Type.Struct){
                if(Type.canCast(t,type,null)){
                    //addLater in-place calculation if possible
                    Value[] newElements=new Value[elements.length];
                    for(int i=0;i<names.length;i++){
                        newElements[i]=elements[i].castTo(t.fields.get(names[i]));
                    }
                    return new Struct(newElements,names);
                }else{
                    throw new TypeError("Cannot cast type:"+type+ " to "+t);
                }
            }
            throw new UnsupportedOperationException("Unimplemented");
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Struct)) return false;
            Struct struct = (Struct) o;
            return Objects.equals(type,struct.type)&&
                    Arrays.equals(elements, struct.elements);
        }
        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + Arrays.hashCode(elements);
            return result;
        }
        @Override
        protected String valueToString() {
            StringBuilder str=new StringBuilder("{");
            for(int i=0;i< elements.length;i++){
                if(i>0){
                    str.append(',');
                }
                if(names[i]!=null){
                    str.append('.').append(names[i]).append('=');
                }
                str.append(elements[i].valueToString());
            }
            return str.append('}').toString();
        }

    }

}
