package bsoelch.noret.lang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Type {
    static final String FIELD_NAME_TYPE = "type";
    static final String FIELD_NAME_LENGTH = "length";

    static final String FIELD_NAME_HAS_VALUE = "hasValue";
    static final String FIELD_NAME_VALUE = "value";
    static final String FIELDS_PROC_TYPES = "argTypes";

    /**Type of NONE Value, assignable to any reference*/
    public static final Type NONE_TYPE = new Type("\"none\"");
    /**Type of NOP Value, assignable to any procedure*/
    public static final Type NOP_TYPE  = new Type("\"NOP\"");
    /**Value type for empty Arrays, assignable to any other type*/
    public static final Type EMPTY_TYPE  = new Type("\"empty\"");

    public static class Primitive extends Type{
        private static final HashMap<String,Type> primitives=new HashMap<>();
        //addLater own class for Type type
        public static final Primitive TYPE       = new Primitive("type") {
            @Override
            void initFields() {
                fields.put(FIELD_NAME_TYPE,this);
                //addLater more fields
                // .isArray
                // .isMap
                // .isStruct
                // .isOptional
                // .isReference
                // .fields
                // .keyType
                // .valueType
            }
        };
        //addLater move any to own class (any is no primitive)
        public static final Primitive ANY       = new Primitive("any");

        public static final Primitive BOOL      = new Primitive("bool");
        public static final Primitive STRING    = new Primitive("string") {
            @Override
            void initFields() {
                fields.put(FIELD_NAME_LENGTH,Numeric.UINT64);
            }
        };

        private Primitive(String name){
            super(name);
            if(primitives.put(name,this)!=null){
                throw new IllegalArgumentException("The primitive \""+name+"\" already exists");
            }
            initFields();
        }
        void initFields(){}
    }
    public static class Numeric extends Primitive{
        public final int level;
        public final boolean signed,isFloat;
        //addLater u?int[32|64].bitsAsFloat, float[32|64].bitsAsInt
        public static final Numeric INT8      = new Numeric("int8",0,true,false);
        public static final Numeric UINT8     = new Numeric("uint8",0,false,false);
        public static final Numeric INT16     = new Numeric("int16",1,true,false);
        public static final Numeric UINT16    = new Numeric("uint16",1,false,false);
        public static final Numeric INT32     = new Numeric("int32",2,true,false);
        public static final Numeric UINT32    = new Numeric("uint32",2,false,false);
        public static final Numeric INT64     = new Numeric("int64",3,true,false);
        public static final Numeric UINT64    = new Numeric("uint64",3,false,false);

        public static final Numeric FLOAT32   = new Numeric("float32",2,true,true);
        public static final Numeric FLOAT64   = new Numeric("float64",3,true,true);

        /**method for ensuring this class (with all Numeric-Type constants) is loaded*/
        static void ensureInitialized(){}

        private Numeric(String name,int level,boolean signed,boolean isFloat) {
            super(name);
            this.level=level;
            this.signed=signed;
            this.isFloat=isFloat;
        }
    }
    public static void addPrimitives(Map<String,Type> typeNames) {
        Numeric.ensureInitialized();
        typeNames.putAll(Primitive.primitives);
    }

    final String name;
    final HashMap<String,Type> fields=new HashMap<>();
    private Type(String name){
        this.name=name;
        fields.put(FIELD_NAME_TYPE,Primitive.TYPE);
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
        }
        //addLater better calculation for common supertype
        return Primitive.ANY;
    }


    private static boolean canAssign(Type to, Type from, boolean allowCast, HashMap<String, GenericBound> generics){
        if(to==from)
            return true;
        if(from==EMPTY_TYPE||to==Primitive.ANY)
            return true;
        if(allowCast&&from==Primitive.ANY){
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
        if(to instanceof Array&&from instanceof Array){
            return canAssign(((Array) to).content,((Array) from).content,allowCast, generics);
        }
        //A? -> bool , A? -> B? iff A->B
        if(from instanceof Optional){
            if(to==Primitive.BOOL){
                return true;
            }else if(to instanceof Optional){
                return canAssign(((Optional) to).content,((Optional) from).content,allowCast, generics);
            }else if(allowCast){
                return canAssign(to,((Optional) from).content,true,generics );
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
        //{A0,..,AN}->{B0,...,BN} iff A_i -> B_i for all i
        if(to instanceof Struct&&from instanceof Struct){
            if(((Struct) to).fieldNames.size()!=((Struct) from).fieldNames.size())
                return false;
            Type t_to,t_from;
            for(String s:((Struct) to).fieldNames){
                t_to=to.fields.get(s);
                t_from=from.fields.get(s);
                if(!canAssign(t_to,t_from,allowCast, generics))
                    return false;
            }
            return true;
        }
        //(A0,...,AN)=>? -> (B0,...,BN)=>? iff A_i -> B_i for all i
        if(to instanceof Proc){
            if(from instanceof Proc){
                if(((Proc) to).argTypes.length!=((Proc) from).argTypes.length)
                    return false;
                for(int i=0;i<((Proc) from).argTypes.length;i++){
                    if(!canAssign(((Proc) from).argTypes[i],((Proc) to).argTypes[i],allowCast,generics))
                        return false;
                }
                return true;
            }else{
                return from == NOP_TYPE;
            }
        }
        if(to instanceof Generic){
            if(from instanceof Generic){
                return to.name.equals(from.name);
            }else if(generics!=null){
                GenericBound prev=generics.get(to.name);
                if(prev==null){
                    generics.put(to.name,new GenericBound(from,Primitive.ANY));
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

    public static boolean canAssign(Type to,Type from, HashMap<String, GenericBound> generics){
        return canAssign(to, from,false, generics);
    }
    public static boolean canCast(Type to,Type from, HashMap<String, GenericBound> generics){
        return canAssign(to, from,true,generics );
    }


    public static class Optional extends Type{
        public final Type content;
        public Optional(Type content) {
            super(content.wrappedName()+"?");
            this.content=content;
            fields.put(FIELD_NAME_HAS_VALUE,Primitive.BOOL);
            fields.put(FIELD_NAME_VALUE,content);
        }
        String wrappedName(){
            return "("+name+")";
        }
    }
    public static class Reference extends Type{
        public final Type content;
        public Reference(Type content) {
            super("@"+content.wrappedName());
            this.content=content;
            fields.put(FIELD_NAME_VALUE,content);
        }
        String wrappedName(){
            return "("+name+")";
        }
    }

    public static class Array extends Type{
        public final Type content;
        public Array(Type content) {
            super(content.wrappedName()+"[]");
            this.content=content;
            fields.put(FIELD_NAME_LENGTH,Numeric.UINT64);
        }
        String wrappedName(){
            return "("+name+")";
        }
    }

    public static class Struct extends Type{
        private final HashSet<String> fieldNames=new HashSet<>();
        private static String structName(Type[] types,String[] names) {
            if(types.length!=names.length){
                throw new IllegalArgumentException();
            }
            StringBuilder ret=new StringBuilder("{");
            for(int i=0;i<types.length;i++){
                if(i>0){
                    ret.append(", ");
                }
                ret.append(types[i]).append(": ").append(names[i]);
            }
            return ret.append('}').toString();
        }

        public Struct(Type[] types, String[] names) {
            super(structName(types,names));
            for(int i=0;i<names.length;i++){
                if(fields.put(names[i],types[i])!=null){
                    throw new IllegalArgumentException("duplicate or reserved field-name \""+names[i]+"\" in struct "+this);
                }
                fieldNames.add(names[i]);
            }
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
            super(procName(argTypes));
            this.argTypes=argTypes;
            fields.put(FIELDS_PROC_TYPES,new Array(Primitive.TYPE));
        }

        String wrappedName(){
            return "("+name+")";
        }

        public Type[] getArgTypes(){
            return argTypes;
        }

    }

    //addLater: OneOf(Type[])
    // syntax: <Type>|<Type>

    /**Generics types in NoRet are designed to allow value read operations
     * without losing the function context, the intended use is syntax like the following
     * read($a,($a,string)=>?)
     * */
    public static class Generic extends Type{
        //addLater? restricted generics
        public Generic(String name) {
            super("$"+name);
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
