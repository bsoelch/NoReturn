package bsoelch.noret.lang;

import bsoelch.noret.TypeError;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Type {
    /*queue for storing all Types that are declared before TYPE is defined,
    *  to allow all types to have a .type field without the need for a specific class-loading order*/
    private static final ArrayDeque<Type> waitingForTypeType=new ArrayDeque<>();

    public static final String FIELD_NAME_TYPE = "type";
    public static final String FIELD_NAME_LENGTH = "length";

    public static final String FIELD_NAME_VALUE = "value";
    public static final String FIELDS_PROC_TYPES = "argTypes";

    public Iterable<Type> childTypes() {
        return Collections.emptySet();
    }

    private static final class TypeType extends Type{
        private TypeType() {
            super("type", 1, false, true);
            fields.put("isArray",     Primitive.BOOL);
            fields.put("isStruct",    Primitive.BOOL);
            fields.put("isOptional",  Primitive.BOOL);
            fields.put("isReference", Primitive.BOOL);
            fields.put("contentType",           this);
            //addLater?  .fields field
            synchronized (waitingForTypeType){
                for(Type t:waitingForTypeType){
                    t.fields.put(FIELD_NAME_TYPE,this);
                }
            }
        }
    }
    /** Generics types in NoRet allow forcing the calle of a procedure to use the same type for two different arguments,
     *      * they allow signatures like ($a,($a)=>?)=>? that make it easier to call a restricted function
     * */
    public static class AnyType extends Type{
        /**default value of type any, for storing non-generic values*/
        public static final AnyType ANY=new AnyType(null);

        public final String genericName;
        public AnyType(String name) {
            super("any", 2, true, true);
            genericName=name==null?null:"$"+name;
        }
        @Override
        public String toString() {
            return genericName!=null?("Generic: "+genericName):super.toString();
        }
    }
    /**Type of NONE Value, assignable to any reference*/
    public static final Type TYPE = new TypeType();
    /**Type of NONE Value, assignable to any reference*/
    public static final Type NONE_TYPE = new Type("\"none\"", 1, false, true);

    public static class Primitive extends Type{
        private static final TreeMap<String,Primitive> primitives=new TreeMap<>();

        public final int byteCount;

        public static final Primitive BOOL      = new Primitive("bool",false, 1);

        private Primitive(String name, boolean varSize, int byteCount){
            super(name, 1, varSize, true);
            this.byteCount = byteCount;
            if(primitives.put(name,this)!=null){
                throw new RuntimeException("The primitive \""+name+"\" already exists");
            }
        }

        public static Collection<Primitive> types(){
            ArrayList<Primitive> ret = new ArrayList<>(Primitive.primitives.values());
            ret.sort(Comparator.comparingInt(a -> a.byteCount));
            return ret;
        }
    }
    public static class Numeric extends Primitive{
        public final int level;
        public final boolean signed,isFloat;
        public final boolean isChar;

        public static final Numeric INT8      = new Numeric("int8",0,true,false,false);
        public static final Numeric UINT8     = new Numeric("uint8",0,false,false,false);
        public static final Numeric INT16     = new Numeric("int16",1,true,false,false);
        public static final Numeric UINT16    = new Numeric("uint16",1,false,false,false);
        public static final Numeric INT32     = new Numeric("int32",2,true,false,false);
        public static final Numeric UINT32    = new Numeric("uint32",2,false,false,false);
        public static final Numeric INT64     = new Numeric("int64",3,true,false,false);
        public static final Numeric UINT64    = new Numeric("uint64",3,false,false,false);

        public static final Numeric CHAR8     = new Numeric("char8",0,false,false,true);
        public static final Numeric CHAR16    = new Numeric("char16",1,false,false,true);
        public static final Numeric CHAR32    = new Numeric("char32",2,false,false,true);

        public static final Numeric FLOAT32   = new Numeric("float32",2,true,true,false);
        public static final Numeric FLOAT64   = new Numeric("float64",3,true,true,false);

        /**method for ensuring this class (with all Numeric-Type constants) is loaded*/
        static void ensureInitialized(){}

        private Numeric(String name,int level,boolean signed,boolean isFloat,boolean isChar) {
            super(name,false, 1<<level);
            this.level=level;
            this.signed=signed;
            this.isFloat=isFloat;
            this.isChar=isChar;
        }

        public int bitSize(){
            return 8*byteCount;
        }
    }
    public static class NoRetString extends Type{
        private static final HashMap<String,Type> stringTypes=new HashMap<>();
        //addLater? nativeString (string with native encoding) ? nonUnicodeStrings
        /**UTF-8 string*/
        public static final NoRetString STRING8=new NoRetString(8,Numeric.CHAR8);
        /**UTF-16 string*/
        public static final NoRetString STRING16=new NoRetString(16,Numeric.CHAR16);
        /**UTF-32 string*/
        public static final NoRetString STRING32=new NoRetString(32,Numeric.CHAR32);

        public final int charBits;
        public final Type.Numeric charType;

        private NoRetString(int charBits, Type.Numeric charType) {
            super("string"+ charBits,1,true, true);
            this.charBits = charBits;
            this.charType=charType;
            fields.put(FIELD_NAME_LENGTH,Numeric.UINT64);
            stringTypes.put(name,this);
        }

        /* addLater use in StringConcat to choose the correct type of string
        public static NoRetString sumType(NoRetString lType, NoRetString rType) {
            return lType.charBits >=rType.charBits ?lType:rType;
        }*/
    }
    public static void addAtomics(Map<String,Type> typeNames) {
        Numeric.ensureInitialized();
        typeNames.put(AnyType.ANY.name,AnyType.ANY);
        typeNames.putAll(Primitive.primitives);
        typeNames.putAll(NoRetString.stringTypes);
    }

    final String name;
    final HashMap<String,Type> fields=new HashMap<>();
    /**number of blocks needed (in compiled code) to store a value of this type*/
    public final int blockCount;
    /**true if values of this type have a variable encoding size*/
    public final boolean varSize;
    /**true if this type is no composition of smaller types*/
    public final boolean isAtomic;

    private Type(String name, int blockCount, boolean varSize, boolean isAtomic){
        this.name=name;
        this.blockCount = blockCount;
        this.varSize = varSize;
        this.isAtomic = isAtomic;
        synchronized (waitingForTypeType) {
            if(TYPE!=null){
                fields.put(FIELD_NAME_TYPE,TYPE);
            }else {
                waitingForTypeType.add(this);
            }
        }
    }
    String wrappedName(){
        return name;
    }
    public Type getField(String fieldName) {
        return fields.get(fieldName);
    }

    public boolean isMutableFlied(String fieldName) {
        return false;
    }

    @Override
    public String toString() {
        return "Type:"+name;
    }

    public static Type commonSupertype(Type t1, Type t2){
        if(canAssign(t1,t2,null)){
            return t1;
        }else if(canAssign(t2,t1,null)){
            return t2;
        }else if(t1 == NONE_TYPE){//none+optional -> optional
            if(t2 instanceof Optional){
                return t2;
            }else{
                return new Optional(t2);
            }
        }else if(t2==NONE_TYPE){
            if(t1 instanceof Optional){
                return t1;
            }else{
                return new Optional(t1);
            }
        }
        //addLater better calculation for common supertype ? use union
        return AnyType.ANY;
    }

    private static boolean canAssign(Type to, Type from, boolean allowCast, HashMap<String, GenericBound> generics){
        if(to==from||to.equals(from))
            return true;
        if(from instanceof Numeric&&to instanceof Numeric){
            return allowCast||(((Numeric) from).level<((Numeric) to).level||
                    (((Numeric) from).level==((Numeric) to).level&&
                        ((Numeric) from).signed==((Numeric) to).signed&&
                        ((Numeric) from).isFloat==((Numeric) to).isFloat))&&
                    ((!((Numeric) from).isFloat)||((Numeric) to).isFloat);
        }
        //A[]->B[] iff A->B
        //{A0,..,AN}->B[] iff A_i -> B for all i
        if(to instanceof Array){
            if(from instanceof Array){
                return canAssign(((Array) to).content,((Array) from).content,allowCast, generics);
            }else if(from instanceof Tuple){
                return Stream.of(((Tuple) from).elements).allMatch(e->canAssign(((Array) to).content,e,allowCast,generics));
            }
        }else if(to instanceof Tuple){
            if(from instanceof Array){
                return Stream.of(((Tuple) to).elements).allMatch(e->canAssign(e,((Array) from).content,allowCast,generics));
            }else if(from instanceof Tuple){
                if(((Tuple) to).elements.length!=((Tuple) from).elements.length)
                    return false;
                return IntStream.range(0,((Tuple) to).elements.length)
                        .allMatch(i->canAssign(((Tuple) to).elements[i],((Tuple) from).elements[i],allowCast, generics));
            }
        }
        //A? -> bool , A? -> B? iff A->B
        if(from instanceof Optional){
            if(to==Primitive.BOOL){
                return true;
            }else if(to instanceof Optional){
                return canAssign(((Optional) to).content,((Optional) from).content,allowCast, generics);
            }
        }else if(to instanceof Optional){
            //A -> B? iff A->B
            return (from == NONE_TYPE)||canAssign(((Optional) to).content,from,allowCast, generics);
        }
        //A -> @B,@A->B,@A->@B iff A->B
        if(from instanceof Reference){
            if(to instanceof Reference){
                return canAssign(((Reference) to).content,((Reference) from).content,allowCast, generics);
            }else if(allowCast){
                return canAssign(to,((Reference) from).content,true, generics);
            }
        }else if(allowCast&&to instanceof Reference){
            return canAssign(((Reference) to).content,from,true, generics);
        }
        //union{A,B,C}->D if A->D and B->D and C->D
        //A->union{B,C,D} if A->B or A->C or A->D
        if(to instanceof Union){
            return Arrays.stream(((Union) to).fieldNames).map(to.fields::get).anyMatch(e->canAssign(e,from,allowCast,generics));
        }else if(from instanceof Union){
            return Arrays.stream(((Union) from).fieldNames).map(from.fields::get).allMatch(e->canAssign(to,e,allowCast,generics));
        }
        //{A0,..,AN}->{B0,...,BN} iff A_i -> B_i for all i
        if(to instanceof Struct&&from instanceof Struct){
            if(((Struct) to).fieldNames.length!=((Struct) from).fieldNames.length)
                return false;
            return IntStream.range(0,((Struct) to).fieldNames.length)
                    .allMatch(i->canAssign(to.fields.get(((Struct) to).fieldNames[i]),from.fields.get(((Struct) from).fieldNames[i]),
                            allowCast, generics));
        }
        //(A0,...,AN)=>? -> (B0,...,BN)=>? iff A_i -> B_i for all i
        if(to instanceof Proc&&from instanceof Proc){
            if(((Proc) to).argTypes.length!=((Proc) from).argTypes.length)
                return false;
            return IntStream.range(0,((Proc) to).argTypes.length)
                    .allMatch(i->canAssign(((Proc) from).argTypes[i],((Proc) to).argTypes[i],allowCast,generics));
        }
        if(to instanceof AnyType){
            if(((AnyType) to).genericName==null){
                return true;
            }else if(from instanceof AnyType){
                return ((AnyType) to).genericName.equals(((AnyType) from).genericName);
            }else if(generics!=null){
                GenericBound prev=generics.get(((AnyType) to).genericName);
                if(prev==null){
                    generics.put(((AnyType) to).genericName,new GenericBound(from,AnyType.ANY));
                    return true;
                }else{
                    prev.assignableFrom=commonSupertype(prev.assignableFrom,from);
                    return canAssign(prev.assignableTo,prev.assignableFrom,false,null);
                }
            }
        }else if(from instanceof AnyType&&((AnyType) from).genericName!=null&&generics!=null){
            GenericBound prev=generics.get(((AnyType) from).genericName);
            if(prev==null){
                generics.put(((AnyType) from).genericName,new GenericBound(Union.EMPTY,to));
                return true;
            }else{
                if(canAssign(prev.assignableTo,to,false,null)){
                    prev.assignableTo=to;
                    return canAssign(prev.assignableTo,prev.assignableFrom,false,null);
                }
            }
        }
        return false;
    }

    //TODO ensure that no Value is assigned without castTo()
    // castTo should only return a value with eactly the supplied type (setting generics to any)
    public static boolean canAssign(Type to,Type from, HashMap<String, GenericBound> generics){
        return canAssign(to, from,false, generics);
    }
    public static boolean canCast(Type to,Type from, HashMap<String, GenericBound> generics){
        return canAssign(to, from,true,generics );
    }


    public static class Optional extends Type{
        public final Type content;
        public Optional(Type content) {
            super(content.wrappedName()+"?", 2, content.varSize, false);
            this.content=content;
            fields.put(FIELD_NAME_VALUE,content);
        }
        String wrappedName(){
            return "("+name+")";
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

        @Override
        public Iterable<Type> childTypes() {
            return Collections.singleton(content);
        }
    }
    public static class Reference extends Type{
        public final Type content;
        public Reference(Type content) {
            super("@"+content.wrappedName(), 1, false, false);
            this.content=content;
            fields.put(FIELD_NAME_VALUE,content);
        }
        String wrappedName(){
            return "("+name+")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Reference reference = (Reference) o;
            return Objects.equals(content, reference.content);
        }
        @Override
        public int hashCode() {
            return Objects.hash(content);
        }
        @Override
        public Iterable<Type> childTypes() {
            return Collections.singleton(content);
        }
    }

    public static class Array extends Type{
        public final Type content;
        public Array(Type content) {
            super(content.wrappedName()+"[]", 1, true, false);
            this.content=content;
            fields.put(FIELD_NAME_LENGTH,Numeric.UINT64);
        }
        String wrappedName(){
            return "("+name+")";
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Array array = (Array) o;
            return Objects.equals(content, array.content);
        }
        @Override
        public int hashCode() {
            return Objects.hash(content);
        }
        @Override
        public Iterable<Type> childTypes() {
            return Collections.singleton(content);
        }
    }

    private static int calculateBlockCount(Type[] types) {
        return Arrays.stream(types).mapToInt(t->t.blockCount).sum();
    }
    private static boolean isVarSize(Type[] types) {
        return Arrays.stream(types).anyMatch(t->t.varSize);
    }
    public static class Tuple extends Type{
        final String tupleName;
        final Type[] elements;
        private static String tupleName(Type[] types) {
            StringBuilder ret=new StringBuilder("tuple{");
            for(int i=0;i<types.length;i++){
                if(i>0){
                    ret.append(", ");
                }
                ret.append(types[i]);
            }
            return ret.append('}').toString();
        }
        public Tuple(String name, Type[] elements) {
            super(name==null?tupleName(elements):name, calculateBlockCount(elements), isVarSize(elements), false);
            this.tupleName=name;
            this.elements=elements;
            fields.put(FIELD_NAME_LENGTH,Numeric.UINT64);
        }
        public Type[] getElements() {
            return elements;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple tuple = (Tuple) o;
            return Arrays.equals(elements, tuple.elements);
        }
        @Override
        public int hashCode() {
            return Arrays.deepHashCode(elements);
        }
        @Override
        public Iterable<Type> childTypes() {
            return Arrays.asList(elements);
        }
    }

    public static class Struct extends StructOrUnion{
        public Struct(String name, Type[] types, String[] names) {
            super(name,types,names,false);
        }
    }
    public static class Union extends StructOrUnion{
        /**Value type for empty Arrays, assignable to any other type*/
        public static final Union EMPTY = new Union("\"EMPTY\"", new Type[0], new String[0]);
        public Union(String name, Type[] types, String[] names) {
            super(name,types,names,true);
        }
    }
    private static abstract class StructOrUnion extends Type{
        public final boolean isUnion;
        final String structName;
        final String[] fieldNames;
        final Type[] elements;
        private static String structName(Type[] types,String[] names,boolean isUnion) {
            if(types.length!=names.length){
                throw new IllegalArgumentException("lengths of types and names do not match");
            }
            StringBuilder ret=new StringBuilder(isUnion?"union{":"struct{");
            for(int i=0;i<types.length;i++){
                if(i>0){
                    ret.append(", ");
                }
                ret.append(types[i]).append(": ").append(names[i]);
            }
            return ret.append('}').toString();
        }
        private StructOrUnion(String name, Type[] types, String[] names,boolean isUnion){
            super(name==null?structName(types,names,isUnion):name,
                    isUnion?Stream.of(types).mapToInt(t->t.blockCount).max().orElse(0):Stream.of(types).mapToInt(t->t.blockCount).max().orElse(0)
                    , isVarSize(types), false);
            structName=name;
            this.isUnion=isUnion;
            this.fieldNames=names.clone();
            this.elements=types.clone();
            for(int i=0;i<names.length;i++){
                if(fields.put(names[i],types[i])!=null){
                    throw new TypeError("duplicate or reserved field-name \""+names[i]+"\" in struct "+this);
                }
            }
        }

        @Override
        public boolean isMutableFlied(String fieldName) {
            for(String field:fieldNames){
                if(field.equals(fieldName))
                    return true;
            }
            return super.isMutableFlied(fieldName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Struct struct = (Struct) o;
            return Objects.equals(fields, struct.fields);
        }
        @Override
        public int hashCode() {
            return Objects.hash(fields);
        }@Override

        public Iterable<Type> childTypes() {
            return Arrays.asList(elements);
        }
    }
    public static class Proc extends Type{
        private final Type[] argTypes;
        private static String procName(Type[] argTypes) {
            StringBuilder ret=new StringBuilder("(");
            for(Type t:argTypes){
                if(ret.length()>1){
                    ret.append(", ");
                }
                ret.append(t);
            }
            return ret.append(")=>?").toString();
        }
        public Proc(Type[] argTypes) {
            super(procName(argTypes), 1, false, false);
            this.argTypes=argTypes;
            fields.put(FIELDS_PROC_TYPES,new Array(Primitive.TYPE));
        }

        String wrappedName(){
            return "("+name+")";
        }

        public Type[] getArgTypes(){
            return argTypes;
        }
        /**Number of blocks needed to store the arguments of this procedure*/
        public long argsBlockSize() {
            return Type.calculateBlockCount(argTypes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Proc proc = (Proc) o;
            return Arrays.equals(argTypes, proc.argTypes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(argTypes);
        }

        public Iterable<Type> childTypes() {
            return Arrays.asList(argTypes);
        }
    }



    public static class GenericBound{
        Type assignableFrom;
        Type assignableTo;
        private GenericBound(Type assignableFrom,Type assignableTo){
            this.assignableFrom=assignableFrom;
            this.assignableTo=assignableTo;
        }

        @Override
        public String toString() {
            return "GenericBound{from=" + assignableFrom + ", to=" + assignableTo +'}';
        }
    }

}
