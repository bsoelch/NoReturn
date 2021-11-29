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

    private static final class TypeType extends Type{
        private TypeType() {
            super("type", 1, false);
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
    /**Type of NONE Value, assignable to any reference*/
    public static final Type TYPE = new TypeType();
    /**Type of NONE Value, assignable to any reference*/
    public static final Type NONE_TYPE = new Type("\"none\"", 1, false);
    /**Value type for empty Arrays, assignable to any other type*/
    public static final Type EMPTY_TYPE  = new Type("\"empty\"", 1, false);
    /**Value type that can hold values of any other type*/
    public static final Type ANY  = new Type("any", 2, true);

    public static class Primitive extends Type{
        private static final HashMap<String,Type> primitives=new HashMap<>();

        public static final Primitive BOOL      = new Primitive("bool",false);

        private Primitive(String name,boolean varSize){
            super(name, 1, varSize);
            if(primitives.put(name,this)!=null){
                throw new RuntimeException("The primitive \""+name+"\" already exists");
            }
            initFields();
        }
        void initFields(){}
    }
    public static class Numeric extends Primitive{
        private static final ArrayList<Numeric> numberTypes=new ArrayList<>();
        public final int level;
        public final boolean signed,isFloat;

        public static final Numeric INT8      = new Numeric("int8",0,true,false);
        public static final Numeric UINT8     = new Numeric("uint8",0,false,false);
        public static final Numeric INT16     = new Numeric("int16",1,true,false);
        public static final Numeric UINT16    = new Numeric("uint16",1,false,false);
        public static final Numeric INT32     = new Numeric("int32",2,true,false);
        public static final Numeric UINT32    = new Numeric("uint32",2,false,false);
        public static final Numeric INT64     = new Numeric("int64",3,true,false);
        public static final Numeric UINT64    = new Numeric("uint64",3,false,false);
        /* TODO introduce char-types (type-aliases for uint8,uint16,uint32 that are converted to their respective character on toString)
        public static final Numeric CHAR8     = new Numeric("char8",0,false,false);
        public static final Numeric CHAR16    = new Numeric("char16",1,false,false);
        public static final Numeric CHAR32    = new Numeric("char32",2,false,false);
        */
        public static final Numeric FLOAT32   = new Numeric("float32",2,true,true);
        public static final Numeric FLOAT64   = new Numeric("float64",3,true,true);

        /**method for ensuring this class (with all Numeric-Type constants) is loaded*/
        static void ensureInitialized(){}

        private Numeric(String name,int level,boolean signed,boolean isFloat) {
            super(name,false);
            this.level=level;
            this.signed=signed;
            this.isFloat=isFloat;
            numberTypes.add(this);
        }

        public int bitSize(){
            return 8*(1<<level);
        }

        public static Iterable<Numeric> types(){
            return numberTypes;
        }
    }
    public static class NoRetString extends Type{
        private static final HashMap<String,Type> stringTypes=new HashMap<>();
        //addLater? nativeString (string with native encoding) ? nonUnicodeStrings
        /**UTF-8 string*/
        public static final NoRetString STRING8=new NoRetString(8);
        /**UTF-16 string*/
        public static final NoRetString STRING16=new NoRetString(16);
        /**UTF-32 string*/
        public static final NoRetString STRING32=new NoRetString(32);

        private final int charSize;

        private NoRetString(int charSize) {
            super("string"+charSize,2,true);
            this.charSize=charSize;
            fields.put(FIELD_NAME_LENGTH,Numeric.UINT64);
            stringTypes.put(name,this);
        }

        public static NoRetString sumType(NoRetString lType, NoRetString rType) {
            return lType.charSize>=rType.charSize?lType:rType;
        }
    }
    public static void addAtomics(Map<String,Type> typeNames) {
        Numeric.ensureInitialized();
        typeNames.put(ANY.name,ANY);
        typeNames.putAll(Primitive.primitives);
        typeNames.putAll(NoRetString.stringTypes);
    }

    final String name;
    final HashMap<String,Type> fields=new HashMap<>();
    /**number of blocks needed (in compiled code) to store a value of this type*/
    public final int blockCount;
    /**true if values of this type have a variable encoding size*/
    public final boolean varSize;

    private Type(String name, int blockCount, boolean varSize){
        this.name=name;
        this.blockCount = blockCount;
        this.varSize = varSize;
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
        return ANY;
    }

    private static boolean canAssign(Type to, Type from, boolean allowCast, HashMap<String, GenericBound> generics){
        if(to==from||to.equals(from))
            return true;
        if(from==EMPTY_TYPE||to==ANY)
            return true;
        if(allowCast&&from==ANY){
            return true;
        }
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
            return Stream.of(((Union) to).fieldNames).map(to.fields::get).anyMatch(e->canAssign(e,from,allowCast,generics));
        }else if(from instanceof Union){
            return Stream.of(((Union) from).fieldNames).map(from.fields::get).allMatch(e->canAssign(to,e,allowCast,generics));
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
        if(to instanceof Generic){
            if(from instanceof Generic){
                return to.name.equals(from.name);
            }else if(generics!=null){
                GenericBound prev=generics.get(to.name);
                if(prev==null){
                    generics.put(to.name,new GenericBound(from,ANY));
                    return true;
                }else{
                    prev.assignableFrom=commonSupertype(prev.assignableFrom,from);
                    return canAssign(prev.assignableTo,prev.assignableFrom,false,null);
                }
            }
        }else if(from instanceof Generic&&generics!=null){
            GenericBound prev=generics.get(from.name);
            if(prev==null){
                generics.put(to.name,new GenericBound(EMPTY_TYPE,to));
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

    //FIXME ensure that no Value is assigned without castTo()
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
            super(content.wrappedName()+"?", 2, content.varSize);
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
    }
    public static class Reference extends Type{
        public final Type content;
        public Reference(Type content) {
            super("@"+content.wrappedName(), 1, false);
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
    }

    public static class Array extends Type{
        public final Type content;
        public Array(Type content) {
            super(content.wrappedName()+"[]", 2, true);
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
    }

    private static int calculateBlockCount(Type[] types) {
        return Stream.of(types).mapToInt(t->t.blockCount).sum();
    }
    private static boolean isVarSize(Type[] types) {
        return Stream.of(types).anyMatch(t->t.varSize);
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
            super(name==null?tupleName(elements):name, calculateBlockCount(elements), isVarSize(elements));
            this.tupleName=name;
            this.elements=elements;
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
    }

    public static class Struct extends StructOrUnion{
        public Struct(String name, Type[] types, String[] names) {
            super(name,types,names,false);
        }
    }
    public static class Union extends StructOrUnion{
        public Union(String name, Type[] types, String[] names) {
            super(name,types,names,true);
        }
    }
    private static abstract class StructOrUnion extends Type{
        public final boolean isUnion;
        final String structName;
        String[] fieldNames;
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
                    , isVarSize(types));
            structName=name;
            this.isUnion=isUnion;
            this.fieldNames=names.clone();
            for(int i=0;i<names.length;i++){
                if(fields.put(names[i],types[i])!=null){
                    throw new TypeError("duplicate or reserved field-name \""+names[i]+"\" in struct "+this);
                }
            }
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
            super(procName(argTypes), 1, false);
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

    }

    /** Generics types in NoRet allow forcing the calle of a procedure to use the same type for two different arguments,
     *      * they allow signatures like ($a,($a)=>?)=>? that make it easier to call a restricted function
     * */
    public static class Generic extends Type{//TODO allow handling of generics in compile mode, generics should be replaceable by any value without side-effects
        public Generic(String name) {
            super("$"+name, 2, true);
        }
        @Override
        public String toString() {
            return "Generic: "+name;
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
