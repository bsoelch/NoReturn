package bsoelch.noret.lang;

import bsoelch.noret.NoRetRuntimeError;
import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class Value{

    public interface ContainerValue extends Iterable<Value>{

    }

    public static class AnyValue extends Value implements ContainerValue{
        public final Value content;
        public AnyValue(Value content) {
            super(Type.ANY);
            this.content=content;
        }

        @Override
        public boolean isMutable() {
            return true;
        }

        @Override
        public String stringRepresentation() {
            return content.stringRepresentation();
        }

        @Override
        public Value castTo(Type t) {
            if(Type.canCast(t,content.type,null)){
                return content.castTo(t);
            }else{
                return super.castTo(t);
            }
        }

        @Override
        public Iterator<Value> iterator() {
            return Stream.of(content).iterator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyValue anyValue = (AnyValue) o;
            return Objects.equals(content, anyValue.content);
        }
        @Override
        public int hashCode() {
            return Objects.hash(content);
        }
    }

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
            if(t instanceof Type.Optional){
                return new Optional((Type.Optional) t,this);
            }else{
                return super.castTo(t);
            }
        }

        @Override
        public String stringRepresentation() {
            return "none";
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

    public Value castTo(Type t) {
        if(t==type){
            return this;
        }else if(t== Type.ANY||t instanceof Type.Generic){
            return type==Type.ANY?this:new AnyValue(this);
        }else if(t instanceof Type.Optional&&Type.canCast(((Type.Optional) t).content,type,null)){
            return new Optional((Type.Optional)t,castTo(((Type.Optional) t).content));
        }else if(t instanceof Type.Union){
            if(Type.canCast(t,type,null)){
                return new Union((Type.Union)t,this);
            }
        }else if(t instanceof Type.NoRetString){
            //TODO cast to string..
            throw  new UnsupportedOperationException("unimplemented");
        }
        throw new TypeError("Cannot cast \""+type+"\" to \""+t+"\"");
    }

    /**A String representation of this value (used by print)*/
    public abstract String stringRepresentation();
    /**Converts this value to a String (used for casts to string)*/
    public String stringValue() {
        return stringRepresentation();
    }

    @Override
    public String toString() {
        return stringRepresentation();
    }

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
        if(type== Type.ANY){
            throw new TypeError("Cannot create instances of Type \""+Type.ANY+"\"");
        }
        if(type instanceof Type.Numeric){
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
        public String stringRepresentation() {
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
        public Value castTo(Type t) {
            if(t instanceof Type.Numeric){
                if(((Type.Numeric) t).isFloat){
                    switch (((Type.Numeric) t).level){
                        case 2:
                            return createPrimitive((Type.Numeric)t,
                                    ((Number)value).floatValue());
                        case 3:
                            return createPrimitive((Type.Numeric)t,
                                    ((Number)value).doubleValue());
                        default:
                            throw new SyntaxError("exceeded maximum number capacity");
                    }
                }else{
                    switch (((Type.Numeric) t).level){
                        case 0:
                            return createPrimitive((Type.Numeric)t,
                                    ((Number)value).byteValue());
                        case 1:
                            return createPrimitive((Type.Numeric)t,
                                    ((Number)value).shortValue());
                        case 2:
                            return createPrimitive((Type.Numeric)t,
                                    ((Number)value).intValue());
                        case 3:
                            return createPrimitive((Type.Numeric)t,
                                    ((Number)value).longValue());
                        default:
                            throw new SyntaxError("exceeded maximum number capacity");
                    }
                }
            }else {
                return super.castTo(t);
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
    public static StringValue createString(Type.NoRetString stringType,String value){
        return new StringValue(stringType, value);
    }
    public static class StringValue extends Value implements Comparable<StringValue>,ContainerValue{
        final byte[] utf8Bytes;
        final String utf16String;
        final int[]  utf32Codepoints;
        private StringValue(Type.NoRetString stringType,String value) {
            super(stringType);
            this.utf8Bytes=value.getBytes(StandardCharsets.UTF_8);
            this.utf16String=value;
            this.utf32Codepoints=value.codePoints().toArray();
            if(stringType== Type.NoRetString.STRING8){
                getters.put(Type.FIELD_NAME_LENGTH,()->createPrimitive(Type.Numeric.UINT64,utf8Bytes.length));
            }else if(stringType== Type.NoRetString.STRING16){
                getters.put(Type.FIELD_NAME_LENGTH,()->createPrimitive(Type.Numeric.UINT64,utf16String.length()));
            }else if(stringType== Type.NoRetString.STRING32){
                getters.put(Type.FIELD_NAME_LENGTH,()->createPrimitive(Type.Numeric.UINT64,utf32Codepoints.length));
            }else{
                assert false;//"Unreachable"
            }

        }

        @Override
        public Value independentCopy(){
            return new StringValue((Type.NoRetString)type, new String(utf8Bytes,StandardCharsets.UTF_8));
        }
        @Override
        public Value castTo(Type t) {
            //TODO casting between stringTypes, casting of string to array
            return super.castTo(t);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StringValue)) return false;
            StringValue that = (StringValue) o;
            return stringValue().equals(that.stringValue());
        }
        @Override
        public int hashCode() {
            return utf16String.hashCode();
        }

        public String stringValue() {
            return utf16String;
        }
        @Override
        public String stringRepresentation() {
            return "\""+stringValue()+"\"";
        }

        @Override
        public int compareTo(StringValue o) {
            return utf16String.compareTo(o.utf16String);
        }

        @Override
        public Value getAtIndex(Value index) {
            long lIndex=(Long)((NumericValue)index.castTo(Type.Numeric.UINT64)).value;
            if(type== Type.NoRetString.STRING8){
                if(lIndex<0||lIndex>= utf8Bytes.length){
                    throw new SyntaxError("String index out of range:"+lIndex+" length:"+lIndex);
                }
                return Value.createPrimitive(Type.Numeric.UINT8,utf8Bytes[(int)lIndex]);
            }else if(type== Type.NoRetString.STRING16){
                if(lIndex<0||lIndex>= utf16String.length()){
                    throw new SyntaxError("String index out of range:"+lIndex+" length:"+lIndex);
                }
                return Value.createPrimitive(Type.Numeric.UINT16,utf16String.charAt((int)lIndex));
            }else if(type== Type.NoRetString.STRING32){
                if(lIndex<0||lIndex>= utf32Codepoints.length){
                    throw new SyntaxError("String index out of range:"+lIndex+" length:"+lIndex);
                }
                return Value.createPrimitive(Type.Numeric.UINT32,utf32Codepoints[(int)lIndex]);
            }else{
                assert false;//"Unreachable"
                throw new RuntimeException("Unreachable");
            }
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

        public byte[] utf8Bytes() {
            return Arrays.copyOf(utf8Bytes,utf8Bytes.length);
        }
        public char[] chars() {
            return utf16String.toCharArray();
        }
        public int[] codePoints() {
            return Arrays.copyOf(utf32Codepoints,utf32Codepoints.length);
        }

        @Override
        public Iterator<Value> iterator() {
            if(type== Type.NoRetString.STRING8){
                return IntStream.range(0, utf8Bytes.length).mapToObj(i->createPrimitive(Type.Numeric.UINT8,utf8Bytes[i])).iterator();
            }else if(type== Type.NoRetString.STRING16){
                return IntStream.range(0, utf16String.length()).mapToObj(i->createPrimitive(Type.Numeric.UINT16,utf16String.charAt(i))).iterator();
            }else if(type== Type.NoRetString.STRING32){
                return Arrays.stream(utf32Codepoints).mapToObj(i->createPrimitive(Type.Numeric.UINT32,i)).iterator();
            }else{
                assert false;//"Unreachable"
                throw new RuntimeException("Unreachable");
            }
        }
    }

    public static class Optional extends Value implements ContainerValue{
        public final Value content;

        public Optional(Type.Optional type,Value content) {
            super(type);
            if(content==NONE){
                this.content=NONE;
            }else if(Type.canAssign(type.content,content.type,null)){
                this.content=content.castTo(type.content);
            }else{
                throw new IllegalArgumentException("Cannot assign "+content.type+" to "+type);
            }
            getters.put("value",()->{
                if(content==NONE){
                    throw new SyntaxError("cannot unpack empty optional");
                }
                return content;
            });
        }

        @Override
        public boolean isMutable() {
            return true;
        }

        @Override
        public Value castTo(Type t) {
            if(t==Type.Primitive.BOOL){
                return createPrimitive(Type.Primitive.BOOL,content!=NONE);
            }else if (t instanceof Type.Optional&&
                    Type.canCast((((Type.Optional)t).content),(((Type.Optional)type).content),null)){
                return new Optional((Type.Optional) t,content==NONE?NONE:content.castTo(((Type.Optional)t).content));
            }else{
                return super.castTo(t);
            }
        }

        @Override
        public String stringRepresentation() {
            return "Optional:{"+(content==NONE?"":content.stringRepresentation())+"}";
        }
        @Override
        public String stringValue() {
            return content.stringValue();
        }

        @Override
        public Iterator<Value> iterator() {
            return Stream.of(content).iterator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Optional optional = (Optional) o;
            return Objects.equals(content, optional.content);
        }
        @Override
        public int hashCode() {
            return Objects.hash(content);
        }
    }

    /**stores either an array or a tuple*/
    public static class ArrayOrTuple extends Value implements ContainerValue{
        final Value[] elements;
        private static Type typeFromElements(Value[] elements,boolean asArray) {
            if(asArray){
                return new Type.Array(Stream.of(elements).map(Value::getType).reduce(Type.EMPTY_TYPE,Type::commonSupertype));
            }else{
                return new Type.Tuple(null,Stream.of(elements).map(Value::getType).toArray(Type[]::new));
            }
        }
        public ArrayOrTuple(Value[] elements,boolean asArray) {
            this(typeFromElements(elements,asArray),elements);
        }
        public ArrayOrTuple(Type type, Value[] elements) {
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
            return new ArrayOrTuple(type,newElements);
        }

        @Override
        public Value castTo(Type t) {
            if(type instanceof Type.Array){
                if(t instanceof Type.Array){
                    Value[] newElements=new Value[elements.length];
                    for(int i=0;i<elements.length;i++){
                        newElements[i]=elements[i].
                                castTo(((Type.Array) t).content);
                    }
                    return new ArrayOrTuple(t,newElements);
                }else if(t instanceof Type.Tuple){
                    Value[] newElements=new Value[elements.length];
                    for(int i=0;i<elements.length;i++){
                        newElements[i]=elements[i].castTo(((Type.Tuple) t).elements[i]);
                    }
                    return new ArrayOrTuple(t,newElements);
                }
            }else if(type instanceof Type.Tuple){
                if(t instanceof Type.Array){
                    Value[] newElements=new Value[elements.length];
                    for(int i=0;i<elements.length;i++){
                        newElements[i]=elements[i].castTo(((Type.Array) t).content);
                    }
                    return new ArrayOrTuple(t,newElements);
                }else if(t instanceof Type.Tuple){
                    if(((Type.Tuple) t).elements.length==((Type.Tuple) type).elements.length){
                        Value[] newElements=new Value[elements.length];
                        for(int i=0;i<elements.length;i++){
                            newElements[i]=elements[i].castTo(((Type.Tuple) t).elements[i]);
                        }
                        return new ArrayOrTuple(t,newElements);
                    }
                }
            }
            //addLater? Array -> String (utf8,utf16,utf32)
            return super.castTo(t);
        }

        @Override
        public Iterator<Value> iterator() {
            return Stream.of(elements).iterator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ArrayOrTuple)) return false;
            ArrayOrTuple array = (ArrayOrTuple) o;
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
        public String stringRepresentation() {
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
        public ArrayOrTuple setAtIndex(Value index, Value value) {
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
        public ArrayOrTuple setRange(Value off, Value to, Value value) {
            //TODO Array.setRange
            throw new UnsupportedOperationException("unimplemented");
        }
        @Override
        public boolean isMutable() {return true;}

        public Value[] elements() {
            return elements;
        }
    }

    public static class Struct extends Value implements ContainerValue{
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
            //TODO type-caching
            return new Type.Struct(null,types,names);
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
        public Value castTo(Type t) {
            if(t instanceof Type.Struct){
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
            }else{
                return super.castTo(t);
            }
        }

        @Override
        public Iterator<Value> iterator() {
            return Arrays.stream(((Type.Struct)type).fieldNames).map(elements::get).iterator();
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
        public String stringRepresentation() {
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

    public static class Union extends Value implements ContainerValue{
        Value content;

        public Union(Type.Union type,Value content) {
            super(type);
            this.content=content;
            initGetters();
            initSetters();
        }

        //TODO handle different field types (raw type casting)
        private void initGetters() {
            for(String name:((Type.Union)type).fieldNames){
                getters.put(name,()->content);
            }
        }
        private void initSetters() {
            for(String name:((Type.Union)type).fieldNames){
                setters.put(name,(v)-> {
                    content=v;
                    return this;
                });
            }
        }

        @Override
        public Value castTo(Type t) {
            if(t instanceof Type.Union&&Type.canCast(t,type,null)){
                return new Union((Type.Union) t,content);
            }
            return super.castTo(t);
        }

        @Override
        public Value independentCopy(){
            return new Union((Type.Union) type, content);
        }

        @Override
        public Iterator<Value> iterator() {
            return Stream.of(content).iterator();
        }

        @Override
        public boolean isMutable() {
            return true;
        }

        @Override
        public String stringRepresentation() {
            return "union{"+content+"}";//addLater? .as...=
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Union union = (Union) o;
            return Objects.equals(type, union.type)&&Objects.equals(content, union.content);
        }
        @Override
        public int hashCode() {
            return Objects.hash(type,content);
        }
    }

    public static class TypeValue extends Value {
        public final Type value;
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
        public String stringRepresentation() {
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
