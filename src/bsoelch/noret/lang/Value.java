package bsoelch.noret.lang;

import bsoelch.noret.NoRetRuntimeError;
import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Value{

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
        protected String stringRepresentation() {
            return "none";
        }
        @Override
        public boolean isMutable() {return false;}
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
        protected String stringRepresentation() {
            return "NOP";
        }
        @Override
        public boolean isMutable() {return false;}
    };
    public static final Value TRUE = createPrimitive(Type.Primitive.BOOL,true) ;
    public static final Value FALSE = createPrimitive(Type.Primitive.BOOL,false) ;

    public abstract boolean isMutable();

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

    public Value(Type type){
        this.type=type;
        getters.put(Type.FIELD_NAME_TYPE,()->new TypeValue(type));
    }
    public final Type getType() {
        return type;
    }

    public Value independentCopy(){
        return this;
    }

    public abstract Value castTo(Type t);
    protected abstract String stringRepresentation();

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
        protected String stringRepresentation() {
            return value.toString();
        }
        @Override
        public boolean isMutable() {return false;}
    }
    public static class NumericValue extends Primitive{
        private NumericValue(Type.Numeric type, Object value) {
            super(type,value);
        }

        @Override
        public NumericValue castTo(Type t) {
            if(t==type||t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
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
    public static class StringValue extends Value implements Comparable<StringValue>{
        final byte[] utf8Bytes;
        private StringValue(String value) {
            super(Type.Primitive.STRING);
            this.utf8Bytes=value.getBytes(StandardCharsets.UTF_8);
            getters.put(Type.FIELD_NAME_LENGTH,
                    ()->createPrimitive(Type.Numeric.UINT64,value.length()));
        }

        @Override
        public Value independentCopy(){
            return createPrimitive(Type.Primitive.STRING, new String(utf8Bytes,StandardCharsets.UTF_8));
        }
        @Override
        public Value castTo(Type t) {
            if(t== Type.Primitive.STRING||t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else if(t instanceof Type.Array&&
                    Type.canCast(((Type.Array)t).content, Type.Numeric.UINT8,null)){
                //TODO cast String to Array (uint8 -> UTF8, uint16 -> UTF16, uint32 -> UTF32)
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
            return Arrays.equals(utf8Bytes, that.utf8Bytes);
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(utf8Bytes);
        }

        public String stringValue() {
            return new String(utf8Bytes,StandardCharsets.UTF_8);
        }
        @Override
        protected String stringRepresentation() {
            return "\""+stringValue()+"\"";
        }

        @Override
        public int compareTo(StringValue o) {
            //TODO more effective Method?
            return new String(utf8Bytes).compareTo(new String(o.utf8Bytes));
        }

        @Override
        public Value getAtIndex(Value index) {
            //TODO? getAtIndex -> Codepoints
            // allow setting indices to non-unicode characters "Hello World"[1]='é'
            long lIndex=(Long)((NumericValue)index.castTo(Type.Numeric.UINT64)).value;
            if(lIndex<0||lIndex>= utf8Bytes.length){
                throw new SyntaxError("String index out of range:"+lIndex+" length:"+lIndex);
            }
            return Value.createPrimitive(Type.Numeric.UINT8,utf8Bytes[(int)lIndex]);
        }
        @Override
        public Value setAtIndex(Value index, Value newValue) {
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
        @Override
        public boolean isMutable() {return true;}
    }

    //addLater? primitiveArrays

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
        public Value independentCopy(){
            Value[] newElements=new Value[elements.length];
            for(int i=0;i< newElements.length;i++){
                newElements[i]=elements[i].independentCopy();
            }
            return new Array(newElements);
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
            //addLater? Array -> String (utf8,utf16,utf32)
            throw new TypeError("Cannot cast "+type+" to "+t);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Array)) return false;
            Array array = (Array) o;
            return Objects.equals(type,array.type)&&
                    Arrays.equals(elements, array.elements);
        }
        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + Arrays.hashCode(elements);
            return result;
        }
        @Override
        protected String stringRepresentation() {
            StringBuilder str=new StringBuilder("{");
            for(int i=0;i< elements.length;i++){
                if(i>0){
                    str.append(',');
                }
                str.append(elements[i].stringRepresentation());
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
        @Override
        public boolean isMutable() {return true;}
    }
    public static class Struct extends Value{
        final HashMap<String,Value> elements;

        private Struct(Type.Struct type,HashMap<String,Value> elements) {
            super(type);
            this.elements=elements;
            initGetters();
            initSetters();
        }
        private static Type typeFromElements(Value[] elements,String[] names) {
            Type[] types=new Type[elements.length];
            for(int i=0;i<types.length;i++){
                types[i]=elements[i].type;
            }
            return new Type.Struct(types,names);
        }
        public Struct(Value[] elements,String[] names) {
            super(typeFromElements(elements,names));
            this.elements=new HashMap<>(elements.length);
            for(int i=0;i<elements.length;i++){
                if(this.elements.put(names[i],elements[i])!=null){
                    throw new SyntaxError("Duplicate name in Struct:\""+names[i]+"\"");
                }
            }
            initGetters();
            initSetters();
        }
        private void initGetters() {
            for(String name:elements.keySet()){
                getters.put(name,()->elements.get(name));
            }
        }
        private void initSetters() {
            for(String name:elements.keySet()){
                setters.put(name,(v)->elements.put(name,v));
            }
        }
        @Override
        public Value independentCopy(){
            HashMap<String,Value> newElements=new HashMap<>(elements.size());
            for(Map.Entry<String, Value> e:elements.entrySet()){
                newElements.put(e.getKey(),e.getValue().independentCopy());
            }
            return new Struct((Type.Struct) type, newElements);
        }

        @Override
        public Struct castTo(Type t) {
            if(t==Type.Primitive.ANY||t instanceof Type.Generic){
                return this;
            }else if(t instanceof Type.Struct){
                if(Type.canCast(t,type,null)){
                    //addLater in-place calculation if possible
                    HashMap<String,Value> newElements=new HashMap<>(elements.size());
                    for(Map.Entry<String, Value> e:elements.entrySet()){
                        newElements.put(e.getKey(),e.getValue().castTo(t.fields.get(e.getKey())));
                    }
                    return new Struct((Type.Struct) t,newElements);
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
            return Objects.equals(type,struct.type)&&elements.equals(struct.elements);
        }
        @Override
        public int hashCode() {
            return Objects.hash(type,elements);
        }
        @Override
        protected String stringRepresentation() {
            StringBuilder str=new StringBuilder("{");
            for(Map.Entry<String, Value> e:elements.entrySet()){
                if(str.length()>1){
                    str.append(',');
                }
                str.append('.').append(e.getKey()).append('=')
                        .append(e.getValue().stringRepresentation());
            }
            return str.append('}').toString();
        }
        @Override
        public boolean isMutable() {return true;}
    }

    private static class TypeValue extends Value {
        final Type value;
        public TypeValue(Type type) {
            super(Type.TYPE);
            this.value=type;
            getters.put("isArray",     ()->createPrimitive(Type.Primitive.BOOL,value instanceof Type.Array    ));
            getters.put("isStruct",    ()->createPrimitive(Type.Primitive.BOOL,value instanceof Type.Struct   ));
            getters.put("isOptional",  ()->createPrimitive(Type.Primitive.BOOL,value instanceof Type.Optional ));
            getters.put("isReference", ()->createPrimitive(Type.Primitive.BOOL,value instanceof Type.Reference));
            getters.put("contentType", this::contentType);
        }

        private Value contentType() {
            if(value instanceof Type.Array){
                return new TypeValue(((Type.Array) value).content);
            }else  if(value instanceof Type.Optional){
                return new TypeValue(((Type.Optional) value).content);
            }else  if(value instanceof Type.Reference){
                return new TypeValue(((Type.Reference) value).content);
            }else{
                return new TypeValue(Type.EMPTY_TYPE);
            }
        }

        @Override
        public boolean isMutable() {
            return false;
        }

        @Override
        public Value castTo(Type t) {
            if(t==Type.TYPE||t==Type.Primitive.ANY){
                return this;
            }else{
                throw new IllegalArgumentException("Cannot cast "+type+" to "+t);
            }
        }

        @Override
        protected String stringRepresentation() {
            return value.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeValue typeValue = (TypeValue) o;
            return Objects.equals(value, typeValue.value);
        }
    }
}
