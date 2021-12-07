package bsoelch.noret;

import bsoelch.noret.lang.*;
import bsoelch.noret.lang.expression.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

//TODO don't include code for unused types
public class CompileToC {
    public static final String VALUE_BLOCK_NAME = "Value";
    public static final String CAST_BLOCK = "("+VALUE_BLOCK_NAME+")";

    private static final String[] procedureArgs     ={VALUE_BLOCK_NAME + "*", VALUE_BLOCK_NAME+"*"};
    private static final String[] procedureArgNames ={"argsIn",               "argsOut"};
    private static final String procedureOut="void*";

    public static final String PROC_PREFIX = "proc_";
    public static final String CONST_PREFIX = "const_";
    public static final String PROCEDURE_TYPE = "Procedure";
    public static final String TYPE_SIG_PREFIX = "TYPE_SIG_";
    public static final String LOG_STREAM_PREFIX = "log_";

    public static final int ARRAY_HEADER = 3;
    public static final int ARRAY_OFF_OFFSET = 0;
    public static final int ARRAY_CAP_OFFSET = 1;
    public static final int ARRAY_LEN_OFFSET = 2;

    public static final String LOG_TYPE_NAME = "LogType";
    public static final String PRINT_TYPE_NAME = "printType";
    public static final String PRINT_VALUE = "printValue";

    public static final String PRINT__UTF8_BUFF = "charBuff";
    public static final String PRINT__UTF8_COUNT = "count";
    public static final String PRINT__STREAM_NAME = "log";
    public static final String PRINT__VALUE_NAME = "value";

    public static final String ID_LOG = "logValue";
    public static final String ARRAY_GET_NAME = "getElement";
    public static final String ARRAY_GET_RAW_NAME = "getRawElement";

    public static final int ERR_NONE  = 0;
    public static final int ERR_MEM   = 1;
    public static final int ERR_TYPE  = 2;
    public static final int ERR_INDEX = 3;
    public static final int ERR_STR_ENCODING=4;

    /* TODO rewrite DataOut
        constant memory may keep the original approach
        local/reference memory will be stored in malloced-memory sections:
            1. Arrays/Strings
            [start], start -> [off,cap,len,data[0],...,data[off],....,data[len],...,data[cap-1]}
            2. any
            [type,data], data -> raw | [cap,len,data[0],...,data[len-1],...,data[cap-1]]
            3. optional:
            [hasData,data], data -> raw | [data[0],...,data[N]]
            4. reference:
            [ptr], ptr->[data[0],...,data[N]]
    */
    private static class DataOut {
        final String prefix;
        final ArrayDeque<String> prefixLines=new ArrayDeque<>();
        int tmpCount;
        final boolean constant;
        private DataOut(String prefix,int tmpCount,boolean constant){
            this.tmpCount=tmpCount;
            this.prefix=prefix;
            this.constant=constant;
        }
        public String nextName(){
            return prefix+(tmpCount++);
        }

        private int getMinCap(Type type,int elementCount){
            if(type instanceof Type.Array){//Array
                if(((Type.Array) type).content instanceof Type.Primitive){
                    return (((Type.Primitive)((Type.Array) type).content).byteCount*elementCount+7)/8;
                }else{
                    return elementCount*((Type.Array) type).content.blockCount;
                }
            }else if(type instanceof Type.NoRetString){//String
                return (((((Type.NoRetString)type).charBits +7)/8)*elementCount+7)/8;
            }else{
                throw new UnsupportedOperationException("unsupported mutable "+type);
            }
        }
        public StringBuilder newValueBuilder(String name, Type type,int elementCount,boolean prefix) {
            if(constant){
                StringBuilder sb=new StringBuilder("static "+VALUE_BLOCK_NAME+" "+name+"[]={");
                if(type instanceof Type.Array||type instanceof Type.NoRetString){
                    //off
                    if(prefix) {sb.append(CAST_BLOCK);}
                    sb.append("{.").append(typeFieldName(Type.Numeric.SIZE)).append("=0/*off*/},");
                    //cap
                    if(prefix) {sb.append(CAST_BLOCK);}
                    sb.append("{.").append(typeFieldName(Type.Numeric.SIZE)).append("=")
                            .append(getMinCap(type,elementCount)).append("/*cap*/},");
                    //len
                    if(prefix) {sb.append(CAST_BLOCK);}
                    sb.append("{.").append(typeFieldName(Type.Numeric.SIZE)).append("=").append(elementCount).append("/*len*/}");
                }else{
                    //TODO implement storage of other value-types
                    throw new UnsupportedOperationException("unimplemented");
                }
                return sb;
            }else{
                //TODO padding
               return new StringBuilder("memcpy("+name+"+"+ARRAY_HEADER+",("+VALUE_BLOCK_NAME+"[]){");
            }
        }
        public void addValueBuilder(String name,StringBuilder builder, Type type,int elementCount){
            if(constant){
                prefixLines.add(builder.append("};").toString());
            }else{
                prefixLines.add(VALUE_BLOCK_NAME+"* "+name+"=malloc(("+(getMinCap(type, elementCount)+ARRAY_HEADER)+
                        ")*sizeof("+VALUE_BLOCK_NAME+"));");
                prefixLines.add(name+"["+ARRAY_OFF_OFFSET+"]="+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"=0}; /*off*/");//TODO padding
                prefixLines.add(name+"["+ARRAY_CAP_OFFSET+"]="+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"="+getMinCap(type, elementCount)+"}; /*cap*/");
                prefixLines.add(name+"["+ARRAY_LEN_OFFSET+"]="+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"="+elementCount+"}; /*len*/");
                prefixLines.add(builder.append("},(").append(getMinCap(type, elementCount)).append(")*sizeof("+VALUE_BLOCK_NAME+"));").toString());
            }
        }
    }

    private final BufferedWriter out;
    private final HashMap<Type, Integer> typeOffsets=new HashMap<>();
    private final HashMap<Type.StructOrUnion, Integer> structOffsets=new HashMap<>();
    private int min2BlockSignature;
    /**minByteSignatures[i]=#{signatures < 2^(i+1)}*/
    private final int[] minByteSignatures =new int[4];

    public CompileToC(BufferedWriter out) {
        this.out = out;
    }

    private void writeLine(String line) throws IOException{
        out.write(line);
        out.newLine();
    }

    private void comment(String str) throws IOException{
        String[] lines=str.split("[\n\r]");
        for(String s:lines){
            writeLine("// "+s);
        }
    }
    private void comment(String prefix,String str) throws IOException{
        String[] lines=str.split("[\n\r]");
        for(String s:lines){
            writeLine(prefix+"// "+s);
        }
    }
    private void include(String include)throws IOException {
        writeLine("#include <"+include+">");
    }

    private String escapeStr(Object o){
        return o.toString().replace("\"","\\\"");
    }

    private Integer typeOffset(Type t) {
        Integer off = typeOffsets.get(t);
        assert off != null;
        return off;
    }
    private Integer structOffset(Type.StructOrUnion t) {
        Integer off = structOffsets.get(t);
        assert off != null;
        return off;
    }

    private static String typeName(Type.Primitive type,boolean uppercase){
        if(type instanceof Type.Numeric){
            if(((Type.Numeric)type).isFloat){
                return "F"+ ((Type.Numeric)type).bitSize();
            }else{
                return (((Type.Numeric)type).signed?"I":(((Type.Numeric)type).isChar?"C":"U"))+
                        ((Type.Numeric)type).bitSize();
            }
        }else if(type== Type.Primitive.BOOL){
            return uppercase?"BOOL":"Bool";
        }else{
            throw new RuntimeException("unexpected primitive: "+type);
        }
    }
    private static ArrayList<String> printParams(Type.Primitive type){
        ArrayList<String> ret=new ArrayList<>();
        //fprintf(STREAM,FORMAT,CAST value FIELD)
        String print="fprintf("+ PRINT__STREAM_NAME +",";
        String value="(*(("+cTypeName(type)+"*)"+PRINT__VALUE_NAME+"))";
        if(type instanceof Type.Numeric){
            if(((Type.Numeric)type).isFloat){
                ret.add(print+"\"%f\","+value+");");
                return ret;
            }else{
                if(((Type.Numeric) type).isChar){
                    if(type.byteCount==1){
                        ret.add(print+"\"'%c'\","+value+");");
                        return ret;
                    }else{
                        ret.add(PRINT__UTF8_COUNT +"=0;");
                        ret.add("codePointToUTF8("+value+", "+ PRINT__UTF8_BUFF +", &"+ PRINT__UTF8_COUNT +");");
                        ret.add(print+"\"%.*s\",(int)"+PRINT__UTF8_COUNT+", (char*)"+ PRINT__UTF8_BUFF +");");
                        return ret;
                    }
                }else{
                    ret.add(print+"\"%\"PRI"+(((Type.Numeric)type).signed?"i":"u")+((Type.Numeric)type).bitSize()+","+value+");");
                    return ret;
                }
            }
        }else if(type== Type.Primitive.BOOL){
            ret.add(print+"\"%s\","+value+"?\"true\":\"false\");");
            return ret;
        }else{
            throw new RuntimeException("unexpected primitive: "+type);
        }
    }
    private String typeSignature(Type t){
        if(t instanceof Type.Primitive){
            return TYPE_SIG_PREFIX+typeName((Type.Primitive)t,true);
        }else if(t==Type.NoRetString.STRING8){
            return TYPE_SIG_PREFIX + "STRING8";
        }else if(t==Type.NoRetString.STRING16){
            return TYPE_SIG_PREFIX + "STRING16";
        }else if(t==Type.NoRetString.STRING32){
            return TYPE_SIG_PREFIX + "STRING32";
        }else if(t==Type.TYPE){
            return TYPE_SIG_PREFIX + "TYPE";
        }else if(t==Type.Primitive.NONE_TYPE){
            return TYPE_SIG_PREFIX + "NONE";
        }else if(t instanceof Type.AnyType){
            return TYPE_SIG_PREFIX + "ANY";
        }else if(t instanceof Type.Optional){
            Integer off = typeOffset(((Type.Optional) t).content);
            return TYPE_SIG_PREFIX + "OPTIONAL|("+off+"ULL<<TYPE_CONTENT_SHIFT)";
        }else if(t instanceof Type.Reference){
            Integer off = typeOffset(((Type.Reference) t).content);
            return TYPE_SIG_PREFIX + "REFERENCE|("+off+"ULL<<TYPE_CONTENT_SHIFT)";
        }else if(t instanceof Type.Array){
            Integer off = typeOffset(((Type.Array) t).content);
            return TYPE_SIG_PREFIX + "ARRAY|("+off+"ULL<<TYPE_CONTENT_SHIFT)";
        }else if(t instanceof Type.Tuple){//addLater combine similar cases
            Integer off = typeOffset(t);
            int len = ((Type.Tuple) t).getElements().length;
            if(len>0xffff){//addLater constant
                throw new SyntaxError("block-type exceeded type maximum allowed element count of "+0xffff);
            }
            return TYPE_SIG_PREFIX+ "TUPLE|("+off+"ULL<<TYPE_CONTENT_SHIFT)|" +"("+ len +"ULL<<TYPE_COUNT_SHIFT)";
        }else if(t instanceof Type.Struct){
            Integer off = structOffset((Type.StructOrUnion) t);
            int len = ((Type.StructOrUnion) t).elementCount();
            if(len>0xffff){
                throw new SyntaxError("block-type exceeded type maximum allowed element count of "+0xffff);
            }
            return TYPE_SIG_PREFIX+ "STRUCT|("+off+"ULL<<TYPE_CONTENT_SHIFT)|" +"("+ len +"ULL<<TYPE_COUNT_SHIFT)";
        }else if(t instanceof Type.Union){
            Integer off = structOffset((Type.StructOrUnion) t);
            int len = ((Type.StructOrUnion) t).elementCount();
            if(len>0xffff){
                throw new SyntaxError("block-type exceeded type maximum allowed element count of "+0xffff);
            }
            return TYPE_SIG_PREFIX+ "UNION|("+off+"ULL<<TYPE_CONTENT_SHIFT)|" +"("+ len +"ULL<<TYPE_COUNT_SHIFT)";
        }else if(t instanceof Type.Proc){
            Integer off = typeOffset(t);
            int len = ((Type.Proc) t).getArgTypes().length;
            if(len>0xffff){
                throw new SyntaxError("block-type exceeded type maximum allowed element count of "+0xffff);
            }
            return TYPE_SIG_PREFIX+ "PROC|("+off+"ULL<<TYPE_CONTENT_SHIFT)|" +"("+ len +"ULL<<TYPE_COUNT_SHIFT)";
        }
        throw new UnsupportedOperationException("unsupported Type :"+t);
    }


    private static String typeFieldName(Type.Primitive type){
        return "as"+typeName(type,false);
    }
    private static String cTypeName(Type.Primitive type){
        if(type instanceof Type.Numeric){
            if(((Type.Numeric)type).isFloat){
                return "float"+((Type.Numeric)type).bitSize()+"_t";
            }else{
                return (((Type.Numeric)type).signed?"int":"uint")+ ((Type.Numeric)type).bitSize()+"_t";
            }
        }else if(type == Type.Primitive.BOOL){
            return "bool";
        }else{
            throw new RuntimeException("unexpected primitive: "+type);
        }
    }

    //addLater use signatures that are less likely to collide with other functions

    private void writeFileHeader(long maxArgSize, Parser.ParserContext context) throws IOException {
        comment("Auto generated code from NoRet compiler");
        //addLater print information about compiled code
        out.newLine();
        //includes
        include("stdlib.h");
        include("stdbool.h");
        include("string.h");
        include("stdio.h");
        include("inttypes.h");
        include("assert.h");
        out.newLine();
        writeLine("#define MAX_ARG_SIZE       0x"+Long.toHexString(maxArgSize));
        writeLine("#define ARG_DATA_INIT_SIZE 0x1000");
        out.newLine();
        //Type enum
        comment("Type Definitions");
        writeLine("typedef uint64_t Type;");
        writeLine("#define " + TYPE_SIG_PREFIX + "MASK       0xff");
        int count=0;
        //primitives 1-8 bytes
        for(Type.Primitive t: Type.Primitive.types()){
            StringBuilder tmp=new StringBuilder(typeSignature(t));
            while(tmp.length()<19){tmp.append(' ');}
            writeLine("#define "+tmp+" 0x"+Integer.toHexString(count++));
            for(int i = 0; i< minByteSignatures.length; i++){
                if(t.byteCount<(2<<i)){
                    minByteSignatures[i]++;
                }
            }
        }
        //1-block Types
        writeLine("#define " + TYPE_SIG_PREFIX + "NONE       0x"+Integer.toHexString(count++));
        writeLine("#define " + TYPE_SIG_PREFIX + "TYPE       0x"+Integer.toHexString(count++));
        writeLine("#define " + TYPE_SIG_PREFIX + "STRING8    0x"+Integer.toHexString(count++));
        writeLine("#define " + TYPE_SIG_PREFIX + "STRING16   0x"+Integer.toHexString(count++));
        writeLine("#define " + TYPE_SIG_PREFIX + "STRING32   0x"+Integer.toHexString(count++));
        writeLine("#define " + TYPE_SIG_PREFIX + "ARRAY      0x"+Integer.toHexString(count++));//content[u32-off]
        writeLine("#define " + TYPE_SIG_PREFIX + "TUPLE      0x"+Integer.toHexString(count++));//content[u32-off][u16-size]
        writeLine("#define " + TYPE_SIG_PREFIX + "REFERENCE  0x"+Integer.toHexString(count++));//content[u32-off]
        writeLine("#define " + TYPE_SIG_PREFIX + "PROC       0x"+Integer.toHexString(count++));//signature[u32-off][u16-size]
        min2BlockSignature=count;
        //2-block types
        writeLine("#define " + TYPE_SIG_PREFIX + "OPTIONAL   0x"+Integer.toHexString(count++));//content[u32-off]
        writeLine("#define " + TYPE_SIG_PREFIX + "ANY        0x"+Integer.toHexString(count++));
        //var-block types
        //addLater handle union and struct differently (depending on sum of lengths of contents)
        writeLine("#define " + TYPE_SIG_PREFIX + "UNION      0x"+Integer.toHexString(count++));//contents[u32-off][u16-size]
        writeLine("#define " + TYPE_SIG_PREFIX + "STRUCT     0x"+Integer.toHexString(count++));//contents[u32-off][u16-size]
        assert count<=0xff;
        writeLine("#define TYPE_CONTENT_SHIFT  8");
        writeLine("#define TYPE_CONTENT_MASK   0xffffffff");
        writeLine("#define TYPE_COUNT_SHIFT    40");
        writeLine("#define TYPE_COUNT_MASK     0xffff");
        comment("Type data for all composite Types");
        ArrayList<Type> contentTypes=new ArrayList<>();
        //addLater use (greedy-alg?) [shortest super-string] to reduce the total length of the saved values
        for(Type t:context.runtimeBlockTypes){
            typeOffsets.put(t,contentTypes.size());
            for(Type c:t.childTypes()){
               contentTypes.add(c);
            }
        }
        for(Type t:context.runtimeTypes){
            typeOffsets.put(t,contentTypes.size());
            contentTypes.add(t);
        }
        ArrayList<Type.StructEntry> structData=new ArrayList<>();
        for(Type.StructOrUnion s:context.runtimeStructs){
            structOffsets.put(s,structData.size());
            for(Type.StructEntry e:s.entries()){
                structData.add(e);
            }
        }
        StringBuilder tmp;
        if(contentTypes.size()>0) {
            tmp = new StringBuilder("Type typeData []={");
            boolean first = true;
            for (Type t : contentTypes) {
                if (first) {
                    first = false;
                } else {
                    tmp.append(',');
                }
                tmp.append(typeSignature(t));
            }
            writeLine(tmp.append("};").toString());
        }
        if(structData.size()>0){
            comment("Type data for struct and union types");
            writeLine("typedef struct{");
            writeLine("  char* name;");
            writeLine("  Type  type;");
            writeLine("}NoRetStructEntry;");
            tmp = new StringBuilder("NoRetStructEntry structData []={");
            boolean first = true;
            for (Type.StructEntry e : structData) {
                if (first) {
                    first = false;
                } else {
                    tmp.append(',');
                }
                tmp.append("{.name=\"").append(e.name).append("\",.type=").append(typeSignature(e.type)).append("}");
            }
            writeLine(tmp.append("};").toString());
        }
        out.newLine();
        //Value struct
        comment("value-block type");
        writeLine("typedef union " + VALUE_BLOCK_NAME + "Impl "+VALUE_BLOCK_NAME+";");
        //Procedure Type
        comment("procedure type (the return-type is void* instead of "+PROCEDURE_TYPE+"* to avoid a recursive type definition)");
        out.write("typedef "+procedureOut+ "(*" + PROCEDURE_TYPE + ")(");
        for(int i=0;i<procedureArgs.length;i++){
            out.write((i>0?",":"")+procedureArgs[i]);
        }
        writeLine(");");
        comment("float types");
        writeLine("typedef float float32_t;");//addLater implement platform independent float32,float64
        writeLine("typedef double float64_t;");
        comment("value-block definition");
        writeLine("union " + VALUE_BLOCK_NAME + "Impl{");
        for(Type.Primitive t:Type.Primitive.types()){
            tmp=new StringBuilder("  ");
            tmp.append(cTypeName(t));
            while(tmp.length()<12){
                tmp.append(' ');//align entries
            }
            tmp.append(' ').append(typeFieldName(t)).append(';');
            writeLine(tmp.toString());

        }
        writeLine("  Type       asType;");
        writeLine("  " + PROCEDURE_TYPE + "  asProc;");
        writeLine("  " + VALUE_BLOCK_NAME + "*     asPtr;");//reference
        writeLine("  uint8_t    raw8[8];");
        writeLine("  uint16_t   raw16[4];");
        writeLine("  uint32_t   raw32[2];");
        writeLine("  uint64_t   raw64[1];");
        writeLine("};");
        //multi-value types:
        //  union   -> (if any element is multi-valued)
        //  struct  ->  elt1,elt2,...,eltN
        //  any     ->  typeID,     val_ptr/raw_data
        //  opt     ->  hasData,    data
        out.newLine();
        comment("definitions and functions for log");
        writeLine("typedef enum{");
        comment("  null,","blank type for initial value");
        for(LogType.Type t:LogType.Type.values()){
            writeLine("  "+t+",");
        }
        writeLine("}" + LOG_TYPE_NAME + ";");
        comment("previously used logType");
        writeLine("static " + LOG_TYPE_NAME + " prevType = null;");
        comment("definition of NEW_LINE character");
        writeLine("#define NEW_LINE \"\\n\"");//addLater set to system line separator
        for(LogType.Type t:LogType.Type.values()){
            writeLine("FILE* " + LOG_STREAM_PREFIX +t+";");
        }
        comment("sets log-streams to their initial value");
        writeLine("void initLogStreams(){");
        for(LogType.Type t:LogType.Type.values()){
            writeLine("  " + LOG_STREAM_PREFIX +t+" = "+(t== LogType.Type.ERR?"stderr":"stdout")+";");
        }
        writeLine("}");
        comment("recursive printing of types");
        writeLine("void " + PRINT_TYPE_NAME + "(const Type type,FILE* log,bool recursive){");
        writeLine("  size_t off,len;");
        writeLine("  NoRetStructEntry e;");
        writeLine("  switch(type&" + TYPE_SIG_PREFIX + "MASK){");
        for(Type.Primitive t:Type.Primitive.types()){
            writeLine("    case "+typeSignature(t)+":");
            writeLine("      fputs(\""+escapeStr(t)+"\",log);");
            writeLine("      break;");
        }
        writeLine("    case " + TYPE_SIG_PREFIX + "STRING8:");
        writeLine("      fputs(\""+escapeStr(Type.NoRetString.STRING8)+"\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "STRING16:");
        writeLine("      fputs(\""+escapeStr(Type.NoRetString.STRING16)+"\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "STRING32:");
        writeLine("      fputs(\""+escapeStr(Type.NoRetString.STRING32)+"\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "TYPE:");
        writeLine("      fputs(\""+escapeStr(Type.TYPE)+"\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "NONE:");
        writeLine("      fputs(\""+escapeStr(Type.NONE_TYPE)+"\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "ANY:");
        writeLine("      fputs(\""+escapeStr(Type.AnyType.ANY)+"\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "OPTIONAL:");
        writeLine("      if(recursive){fputs(\"(\",log);}");
        writeLine("      " + PRINT_TYPE_NAME + "(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log,true);");
        writeLine("      fputs(recursive?\"?)\":\"?\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "REFERENCE:");
        writeLine("      fputs(recursive?\"(@\":\"@\",log);");
        writeLine("      " + PRINT_TYPE_NAME + "(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log,true);");
        writeLine("      if(recursive){fputs(\")\",log);}");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "ARRAY:");
        writeLine("      if(recursive){fputs(\"(\",log);}");
        writeLine("      " + PRINT_TYPE_NAME + "(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log,true);");
        writeLine("      fputs(recursive?\"[])\":\"[]\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "TUPLE:");
        writeLine("      fputs(\"tuple{\",log);");
        writeLine("      off=(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK;");
        writeLine("      len=(type>>TYPE_COUNT_SHIFT)&TYPE_COUNT_MASK;");
        writeLine("      for(size_t i=off;i<off+len;i++){");
        writeLine("        if(i>off){fputs(\", \",log);};");//no wrapping in tuple elements
        writeLine("        " + PRINT_TYPE_NAME + "(typeData[i],log,false);");
        writeLine("      }");
        writeLine("      fputs(\"}\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "PROC:");
        writeLine("      fputs(\"(\",log);");
        writeLine("      off=(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK;");
        writeLine("      len=(type>>TYPE_COUNT_SHIFT)&TYPE_COUNT_MASK;");
        writeLine("      for(size_t i=off;i<off+len;i++){");
        writeLine("        if(i>off){fputs(\", \",log);};");//no wrapping in function arguments
        writeLine("        " + PRINT_TYPE_NAME + "(typeData[i],log,false);");
        writeLine("      }");
        writeLine("      fputs(\")=>?\",log);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "STRUCT:");
        writeLine("    case " + TYPE_SIG_PREFIX + "UNION:");
        writeLine("      fputs(((type&" + TYPE_SIG_PREFIX + "MASK)=="+TYPE_SIG_PREFIX+"STRUCT)?\"struct{\":\"union{\",log);");
        writeLine("      off=(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK;");
        writeLine("      len=(type>>TYPE_COUNT_SHIFT)&TYPE_COUNT_MASK;");
        writeLine("      for(size_t i=off;i<off+len;i++){");
        writeLine("        if(i>off){fputs(\", \",log);};");
        writeLine("        e=structData[i];");
        writeLine("        " + PRINT_TYPE_NAME + "(e.type,log,true);");
        writeLine("        fprintf(log,\": %s\",e.name);");
        writeLine("      }");
        writeLine("      fputs(\"}\",log);");
        writeLine("      break;");
        writeLine("    default:");
        writeLine("      fprintf(stderr,\"unknown type signature:%\"PRIu64,type&" + TYPE_SIG_PREFIX + "MASK);");
        writeLine("      exit("+ERR_TYPE+");");
        writeLine("      break;");
        writeLine("  }");
        writeLine("}");
        comment("transforming of string types");
        //TODO prevent buffer overflows
        //TODO syntax-check encoding
        writeLine("static void codePointToUTF8("+cTypeName(Type.Numeric.CHAR32)+" codepoint,"
                +cTypeName(Type.Numeric.CHAR8)+ "* buff,size_t* " + PRINT__UTF8_COUNT + "){");
        writeLine("  if(codepoint<0x80){");
        writeLine("   buff[(*" + PRINT__UTF8_COUNT + ")++]=codepoint&0x7f;");
        writeLine("  }else if(codepoint<0x800){");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0xc0|((codepoint>>6)&0x1f);");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0x80|(codepoint&0x3f);");
        writeLine("  }else if(codepoint<0x10000){");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0xe0|((codepoint>>12)&0xf);");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0x80|((codepoint>>6)&0x3f);");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0x80|(codepoint&0x3f);");
        writeLine("  }else if(codepoint<0x200000){");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0xf0|((codepoint>>18)&0x7);");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0x80|((codepoint>>12)&0x3f);");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0x80|((codepoint>>6)&0x3f);");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0x80|(codepoint&0x3f);");
        writeLine("  }else{");
        writeLine("    fprintf(stderr,\"code-point out of range: %\"PRIu32,codepoint);");
        writeLine("    exit("+ERR_STR_ENCODING+");");
        writeLine("  }");
        writeLine("}");
        writeLine("static "+cTypeName(Type.Numeric.CHAR32)+" codePointFromUTF8("+cTypeName(Type.Numeric.CHAR8)+"* buff,size_t* off){");
        writeLine("  if((buff[*off]&0x80)==0){");
        writeLine("    return buff[(*off)++];");
        writeLine("  }else{");
        writeLine("    int m=0x40,c=0;");
        writeLine("    while(buff[*off]&m){");
        writeLine("      c++;");
        writeLine("      m>>=1;");
        writeLine("    }");
        writeLine("    "+cTypeName(Type.Numeric.CHAR32)+" ret=buff[(*off)++]&(m-1);");
        writeLine("    while(c>0){");
        writeLine("      if((buff[(*off)]&0xc0)!=0x80){");
        writeLine("        fprintf(stderr,\"format error in UTF8-string\");");
        writeLine("        exit("+ERR_STR_ENCODING+");");
        writeLine("      }");
        writeLine("      ret<<=6;");
        writeLine("      ret|=buff[(*off)++]&0x3f;");
        writeLine("      c--;");
        writeLine("    }");
        writeLine("    return ret;");
        writeLine("  }");
        writeLine("}");
        writeLine("static void codePointToUTF16("+cTypeName(Type.Numeric.CHAR32)+" codepoint,"+cTypeName(Type.Numeric.CHAR16)+ "* buff,size_t* " + PRINT__UTF8_COUNT + "){");
        writeLine("  if(codepoint<0x10000){");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=codepoint&0xffff;");
        writeLine("  }else if(codepoint<0x110000){");
        writeLine("    codepoint-=10000;");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0xd800|((codepoint>>10)&0x3ff);");
        writeLine("    buff[(*" + PRINT__UTF8_COUNT + ")++]=0xdc00|(codepoint&0x3ff);");
        writeLine("  }else{");
        writeLine("    fprintf(stderr,\"code-point out of range: %\"PRIu32,codepoint);");
        writeLine("    exit("+ERR_STR_ENCODING+");");
        writeLine("  }");
        writeLine("}");
        writeLine("static "+cTypeName(Type.Numeric.CHAR32)+" codePointFromUTF16("+cTypeName(Type.Numeric.CHAR16)+"* buff,size_t* off){");
        writeLine("  if(((buff[*off]&0xdC00)==0xd800)&&((buff[(*off)+1]&0xdC00)==0xdC00)){");
        writeLine("    "+cTypeName(Type.Numeric.CHAR32)+" ret=buff[(*off)++]&0x3ff;");
        writeLine("    ret<<=10;");
        writeLine("    ret|=buff[(*off)++]&0x3ff;");
        writeLine("    return ret+0x10000;");
        writeLine("  }else{");
        writeLine("    return buff[(*off)++];");
        writeLine("  }");
        writeLine("}");
        comment("recursive printing of values");
        writeLine("void " + PRINT_VALUE + "(FILE* "+ PRINT__STREAM_NAME +",Type type,const void* "+PRINT__VALUE_NAME+"){");//addLater surround strings with "" if not at top level
        writeLine("  Value* data;");
        writeLine("  Type valType;");
        writeLine("  "+cTypeName(Type.Numeric.CHAR8)+ " " + PRINT__UTF8_BUFF + "[4];");
        writeLine("  size_t " + PRINT__UTF8_COUNT + ";");
        writeLine("  "+cTypeName(Type.Numeric.CHAR16)+"* chars16;");
        writeLine("  "+cTypeName(Type.Numeric.CHAR32)+"* chars32;");
        writeLine("  size_t off,len;");
        writeLine("  switch(type&" + TYPE_SIG_PREFIX + "MASK){");
        for(Type.Primitive t: Type.Primitive.types()){
            writeLine("    case " + typeSignature(t)+":");
            ArrayList<String> parts=printParams(t);
            for(String l:parts){
                writeLine("      "+l);
            }
            writeLine("      break;");
        }
        writeLine("    case " + TYPE_SIG_PREFIX + "NONE:");
        writeLine("      fputs(\"\\\"none\\\"\","+ PRINT__STREAM_NAME +");");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "STRING8:");//!!! this implementation assumes that the system encoding is UTF8
        writeLine("      data=*(("+VALUE_BLOCK_NAME+"**)"+PRINT__VALUE_NAME+");/*"+PRINT__VALUE_NAME+"->asPtr*/");
        writeLine("      fprintf("+ PRINT__STREAM_NAME +",\"%.*s\",(int)(data["+ARRAY_LEN_OFFSET+"]."
                +typeFieldName(Type.Numeric.SIZE)+")"+",(char*)(data+"+ARRAY_HEADER+
                "/*header*/+data["+ARRAY_OFF_OFFSET+"]." +typeFieldName(Type.Numeric.SIZE)+"/*off*/));");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "STRING16:");
        writeLine("      data=*(("+VALUE_BLOCK_NAME+"**)"+PRINT__VALUE_NAME+");/*"+PRINT__VALUE_NAME+"->asPtr*/");
        writeLine("      chars16=(("+cTypeName(Type.Numeric.CHAR16)+"*)(data+"+ARRAY_HEADER+"))/*header size*/" +
                    "+data["+ARRAY_OFF_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+"/*off*/;");
        writeLine("      len=data["+ARRAY_LEN_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+"/*len*/;");
        writeLine("      off=0;");
        writeLine("      while(off<len){");
        writeLine("        "+PRINT__UTF8_COUNT+"=0;");
        writeLine("        codePointToUTF8(codePointFromUTF16(chars16,&off), "+PRINT__UTF8_BUFF+", &"+PRINT__UTF8_COUNT+");");
        writeLine("        fprintf("+ PRINT__STREAM_NAME +","+"\"%.*s\",(int)"+PRINT__UTF8_COUNT+", (char*)"+ PRINT__UTF8_BUFF +");");
        writeLine("      }");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "STRING32:");
        writeLine("      data=*(("+VALUE_BLOCK_NAME+"**)"+PRINT__VALUE_NAME+");/*"+PRINT__VALUE_NAME+"->asPtr*/");
        writeLine("      chars32=(("+cTypeName(Type.Numeric.CHAR32)+"*)(data+"+ARRAY_HEADER+"))/*header size*/" +
                    "+data["+ARRAY_OFF_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+"/*off*/;");
        writeLine("      len=data["+ARRAY_LEN_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+"/*len*/;");
        writeLine("      off=0;");
        writeLine("      while(off<len){");
        writeLine("        "+PRINT__UTF8_COUNT+"=0;");
        writeLine("        codePointToUTF8(chars32[off++], "+PRINT__UTF8_BUFF+", &"+PRINT__UTF8_COUNT+");");
        writeLine("        fprintf("+ PRINT__STREAM_NAME +","+"\"%.*s\",(int)"+PRINT__UTF8_COUNT+", (char*)"+ PRINT__UTF8_BUFF +");");
        writeLine("      }");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "TYPE:");
        writeLine("      " + PRINT_TYPE_NAME + "(*((Type*)"+PRINT__VALUE_NAME+"),"+ PRINT__STREAM_NAME +",false);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "ANY:");
        writeLine("      valType=*((Type*)"+PRINT__VALUE_NAME+");");
        writeLine("      if((valType&TYPE_SIG_MASK)<"+min2BlockSignature+"){");
        writeLine("        " + PRINT_VALUE + "("+ PRINT__STREAM_NAME +",valType,"+PRINT__VALUE_NAME+"+sizeof("+VALUE_BLOCK_NAME+")/*content*/);");
        writeLine("      }else{");
        writeLine("        " + PRINT_VALUE +
                "("+ PRINT__STREAM_NAME +",valType,*(("+VALUE_BLOCK_NAME+"**)("+PRINT__VALUE_NAME+"+sizeof("+VALUE_BLOCK_NAME+")))" +
                    "/*"+PRINT__VALUE_NAME+"*/);");
        writeLine("      }");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "OPTIONAL:");
        writeLine("      if(*((bool*)"+PRINT__VALUE_NAME+")){");
        writeLine("        fputs(\"Optional{\","+ PRINT__STREAM_NAME +");");
        writeLine("        valType=typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK];");
        writeLine("        if((valType&TYPE_SIG_MASK)<"+min2BlockSignature+"){");
        writeLine("          " + PRINT_VALUE + "("+ PRINT__STREAM_NAME +",valType,"+PRINT__VALUE_NAME+"+sizeof("+VALUE_BLOCK_NAME+")" +
                    "  /*"+PRINT__VALUE_NAME+"*/);");
        writeLine("        }else{");
        writeLine("          " + PRINT_VALUE +
                "("+ PRINT__STREAM_NAME +",valType,*(("+VALUE_BLOCK_NAME+"**)("+PRINT__VALUE_NAME+"+sizeof("+VALUE_BLOCK_NAME+")))" +
                    "/*"+PRINT__VALUE_NAME+"*/);");
        writeLine("        }");
        writeLine("        fputs(\"}\","+ PRINT__STREAM_NAME +");");
        writeLine("      }else{");
        writeLine("        fputs(\"Optional{}\","+ PRINT__STREAM_NAME +");");
        writeLine("      }");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "REFERENCE:");
        writeLine("      valType=typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK];");
        writeLine("      " + PRINT_VALUE + "("+ PRINT__STREAM_NAME +",valType,*(("+VALUE_BLOCK_NAME+"**)"+PRINT__VALUE_NAME+")/*content*/);");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "ARRAY:");
        writeLine("      data=*(("+VALUE_BLOCK_NAME+"**)"+PRINT__VALUE_NAME+");/*"+PRINT__VALUE_NAME+"->asPtr*/");
        writeLine("      valType=typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK];");
        writeLine("      size_t w;");
        writeLine("      void* min;");
        writeLine("      void* max;");
        writeLine("      if((valType&TYPE_SIG_MASK)<"+minByteSignatures[0]+"){");
        writeLine("        w=1;");
        writeLine("      }else if((valType&TYPE_SIG_MASK)<"+minByteSignatures[1]+"){");
        writeLine("        w=2;");
        writeLine("      }else if((valType&TYPE_SIG_MASK)<"+minByteSignatures[2]+"){");
        writeLine("        w=4;");
        writeLine("      }else if((valType&TYPE_SIG_MASK)<"+minByteSignatures[3]+"){");
        writeLine("        w=8;");
        writeLine("      }else if((valType&TYPE_SIG_MASK)<"+min2BlockSignature+"){");
        writeLine("        w=sizeof("+VALUE_BLOCK_NAME+");");
        writeLine("      }else{");
        writeLine("        w=2*sizeof("+VALUE_BLOCK_NAME+");");
        writeLine("      }");
        writeLine("      min=data+"+ARRAY_HEADER+"/*header size*/+data["+ARRAY_OFF_OFFSET+"]."+
                typeFieldName(Type.Numeric.SIZE)+"/*off*/;");
        writeLine("      max=min+w*(data["+ARRAY_LEN_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+")/*len*/;");
        writeLine("      fputs(\"{\","+ PRINT__STREAM_NAME +");");
        writeLine("      for(void* p=min;p<max;p+=w){");
        writeLine("        if(p>min){fputs(\",\","+ PRINT__STREAM_NAME +");}");
        writeLine("        " + PRINT_VALUE + "("+ PRINT__STREAM_NAME +",valType,p/*element*/);");
        writeLine("      }");
        writeLine("      fputs(\"}\","+ PRINT__STREAM_NAME +");");
        writeLine("      break;");
        //TODO Print Containers
        writeLine("    case " + TYPE_SIG_PREFIX + "TUPLE:");
        writeLine("    case " + TYPE_SIG_PREFIX + "UNION:");
        writeLine("    case " + TYPE_SIG_PREFIX + "STRUCT:");
        writeLine("      assert(false && \" unimplemented \");");
        writeLine("      break;");
        writeLine("    case " + TYPE_SIG_PREFIX + "PROC:");
        writeLine("      fputs(\"[procedure]\","+ PRINT__STREAM_NAME +");");
        writeLine("      break;");
        writeLine("    default:");
        writeLine("      fprintf(stderr,\"unknown type signature:%\"PRIu64,type&" + TYPE_SIG_PREFIX + "MASK);");
        writeLine("      exit("+ERR_TYPE+");");
        writeLine("      break;");
        writeLine("  }");
        writeLine("}");
        comment("log-Method");
        writeLine("void " + ID_LOG + "(" + LOG_TYPE_NAME + " logType,bool append,const Type type,const " +VALUE_BLOCK_NAME+"* value){");
        writeLine("  if(prevType!=null){");
        writeLine("    if((logType!=prevType)||(!append)){");
        writeLine("      switch(prevType){");
        writeLine("        case null:");
        writeLine("          break;");
        for(LogType.Type t:LogType.Type.values()){
            writeLine("        case "+t+":");
            writeLine("          fputs(NEW_LINE," + LOG_STREAM_PREFIX +t+");");
            writeLine("          break;");
        }
        writeLine("      }");
        writeLine("    }");
        writeLine("  }");
        writeLine("  FILE* log;");
        writeLine("  switch(logType){");
        writeLine("    case null:");
        for(LogType.Type t:LogType.Type.values()){
            writeLine("    case "+t+":");
            writeLine("      log=" + LOG_STREAM_PREFIX +t+";");
            if(t== LogType.Type.DEBUG){
                writeLine("      if(!append){");
                writeLine("        fputs(\"Debug: \",log);");
                writeLine("      }");
            }else if(t== LogType.Type.INFO){
                writeLine("      if(!append){");
                writeLine("        fputs(\"Info: \",log);");
                writeLine("      }");
            }
            writeLine("      break;");
        }
        writeLine("  }");
        writeLine("  "+PRINT_VALUE+"(log,type,value);");
        writeLine("  prevType=logType;");
        writeLine("}");
        out.newLine();
        comment("read an element from an Array");
        writeLine(VALUE_BLOCK_NAME+ "* " + ARRAY_GET_NAME + "(" +VALUE_BLOCK_NAME+
                "* array,uint64_t index,uint64_t width){");
        writeLine("  if(index<array["+ARRAY_LEN_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+"){");
        writeLine("    return (array+"+ARRAY_HEADER+")+(array["+ARRAY_OFF_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+
                "+index)*width;");
        writeLine("  }else{");
        writeLine("    fprintf(stderr,\"array index out of range:%\"PRIu64\" length:%\"PRIu64\"\\n\"," +
                "index,array["+ARRAY_LEN_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+");");
        writeLine("    exit("+ERR_INDEX+");");
        writeLine("  }");
        writeLine("}");
        comment("read a raw-element with width byteWidth from an Array");
        writeLine("void* " + ARRAY_GET_RAW_NAME + "(" +VALUE_BLOCK_NAME+"* array,uint64_t index,int byteWidth){");
        writeLine("  if(index<array["+ARRAY_LEN_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+"){");
        writeLine("    return ((void*)(array+"+ARRAY_HEADER+"))+(array["+ARRAY_OFF_OFFSET+"]."+
                typeFieldName(Type.Numeric.SIZE)+"+index)*byteWidth;");
        writeLine("  }else{");
        writeLine("    fprintf(stderr,\"array index out of range:%\"PRIu64\" length:%\"PRIu64\"\\n\",index," +
                "array["+ARRAY_LEN_OFFSET+"]."+typeFieldName(Type.Numeric.SIZE)+");");
        writeLine("    exit("+ERR_INDEX+");");
        writeLine("  }");
        writeLine("}");
        out.newLine();
    }

    /**transforms str in a string only containing [0-9A-Za-z_] */
    private String asciify(String str){
        StringBuilder ascii=new StringBuilder(str.length());
        for(char c:str.toCharArray()){
            if((c>='0'&&c<='9')||(c>='A'&&c<='Z')||(c>='a'&&c<='z')){
                ascii.append(c);
            }else if(c=='_'){
                ascii.append("__");
            }else{
                ascii.append("_").append((int)c);
            }
        }
        return ascii.toString();
        //TODO cache names
    }

    private static void writeNumber(StringBuilder out, Value v, boolean prefix) {
        Type.Numeric t=(Type.Numeric) v.getType();
        if(prefix){
            out.append(CAST_BLOCK);
        }
        out.append("{.").append(typeFieldName(t)).append("=").append(((Value.Primitive) v).getValue()).append("}");
    }

    /**Writes a value as a array of union declarations*/
    private void writeConstValueAsUnion(StringBuilder out, Value v, DataOut dataOut, boolean isFirst, boolean prefix){
        if(!isFirst){
            out.append(',');
        }
        if(v.getType() == Type.Primitive.BOOL){
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.").append(typeFieldName(Type.Primitive.BOOL)).append("=")
                    .append(((Value.Primitive) v).getValue()).append("}");
        }else if(v.getType() == Type.TYPE){
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asType=").append(typeSignature(((Value.TypeValue)v).value)).append("}");
        }else if(v.getType() instanceof Type.Numeric){
            writeNumber(out, v,  prefix);
        }else if(v.getType() instanceof Type.AnyType){
            Type contentType = ((Value.AnyValue) v).content.getType();
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asType=").append(typeSignature(contentType)).append("}");
            if(contentType.blockCount>1) {
                if (prefix) {
                    out.append(CAST_BLOCK);
                }
                String loc=dataOut.nextName();
                out.append("{.asPtr=(").append(loc).append(")}");
                StringBuilder content=dataOut.newValueBuilder(loc,contentType,1,prefix);
                writeConstValueAsUnion(content, ((Value.AnyValue) v).content, dataOut, !dataOut.constant, prefix);
                dataOut.addValueBuilder(loc,content,contentType,1);
            }else{
                writeConstValueAsUnion(out,((Value.AnyValue) v).content,dataOut,false, prefix);
            }
        }else if(v.getType() instanceof Type.Optional){
            Value content = ((Value.Optional) v).content;
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.").append(typeFieldName(Type.Primitive.BOOL)).append("=").append(content != Value.NONE).append("}");
            if(content.getType().blockCount>1) {
                if (prefix) {
                    out.append(CAST_BLOCK);
                }
                String loc=dataOut.nextName();
                out.append("{.asPtr=(").append(loc).append(")}");
                StringBuilder tmp=dataOut.newValueBuilder(loc,content.getType(),1,prefix);
                writeConstValueAsUnion(tmp, content, dataOut, !dataOut.constant, prefix);
                dataOut.addValueBuilder(loc,tmp,content.getType(),1);
            }else{
                writeConstValueAsUnion(out,content,dataOut,false, prefix);
            }
        }else if(v.getType() instanceof Type.NoRetString){
            if(prefix){out.append(CAST_BLOCK); }
            String loc=dataOut.nextName();
            out.append("{.asPtr=(").append(loc).append(")}");
            int length = ((Value.StringValue) v).length();
            StringBuilder content=dataOut.newValueBuilder(loc,v.getType(), length,prefix);
            int bitCount=((Type.NoRetString) v.getType()).charBits;
            for(int c=8;c<65;c*=2){//round to next multiple of 2
                if(bitCount<=c){
                    bitCount=c;
                    break;
                }
            }
            int j=0,c=64/bitCount;
            if(dataOut.constant){
                content.append(',');
            }
            if(prefix){content.append(CAST_BLOCK); }
            content.append("{.raw").append(bitCount).append("={");
            for (Value elt : ((Value.StringValue) v)) {
                if(j==c){
                    j=0;
                    content.append("}}");
                    content.append(',');
                    if(prefix){content.append(CAST_BLOCK); }
                    content.append("{.raw").append(bitCount).append("={");
                }
                if(j>0){
                    content.append(',');
                }
                content.append("0x").append(Integer.toHexString((((Number)((Value.Primitive)elt).getValue()).intValue())&
                        ((int)((1L<<bitCount)-1))));
                j++;
            }
            while(j<c){
                content.append(",0x0");
                j++;
            }
            content.append("}}");
            dataOut.addValueBuilder(loc,content,v.getType(),length);
        }else if(v.getType() instanceof Type.Array){
            if(prefix){out.append(CAST_BLOCK); }
            String loc=dataOut.nextName();
            out.append("{.asPtr=(").append(loc).append(")}");
            StringBuilder content=dataOut.newValueBuilder(loc,v.getType(),((Value.ArrayOrTuple) v).elements().length,prefix);
            isFirst=!dataOut.constant;
            if(((Type.Array) v.getType()).content instanceof Type.Primitive){
                int bitCount=8*((Type.Primitive) ((Type.Array) v.getType()).content).byteCount;
                for(int c=8;c<65;c*=2){//round to next multiple of 2
                    if(bitCount<=c){
                        bitCount=c;
                        break;
                    }
                }
                int j=0,c=64/bitCount;
                if(dataOut.constant){
                    content.append(',');
                }
                if(prefix){content.append(CAST_BLOCK); }
                content.append("{.raw").append(bitCount).append("={");
                for (Value elt : (Value.ArrayOrTuple) v) {
                    if(j==c){
                        j=0;
                        content.append("}}");
                        content.append(',');
                        if(prefix){content.append(CAST_BLOCK); }
                        content.append("{.raw").append(bitCount).append("={");
                    }
                    if(j>0){
                        content.append(',');
                    }
                    content.append(((Value.Primitive)elt).getValue());
                    j++;
                }
                while(j<c){
                    content.append(",0");
                    j++;
                }
                content.append("}}");
            }else{
                for (Value elt : (Value.ArrayOrTuple) v) {
                    writeConstValueAsUnion(content,elt, dataOut, isFirst, prefix);
                    isFirst=false;
                }
            }
            dataOut.addValueBuilder(loc,content,v.getType(),((Value.ArrayOrTuple) v).elements().length);
        }else if(v.getType() instanceof Type.Tuple){//TODO compress storage of tuples/structs?
            isFirst=true;
            for(Value elt:(Value.ArrayOrTuple)v){
                writeConstValueAsUnion(out,elt, dataOut, isFirst, prefix);
                isFirst=false;
            }
        }else if(v instanceof Value.Struct){
            isFirst=true;
            for(Value elt:(Value.Struct)v){
                writeConstValueAsUnion(out,elt, dataOut, isFirst, prefix);
                isFirst=false;
            }
        }else if(v == Value.NONE){
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asPtr=NULL/*none*/}");
        }else{
            throw new UnsupportedOperationException(v.getType()+" is currently not supported in the compiler");
        }
    }

    //TODO allow links to other constants in constants
    private void writeConstant(String name, Value value) throws IOException {
        comment("const "+value.getType()+" : "+name+" = "+value.stringRepresentation());
        name=asciify(name);
        StringBuilder tmp=new StringBuilder();
        DataOut constData= new DataOut( "tmp_" + CONST_PREFIX + name, 0,true);
        writeConstValueAsUnion(tmp, value, constData, true, false);
        for(String l:constData.prefixLines){
            writeLine(l);
        }
        out.write("const " + VALUE_BLOCK_NAME + " " + CONST_PREFIX +name);
        out.write(" []={");
        writeLine(tmp.append("};").toString());
    }
    private void writeProcSignature(String name, Procedure proc) throws IOException{
        String tmp=Arrays.toString(proc.argTypes());
        comment(name+"("+tmp.substring(1,tmp.length()-1)+")");
        out.write(procedureOut+ " " + PROC_PREFIX +name+"(");
        for(int i=0;i<procedureArgs.length;i++){
            if(i>0)
                out.write(",");
            out.write(procedureArgs[i]+" "+procedureArgNames[i]);
        }
        out.write(")");
    }

    private void writeProcDeclaration(String name, Procedure proc) throws IOException{
        writeProcSignature(name, proc);
        writeLine(";");
    }


    private void writeArgs(String indent, Expression[] args, ArrayList<String> initLines, StringBuilder line, String name, String[] argNames, final String argsOut) throws IOException {
        long argOff=0;
        int blockSize;
        for (Expression firstArg : args) {
            comment(indent+"{","arg1: "+firstArg.expectedType());
            blockSize = firstArg.expectedType().blockCount;
            line.append(indent).append("  memcpy,").append(argsOut).append("[").append(argOff).append("],");
            writeExpression("  ", initLines, line, firstArg, false,0, name, argNames);
            line.append(",").append(blockSize).append("*sizeof("+VALUE_BLOCK_NAME+"));");
            argOff += blockSize;
            for (String l : initLines) {
                writeLine(l);
            }
            initLines.clear();
            writeLine(line.toString());
            line.setLength(0);
            writeLine(indent+"}");
        }
    }

    private void writeProcImplementation(String name, Procedure proc) throws IOException{
        writeProcSignature(name, proc);
        writeLine("{");
        Type[] argTypes=proc.argTypes();
        String[] argNames=new String[proc.maxValues];//names of the arg-variables
        long off=0;
        int valCount=0;
        for(int i=0;i<argTypes.length;i++){
            argNames[i]= "(argsIn+"+(off)+")";
            off+=argTypes[i].blockCount;
            valCount++;
            comment("  ","var"+i+":"+argNames[i]);//tmp
        }
        if(!proc.isNative()){
            ArrayList<String> initLines=new ArrayList<>();
            StringBuilder line=new StringBuilder();
            for(Action a:proc.actions()){
                if(a instanceof ValDef){
                    argNames[valCount]="var"+valCount;
                    line.setLength(0);
                    int blockCount = ((ValDef) a).getType().blockCount;
                    comment("  " + VALUE_BLOCK_NAME + " var" +valCount+" ["+ blockCount +"];",
                            "("+((ValDef) a).getType()+")");
                    line.append("    memcpy(var").append(valCount).append(",");
                    comment("  {","Initialize: "+((ValDef) a).getInitValue());
                    writeExpression("    ",initLines,line,((ValDef) a).getInitValue(),false,0, name, argNames);
                    for(String l:initLines){
                        writeLine(l);
                    }
                    initLines.clear();
                    writeLine(line.append(",").append(blockCount).append("*sizeof("+VALUE_BLOCK_NAME+"));").toString());
                    writeLine("  }");
                    valCount++;
                }else if(a instanceof Assignment){
                    comment("  {","Assign: "+a);
                    line.setLength(0);
                    if(((Assignment) a).target.expectedType() instanceof Type.Primitive){
                        line.append("    ");
                        //target expression
                        writeExpression("    ", initLines, line, ((Assignment) a).target, true, 0, name, argNames);
                        line.append("=");
                        //source expression
                        writeExpression("    ", initLines, line, ((Assignment) a).expr, true, 0, name, argNames);
                    }else {
                        line.append("    memcpy(");
                        //target-ptr
                        writeExpression("    ", initLines, line, ((Assignment) a).target, false, 0, name, argNames);
                        line.append(", ");
                        //source-ptr
                        writeExpression("    ", initLines, line, ((Assignment) a).expr, false, 0, name, argNames);
                        line.append(", ").append(((Assignment) a).expr.expectedType().blockCount).append("*sizeof(" + VALUE_BLOCK_NAME + "))");
                    }
                    for(String l:initLines){
                        writeLine(l);
                    }
                    initLines.clear();
                    writeLine(line.append(';').toString());
                    writeLine("  }");
                }else if(a instanceof LogAction){
                    comment("  {","Log: "+a);
                    line.setLength(0);
                    line.append("    " + ID_LOG + "(").append(((LogAction) a).type.type).append(",")
                            .append(((LogAction) a).type.append).append(',')
                            .append(typeSignature(((LogAction) a).expr.expectedType()))
                            .append(",");
                    writeExpression("    ",initLines,line,((LogAction) a).expr,false,0, name, argNames);
                    for(String l:initLines){
                        writeLine(l);
                    }
                    initLines.clear();
                    writeLine(line.append(");").toString());
                    writeLine("  }");
                }else{
                    throw new UnsupportedOperationException("Unsupported ActionType: "+a.getClass().getSimpleName());
                }
            }
            //TODO cleanup memory
            Procedure.ProcChild firstStatic=null;
            Expression[] firstArgs=null;
            ArrayList<Procedure.ProcChild> children= proc.children();
            ArrayList<Expression[]> childArgs= proc.childArguments();
            for(int i=0;i<children.size();i++){
                Procedure.ProcChild c=children.get(i);
                if(c instanceof Procedure.StaticProcChild||
                        (c instanceof Procedure.DynamicProcChild&&!((Procedure.DynamicProcChild) c).isOptional)){
                    firstStatic=children.remove(i);
                    firstArgs=childArgs.remove(i);
                    //static procedure
                    break;
                }
            }
            initLines.clear();
            line.setLength(0);
            if(firstStatic!=null){
                //TODO allow multiple child-procedure calls (pthreads)
                //writeAllOtherCalls
                for(int i=0;i<children.size();i++){
                    Procedure.ProcChild c=children.get(i);
                    comment("  ","Call:"+c);
                    if(c instanceof Procedure.StaticProcChild||
                            (c instanceof Procedure.DynamicProcChild&&!((Procedure.DynamicProcChild) c).isOptional)){
                        writeLine("  {");
                        writeLine("    "+VALUE_BLOCK_NAME+"* newArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
                        writeLine("    if(newArgs==NULL){");//check pointer
                        writeLine("      fputs(\"out of memory\\n\",stderr);");
                        writeLine("      exit("+ERR_MEM+");");
                        writeLine("    }");
                        writeArgs("    ",childArgs.get(i),initLines,line,name,argNames,"newArgs");
                        //addLater call function (via pthreads)
                        writeLine("    assert(false && \"unimplemented\");");
                        writeLine("  }");
                        //static procedure
                        break;
                    }else{
                        writeLine("  if("+argNames[((Procedure.DynamicProcChild)children.get(i)).varId]+"[0]." +
                                ""+typeFieldName(Type.Primitive.BOOL)+"){");
                        writeLine("    "+VALUE_BLOCK_NAME+"* newArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
                        writeLine("    if(newArgs==NULL){");//check pointer
                        writeLine("      fputs(\"out of memory\\n\",stderr);");
                        writeLine("      exit("+ERR_MEM+");");
                        writeLine("    }");
                        writeArgs("    ",childArgs.get(i),initLines,line,name,argNames,"newArgs");
                        //addLater call function (via pthreads)
                        writeLine("      assert(false && \"unimplemented\");");
                        writeLine("  }");
                    }
                }
                comment("  ","Call:"+firstStatic);
                writeArgs("  ",firstArgs, initLines, line, name, argNames, "argsOut");
                if(firstStatic instanceof Procedure.StaticProcChild){
                    writeLine("  return &"+PROC_PREFIX+asciify(((Procedure.StaticProcChild) firstStatic).name)+";");
                }else{
                    writeLine("  return "+argNames[((Procedure.DynamicProcChild)firstStatic).varId]+"[0].asProc");
                }
            }else{
                //all children are optional
                writeLine("  "+PROCEDURE_TYPE+" ret=NULL;");
                for(int i=0;i<children.size();i++){
                    comment("  ","Call:"+children.get(i));
                    writeLine("  if("+argNames[((Procedure.DynamicProcChild)children.get(i)).varId]+"[0]."+
                            typeFieldName(Type.Primitive.BOOL)+"){");
                    writeLine("    if(ret==NULL){");
                    writeArgs("      ",childArgs.get(i), initLines, line, name, argNames, "argsOut");
                    writeLine("      ret="+argNames[((Procedure.DynamicProcChild)children.get(i)).varId]+"[1].asProc;");
                    writeLine("    }else{");
                    writeLine("    "+VALUE_BLOCK_NAME+"* newArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
                    writeLine("    if(newArgs==NULL){");//check pointer
                    writeLine("      fputs(\"out of memory\\n\",stderr);");
                    writeLine("      exit("+ERR_MEM+");");
                    writeLine("    }");
                    writeArgs("    ",childArgs.get(i),initLines,line,name,argNames,"newArgs");
                    //addLater call function (via pthreads)
                    writeLine("      assert(false && \"unimplemented\");");
                    writeLine("    }");
                    writeLine("  }");
                }
                writeLine("  return ret;");
            }
        }else {
            comment("  ", "Native");
            //TODO implementations of native procedures
            //? own syntax for native procedures:
            //native out name(in1,in2,in3);
            //natives act like procedures with the signature (in1,in2,in3,$a,(out,$a)=>?)
            //code implementation: unwrap ins => call native function => wrap out => call child function
            // code signatures:
            // - native_proc_[name] wrapping and unwrapping of values
            // - [name] native procedure code
            writeLine("  return NULL;");
        }
        writeLine("}");
        out.newLine();
    }

    private String[] unOpParts(LeftUnaryOp op,boolean unwrap) {
        String[] parts=new String[2];
        Type inType=op.expr.expectedType();
        Type outType=op.expectedType();
        switch (op.op){
            case PLUS:
                if(inType instanceof Type.Numeric){
                    return null;
                }else{
                    throw new SyntaxError("unsupported type for +:"+inType);
                }
            case MINUS:
                if(inType instanceof Type.Numeric){
                    parts[0]=(unwrap?"(":("(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{."+ typeFieldName((Type.Numeric)outType)+
                            "="))+"-("+ cTypeName((Type.Numeric)outType)+")(";
                    parts[1]=unwrap?")":")}})";
                    return parts;
                }else{
                    throw new SyntaxError("unsupported type for -:"+inType);
                }
            case FLIP:
                if(inType instanceof Type.Numeric&&!((Type.Numeric)outType).isFloat){
                    parts[0]=(unwrap?"(":("(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{."+ typeFieldName((Type.Numeric)outType)+
                            "="))+"~("+ cTypeName((Type.Numeric)outType)+")(";
                    parts[1]=unwrap?")":")}})";
                    return parts;
                }else{
                    throw new SyntaxError("unsupported type for ~:"+inType);
                }
            case NOT:
                if(inType == Type.Primitive.BOOL){
                    parts[0]=(unwrap?"(":("(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{."+ typeFieldName((Type.Numeric)outType)+
                            "="))+"!("+ cTypeName((Type.Numeric)outType)+")(";
                    parts[1]=unwrap?")":")}})";
                    return parts;
                }
                throw new SyntaxError("unsupported type for !:"+inType);
            case DIV:
            case INT_DIV:
            case MULT:
            case MOD:
            case POW:
            case AND:
            case OR:
            case XOR:
            case FAST_AND:
            case FAST_OR:
            case GT:
            case GE:
            case NE:
            case EQ:
            case LT:
            case LE:
            case IF:
            case LSHIFT:
            case RSHIFT:
                throw new SyntaxError(op.op+" is not a Unary Operation");
        }
        throw new RuntimeException("Unreachable");
    }

    private String[] binOpParts(BinOp op,boolean unwrap) {
        String[] parts=new String[3];
        Type lType=op.left.expectedType();
        Type rType=op.right.expectedType();
        Type outType=op.expectedType();
        //default values, this values are correct for most bin-ops
        parts[0]=(unwrap?"(":("(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{."+ typeFieldName((Type.Primitive)outType)+
                "=("))+"("+cTypeName((Type.Primitive)outType)+")(";
        parts[2]=unwrap?"))":"))}})";
        switch (op.op){
            case PLUS:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="+";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case MINUS:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="-";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case MULT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="*";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case DIV:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    if((((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                        parts[1]="/";
                    }else{
                        //TODO different handling for int/int
                        throw new UnsupportedOperationException("unimplemented");
                    }
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case MOD:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="%";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case INT_DIV:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    if((((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                        //TODO different handling for float/float
                        throw new UnsupportedOperationException("unimplemented");
                    }else{
                        parts[1]="/";
                        return parts;
                    }
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case POW:
                throw new UnsupportedOperationException("unimplemented:"+op.op);
            case AND:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                    !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[1]="&";
                    return parts;
                }else if(lType== Type.Primitive.BOOL&&rType== Type.Primitive.BOOL){
                    parts[1]="&&";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case OR:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[1]="|";
                    return parts;
                }else if(lType== Type.Primitive.BOOL&&rType== Type.Primitive.BOOL){
                    parts[1]="||";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case XOR:
                if((lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat))||
                        (lType== Type.Primitive.BOOL&&rType== Type.Primitive.BOOL)){
                    parts[1]="^";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case LSHIFT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[1]="<<";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case RSHIFT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[1]=">>";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case FAST_AND://addLater separate handling of fastAnd if(cond1){res=cond2}else{res=false}
                if(lType== Type.Primitive.BOOL&&rType== Type.Primitive.BOOL){
                    parts[1]="&&";
                    return parts;
                }else{
                    throw new SyntaxError(OperatorType.FAST_AND+" only exists for "+ Type.Primitive.BOOL);
                }
            case FAST_OR://addLater separate handling of fastOr if(cond1){res=true}else{res=cond2}
                if(lType== Type.Primitive.BOOL&&rType== Type.Primitive.BOOL){
                    parts[1]="||";
                    return parts;
                }else{
                    throw new SyntaxError(OperatorType.FAST_OR+" only exists for "+ Type.Primitive.BOOL);
                }
            case NE:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="==";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case EQ:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="!=";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case GT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]=">";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case GE:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]=">=";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case LT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="<";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case LE:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[1]="<=";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case NOT:
            case FLIP:
            case IF:
                throw new SyntaxError(op.op+" is no binary operation");
        }
        throw new RuntimeException("Unreachable");
    }
    private String[] typecastParts(Type to, Type from,boolean unwrap) {
        String[] parts=new String[2];
        if(from instanceof Type.Numeric&&to instanceof Type.Numeric){
            if(unwrap){
                parts[0]="(("+cTypeName((Type.Numeric)to)+")(";
                parts[1]="))";
            }else{
                parts[0]="(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{."+ typeFieldName((Type.Numeric)to)+"=("+cTypeName((Type.Numeric)to)+")(";
                parts[1]="[0]."+ typeFieldName((Type.Numeric)from)+")}})";
            }
            return parts;
        }else if(to instanceof Type.Optional&&from.blockCount==1){
            if(unwrap){
                throw new RuntimeException("cannot unwrap Optional");
            }
            if(from instanceof Type.Primitive){
                parts[0] = "((" + VALUE_BLOCK_NAME + "[]){" + CAST_BLOCK + "{."+typeFieldName(Type.Primitive.BOOL)+"="
                        + (from != Type.NONE_TYPE) + "}," + CAST_BLOCK + "{."+typeFieldName((Type.Primitive) from)+"=";
                parts[1] = "}})";
            }else {
                parts[0] = "((" + VALUE_BLOCK_NAME + "[]){" + CAST_BLOCK + "{."+typeFieldName(Type.Primitive.BOOL)+"="
                        + (from != Type.NONE_TYPE) + "},(";
                parts[1] = ")[0]})";
            }
            return parts;
        }else if(to instanceof Type.AnyType&&from.blockCount==1){
            if(unwrap){
                throw new RuntimeException("cannot unwrap Any");
            }
            parts[0] = "((" + VALUE_BLOCK_NAME + "[]){" + CAST_BLOCK + "{.asType=" + typeSignature(from) + "},";
            if(from instanceof Type.Primitive){
                parts[0] += CAST_BLOCK+"{."+typeFieldName((Type.Primitive) from)+"=";
                parts[1] = "}})";
            }else {
                parts[0] +="(";
                parts[1] = ")[0]})";
            }
            return parts;
        }else if(to== Type.Primitive.BOOL &&from instanceof Type.Optional){
            if(unwrap){
                parts[0]="((";
                parts[1]=")[0]."+typeFieldName(Type.Primitive.BOOL)+")";
            }else{
                parts[0]="(";
                parts[1]=")";
            }
            return parts;
        }else{
            throw new UnsupportedOperationException("Cast from "+from+" to "+to+" is currently not implemented");
        }
    }

    private String unwrapSuffix(Type type){
        if(type instanceof Type.Primitive){
            return "[0]."+typeFieldName((Type.Primitive) type);
        }else{
            throw new RuntimeException("Cannot unwrap "+type);
        }
    }

    private int writeExpression(String indent, ArrayList<String> initLines, StringBuilder line, Expression expr,
                                boolean unwrap,int tmpCount, String procName, String[] varNames){
        if(expr instanceof VarExpression){
            line.append(varNames[((VarExpression)expr).varId]);
            if(unwrap){
                line.append(unwrapSuffix(expr.expectedType()));
            }
            return tmpCount;
        }else if(expr instanceof ThisExpr){
            if(unwrap){
                throw new RuntimeException("Cannot unwrap procedures");
            }
            line.append("(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{.asProc=&" + PROC_PREFIX).append(procName).append("}})");
            return tmpCount;
        }else if(expr instanceof ValueExpression){
            if(((ValueExpression) expr).constId!=null){
                line.append(CONST_PREFIX).append(asciify(((ValueExpression) expr).constId));
                if(unwrap){
                    line.append(unwrapSuffix(expr.expectedType()));
                }
                return tmpCount;
            }else{
                boolean needsExternalData;
                if(((ValueExpression)expr).getValue() instanceof Value.AnyValue){
                    //special handling for AnyValues, needsExternalData depends on content
                    Value content=((Value.AnyValue) ((ValueExpression)expr).getValue()).content;
                    needsExternalData=content.getType().blockCount>1||content.getType().needsExternalData;
                }else{
                    needsExternalData=expr.expectedType().needsExternalData;
                }
                if(needsExternalData){
                    if(unwrap){
                        throw new RuntimeException("Cannot unwrap "+expr.expectedType());
                    }
                    int blockCount=expr.expectedType().blockCount;
                    DataOut data= new DataOut("tmp", tmpCount + 1,false);
                    StringBuilder tmp;
                    initLines.add(indent+"Value tmp"+tmpCount+" ["+blockCount+"];");
                    tmp=new StringBuilder("  memcpy(tmp"+tmpCount+",("+VALUE_BLOCK_NAME+"[]){");
                    writeConstValueAsUnion(tmp,((ValueExpression)expr).getValue(),data,true, true);
                    tmp.append("},").append(blockCount).append("*sizeof("+VALUE_BLOCK_NAME+"));");
                    initLines.add(indent+"{");
                    for(String l:data.prefixLines){
                        initLines.add(indent+"  "+l);
                    }
                    initLines.add(indent+tmp);
                    initLines.add(indent+"}");
                    line.append("tmp").append(tmpCount);
                    return tmpCount+1;
                }else{
                    if(unwrap){
                        if(!(expr.expectedType() instanceof Type.Primitive)){
                            throw new RuntimeException("Cannot unwrap "+expr.expectedType());
                        }
                        line.append("((").append(cTypeName((Type.Primitive)expr.expectedType())).append(")(")
                                .append(((ValueExpression)expr).getValue().stringValue()).append("))");
                    }else{
                        line.append("(("+VALUE_BLOCK_NAME+"[]){");
                        writeConstValueAsUnion(line,((ValueExpression)expr).getValue(),null,true, true);
                        line.append("})");
                    }
                    return tmpCount;
                }
            }
        }else if(expr instanceof BinOp){
            String[] parts=binOpParts(((BinOp) expr),unwrap);
            line.append(parts[0]);
            tmpCount=writeExpression(indent,initLines,line,((BinOp)expr).left,true,tmpCount, procName, varNames);
            line.append(parts[1]);
            tmpCount=writeExpression(indent,initLines,line,((BinOp)expr).right,true,tmpCount, procName, varNames);
            line.append(parts[2]);
            return tmpCount;
        }else if(expr instanceof LeftUnaryOp){
            String[] parts=unOpParts(((LeftUnaryOp) expr),unwrap);
            if(parts!=null){
                line.append(parts[0]);
                tmpCount=writeExpression(indent,initLines,line,((LeftUnaryOp)expr).expr,true,tmpCount, procName, varNames);
                line.append(parts[1]);
            }else{
                tmpCount=writeExpression(indent,initLines,line,((LeftUnaryOp)expr).expr,unwrap,tmpCount, procName, varNames);
            }
            return tmpCount;
        }else if(expr instanceof TypeCast){
            String[] parts=typecastParts(((TypeCast) expr).type,((TypeCast) expr).value.expectedType(),unwrap);
            line.append(parts[0]);
            tmpCount=writeExpression(indent,initLines,line,((TypeCast)expr).value,((TypeCast)expr).value.expectedType() instanceof Type.Primitive,
                    tmpCount, procName, varNames);
            line.append(parts[1]);
            return tmpCount;
        }else if(expr instanceof IfExpr){
            if(unwrap){
                //TODO handle unwrapping of if-expressions
                throw new UnsupportedOperationException("Unwrapping of IfExpr is currently not supported");
            }else{
                //addLater use ?: if both arguments can be inlined
                // handle primitive values
                int blockCount = expr.expectedType().blockCount;
                initLines.add(indent+"Value tmp"+tmpCount+" ["+blockCount+"];");
                int prevTmp=tmpCount++;
                initLines.add(indent+"{");
                StringBuilder tmp=new StringBuilder(indent+"  if(");
                tmpCount=writeExpression(indent+"    ",initLines,tmp,((IfExpr)expr).cond,true,tmpCount, procName, varNames);
                initLines.add(tmp.append("){").toString());
                tmp = new StringBuilder(indent + "    memcpy(tmp" + prevTmp + ",");
                tmpCount=writeExpression(indent,initLines,tmp,((IfExpr)expr).ifVal,false, tmpCount,procName, varNames);
                tmp.append(",").append(blockCount).append("*sizeof("+VALUE_BLOCK_NAME+"))");
                initLines.add(tmp.append(";").toString());
                initLines.add(indent+"  }else{");
                tmp = new StringBuilder(indent + "    memcpy(tmp" + prevTmp + ",");
                writeExpression(indent+"    ",initLines,tmp,((IfExpr)expr).elseVal,false, tmpCount, procName, varNames);
                tmp.append(",").append(blockCount).append("*sizeof("+VALUE_BLOCK_NAME+"))");
                initLines.add(tmp.append(";").toString());
                initLines.add(indent+"  }");
                initLines.add(indent+"}");
                line.append("tmp").append(prevTmp);
                return prevTmp;
            }
        }else if(expr instanceof GetField){
            if(((GetField) expr).fieldName.equals(Type.FIELD_NAME_TYPE)){
                if(((GetField) expr).value.expectedType() instanceof Type.AnyType){
                    tmpCount=writeExpression(indent+"    ",initLines,line,((GetField) expr).value,false,tmpCount, procName, varNames);
                }else{
                    line.append("(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK + "{.asType=").append(typeSignature(((GetField) expr).value.expectedType()))
                            .append("}})");
                    if(unwrap){
                        line.append(unwrapSuffix(expr.expectedType()));
                    }
                }
                return tmpCount;
            }else if(((GetField) expr).fieldName.equals(Type.FIELD_NAME_LENGTH)){
                if((((GetField) expr).value.expectedType() instanceof Type.Array||
                        ((GetField) expr).value.expectedType() instanceof Type.NoRetString)){
                    line.append('(');
                    writeExpression(indent,initLines,line, ((GetField) expr).value,false, tmpCount,procName,varNames);
                    line.append("[0].asPtr+"+ARRAY_LEN_OFFSET+")");//array.length
                    if(unwrap){
                        line.append(unwrapSuffix(expr.expectedType()));
                    }
                    return tmpCount;
                }else if((((GetField) expr).value.expectedType() instanceof Type.Tuple)){
                    if(!unwrap){
                        line.append("(("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK + "{.")
                                .append(typeFieldName(Type.Numeric.SIZE)).append("=");
                    }
                    line.append(((Type.Tuple) ((GetField) expr).value.expectedType()).getElements().length);
                    if(!unwrap){
                        line.append("}})");
                    }
                    return tmpCount;
                }
            }else if(((GetField) expr).value.expectedType() instanceof Type.Optional&&
                    ((GetField) expr).fieldName.equals(Type.FIELD_NAME_VALUE)){
                //TODO check value before access, dereference pointers
                line.append("((");
                writeExpression(indent,initLines,line, ((GetField) expr).value,false, tmpCount,procName,varNames);
                line.append(")+1)");//optional.length
                if(unwrap){
                    line.append(unwrapSuffix(expr.expectedType()));
                }
                return tmpCount;
            }

            //TODO struct/union field access
            throw new UnsupportedOperationException(expr.getClass().getSimpleName()+" is currently not supported");
        }else if(expr instanceof GetIndex){
            if(((GetIndex)expr).value.expectedType() instanceof Type.Array){
                if(((Type.Array) ((GetIndex)expr).value.expectedType()).content instanceof Type.Primitive){
                    Type.Primitive content=(Type.Primitive)((Type.Array) ((GetIndex)expr).value.expectedType()).content;
                    if(!unwrap){
                        line.append("((" + VALUE_BLOCK_NAME + "[]){" + CAST_BLOCK + "{.").append(typeFieldName(content))
                                .append("=");
                    }
                    line.append("*((").append(cTypeName(content)).append("*)" + ARRAY_GET_RAW_NAME + "(");
                    tmpCount=writeExpression(indent,initLines,line, ((GetIndex) expr).value,false,tmpCount,procName,varNames);
                    line.append("->asPtr,");
                    tmpCount=writeExpression(indent,initLines,line, ((GetIndex) expr).index,true, tmpCount,procName,varNames);
                    line.append(",").append(content.byteCount).append("))");
                    if(!unwrap){
                        line.append("}})");
                    }
                }else {
                    line.append(ARRAY_GET_NAME + "(");
                    tmpCount = writeExpression(indent, initLines, line, ((GetIndex) expr).value, false, tmpCount, procName, varNames);
                    line.append("->asPtr,");
                    tmpCount = writeExpression(indent, initLines, line, ((GetIndex) expr).index, true, tmpCount, procName, varNames);
                    line.append(",").append(((GetIndex) expr).value.expectedType().blockCount).append(")");
                    if (unwrap) {
                        line.append(unwrapSuffix(expr.expectedType()));
                    }
                }
                return tmpCount;
            }else if(((GetIndex)expr).value.expectedType() instanceof Type.NoRetString){
                Type.Numeric charType=((Type.NoRetString) ((GetIndex)expr).value.expectedType()).charType;
                if(!unwrap){
                    line.append("((" + VALUE_BLOCK_NAME + "[]){" + CAST_BLOCK + "{.").append(typeFieldName(charType))
                            .append("=");
                }
                line.append("*((").append(cTypeName(charType)).append("*)" + ARRAY_GET_RAW_NAME + "(");
                tmpCount=writeExpression(indent,initLines,line, ((GetIndex) expr).value,false,tmpCount,procName,varNames);
                line.append("->asPtr,");
                tmpCount=writeExpression(indent,initLines,line, ((GetIndex) expr).index,true, tmpCount,procName,varNames);
                line.append(",").append((((Type.NoRetString) ((GetIndex)expr).value.expectedType()).charBits +7)/8).append("))");
                if(!unwrap){
                    line.append("}})");
                }
                return tmpCount;
            }else if(((GetIndex)expr).value.expectedType() instanceof Type.Tuple){
                //addLater get tuple elements
                throw new UnsupportedOperationException("unimplemented");
            }
            throw new UnsupportedOperationException(expr.getClass().getSimpleName()+" is currently not supported");
            //return tmpCount;
        }else {
            //TODO other expressions
            throw new UnsupportedOperationException(expr.getClass().getSimpleName()+" is currently not supported");
            //return tmpCount;
        }
    }


    private void writeRunSignature() throws IOException {
        comment(" main procedure handling function (written in a way that allows easy usage in pthreads)");
        out.write("void* noRet_run(void* initState)");
    }
    private void declareRun() throws IOException {
        writeRunSignature();
        writeLine(";");
    }

    /**writes an integrated interpreted that runs the NoRet C-Representation as a C-Program*/
    private void writeMain(boolean hasArgs) throws IOException {
        writeRunSignature();writeLine("{");
        writeLine("    " + PROCEDURE_TYPE + " f=*((Procedure*)initState);");
        writeLine("    initState+=sizeof(" + PROCEDURE_TYPE + ");");
        writeLine("    " + VALUE_BLOCK_NAME + "* argsI=*(("+VALUE_BLOCK_NAME+"**)initState);");
        writeLine("    initState+=sizeof(" + VALUE_BLOCK_NAME + "*);");
        writeLine("    " + VALUE_BLOCK_NAME + "* argsO=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
        writeLine("    " + VALUE_BLOCK_NAME + "* argsTmp;");
        writeLine("    if(argsO==NULL){");
        writeLine("      fputs(\"out of memory\\n\",stderr);");
        writeLine("      exit("+ERR_MEM+");");
        writeLine("    }");
        writeLine("    do{");
        writeLine("        f=(" + PROCEDURE_TYPE + ")f(argsI,argsO);");
        comment("        ","swap args");
        writeLine("        argsTmp=argsI;");
        writeLine("        argsI=argsO;");
        writeLine("        argsO=argsTmp;");
        writeLine("    }while(f!=NULL);");
        writeLine("    return (void*)"+ERR_NONE+";");
        writeLine("}");
        out.newLine();
        comment("main method of the C representation: ");
        comment("  transforms the input arguments and starts the noRet_run function on this thread");
        if(hasArgs){
            writeLine("int main(int argc,char** argv){");
            //ignore first argument for consistency with interpreter
        }else{
            writeLine("int main(){");
        }
        comment("  ","check type assumptions");
        writeLine("  assert(sizeof(Value)==8);");
        writeLine("  assert(sizeof(float32_t)==4);");
        writeLine("  assert(sizeof(float64_t)==8);");
        comment("  ","[proc_ptr,args_ptr,arg_data]");
        writeLine("  char init[sizeof(" + PROCEDURE_TYPE + ")+2*sizeof("+VALUE_BLOCK_NAME+"*)];");
        writeLine("  size_t off=0;");
        writeLine("  *((" + PROCEDURE_TYPE + "*)init)=&" + PROC_PREFIX + "start;");
        writeLine("  off+=sizeof(" + PROCEDURE_TYPE + ");");
        writeLine("  " + VALUE_BLOCK_NAME + "* initArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
        writeLine("  if(initArgs==NULL){");
        writeLine("    fputs(\"out of memory\\n\",stderr);");
        writeLine("    return "+ERR_MEM+";");
        writeLine("  }");
        writeLine("  *((" + VALUE_BLOCK_NAME + "**)(init+off))=initArgs;");
        writeLine("  off+=sizeof(" + VALUE_BLOCK_NAME + "*);");
        if(hasArgs){
            comment("  ","prepare program Arguments");
            comment("  ","!!! currently only UTF-8 encoding is supported !!!");//addLater support for other encodings of argv
            writeLine("  "+VALUE_BLOCK_NAME+"* argArray=malloc(((argc-1)+"+ARRAY_HEADER+")*sizeof("+VALUE_BLOCK_NAME+"));");
            writeLine("  if(argArray==NULL){");
            writeLine("    fputs(\"out of memory\\n\",stderr);");
            writeLine("    return "+ERR_MEM+";");
            writeLine("  }");
            writeLine("  argArray["+ARRAY_OFF_OFFSET+"] = "+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"=0 /*off*/};");
            writeLine("  argArray["+ARRAY_CAP_OFFSET+"] = "+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"=(argc-1) /*cap*/};");
            writeLine("  argArray["+ARRAY_LEN_OFFSET+"] = "+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"=(argc-1) /*len*/};");
            writeLine("  initArgs[0] = "+CAST_BLOCK+"{.asPtr=argArray};");
            writeLine("  for(int i=1;i<argc;i++){");//skip first argument
            writeLine("    int l=strlen(argv[i]);");
            writeLine("    "+VALUE_BLOCK_NAME+"* tmp=malloc(((l+7)/8+"+ARRAY_HEADER+")*sizeof("+VALUE_BLOCK_NAME+"));");
            writeLine("    if(tmp==NULL){");
            writeLine("      fputs(\"out of memory\\n\",stderr);");
            writeLine("      return "+ERR_MEM+";");
            writeLine("    }");
            writeLine("    tmp["+ARRAY_OFF_OFFSET+"] = "+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"=0/*off*/};");
            writeLine("    tmp["+ARRAY_CAP_OFFSET+"] = "+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"=(l+7)/8 /*cap*/};");
            writeLine("    tmp["+ARRAY_LEN_OFFSET+"] = "+CAST_BLOCK+"{."+typeFieldName(Type.Numeric.SIZE)+"=l /*len*/};");
            //store lengths of arguments
            writeLine("    argArray[i+"+(ARRAY_HEADER-1)+"] = "+CAST_BLOCK+"{.asPtr=tmp};");//pointer to data
            comment("    off="+ARRAY_HEADER+";","reuse off variable");
            writeLine("    for(int j=0,k=0;j+k<l;j++){");
            writeLine("      if(j==8){");
            writeLine("        j=0;");
            writeLine("        k+=8;");
            writeLine("        off++;");
            writeLine("      }");
            writeLine("      tmp[off].raw8[j]=argv[i][j+k];");
            writeLine("    }");
            writeLine("    off++;");
            writeLine("  }");
        }
        writeLine("  initLogStreams();");
        writeLine("  noRet_run(init);");
        comment("  puts(\"\");","finish last line in stdout");
        writeLine("  return "+ERR_NONE+";");
        writeLine("}");
    }

    public void compile(Parser.ParserContext context) throws IOException {
        writeFileHeader(context.maxArgSize(),context);
        for(Map.Entry<String, Value> e:context.constants.entrySet()){
            writeConstant(e.getKey(),e.getValue());
        }
        out.newLine();
        //TODO handle native procedures
        for(Map.Entry<String, Procedure> e:context.procNames.entrySet()){
            writeProcDeclaration(asciify(e.getKey()),e.getValue());
        }
        Procedure start= context.getProc("start");
        if(start!=null){
            declareRun();
        }
        out.newLine();
        for(Map.Entry<String, Procedure> e:context.procNames.entrySet()){
            writeProcImplementation(asciify(e.getKey()),e.getValue());
        }
        if(start!=null){
            Type[] startTypes= start.argTypes();
            if(startTypes.length>0){
                if (startTypes.length != 1 || !(startTypes[0] instanceof Type.Array) ||
                        ((Type.Array) startTypes[0]).content != Type.NoRetString.STRING8) {
                    throw new SyntaxError("wrong signature of start, " +
                            "expected ()=>? or (string[])=>?");
                }
                writeMain(true);
            }else{
                writeMain(false);
            }
        }
        out.flush();
    }

}
