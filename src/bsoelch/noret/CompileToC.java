package bsoelch.noret;

import bsoelch.noret.lang.*;
import bsoelch.noret.lang.expression.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CompileToC {
    public static final String VALUE_BLOCK_NAME = "Value";
    public static final String CAST_BLOCK = "("+VALUE_BLOCK_NAME+")";

    private static final String[] procedureArgs     ={VALUE_BLOCK_NAME + "*", VALUE_BLOCK_NAME+"*", VALUE_BLOCK_NAME + "**"};
    private static final String[] procedureArgNames ={"argsIn",               "argsOut",            "argData"};
    private static final String procedureOut="void*";

    private static final String CONST_DATA_NAME = "constData";
    private static final String CONST_DATA_SIGNATURE = VALUE_BLOCK_NAME + " " +CONST_DATA_NAME+" []";

    private static final long LEN_MASK_IN_PLACE = 0x0000000000000000L;
    private static final long LEN_MASK_CONST    = 0x8000000000000000L;
    private static final long LEN_MASK_LOCAL    = 0x4000000000000000L;
    private static final long LEN_MASK_TMP      = 0xC000000000000000L;
    public static final String PROC_PREFIX = "proc_";
    public static final String CONST_PREFIX = "const_";
    public static final String PROCEDURE_TYPE = "Procedure";

    private static class DataOut {
        final StringBuilder build;
        long off;
        final String name;
        final long mask;
        private DataOut(String prefix,long off,String name,long mask){
            build=new StringBuilder(prefix);
            this.off=off;
            this.name=name;
            this.mask=mask;
        }
    }

    private final BufferedWriter out;
    private long typeDataOff=0;
    private final HashMap<Type,Long> typeOffsets=new HashMap<>();
    private StringBuilder typeDataDeclarations;

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

    private Long typeOffset(Type t) {
        Long off = typeOffsets.get(t);
        if (off == null) {
            typeOffsets.put(t, typeDataOff);
            if(typeDataOff>0){
                typeDataDeclarations.append(',');
            }
            typeDataDeclarations.append(typeSignature(t));
            return typeDataOff++;
        }
        return off;
    }

    private String typeSignature(Type t){
        if(t==Type.EMPTY_TYPE){
            return "TYPE_SIG_EMPTY";
        }else if(t==Type.Primitive.BOOL){
            return "TYPE_SIG_BOOL";
        }else if(t==Type.Numeric.INT8){
            return "TYPE_SIG_I8";
        }else if(t==Type.Numeric.UINT8){
            return "TYPE_SIG_U8";
        }else if(t==Type.Numeric.INT16){
            return "TYPE_SIG_I16";
        }else if(t==Type.Numeric.UINT16){
            return "TYPE_SIG_U16";
        }else if(t==Type.Numeric.INT32){
            return "TYPE_SIG_I32";
        }else if(t==Type.Numeric.UINT32){
            return "TYPE_SIG_U32";
        }else if(t==Type.Numeric.INT64){
            return "TYPE_SIG_I64";
        }else if(t==Type.Numeric.UINT64){
            return "TYPE_SIG_U64";
        }else if(t==Type.Numeric.FLOAT32){
            return "TYPE_SIG_F32";
        }else if(t==Type.Numeric.FLOAT64){
            return "TYPE_SIG_F64";
        }else if(t==Type.NoRetString.STRING8){
            return "TYPE_SIG_STRING8";
        }else if(t==Type.NoRetString.STRING16){
            return "TYPE_SIG_STRING16";
        }else if(t==Type.NoRetString.STRING32){
            return "TYPE_SIG_STRING32";
        }else if(t==Type.TYPE){
            return "TYPE_SIG_TYPE";
        }else if(t==Type.Primitive.NONE_TYPE){
            return "TYPE_SIG_NONE";
        }else if(t==Type.Primitive.ANY){
            return "TYPE_SIG_ANY";
        }else if(t instanceof Type.Optional){
            typeSignature(((Type.Optional) t).content);//ensure type-signature exists
            Long off = typeOffset(((Type.Optional) t).content);
            return "TYPE_SIG_OPTIONAL|("+off+"<<TYPE_CONTENT_SHIFT)";
        }else if(t instanceof Type.Reference){
            typeSignature(((Type.Reference) t).content);//ensure type-signature exists
            Long off = typeOffset(((Type.Reference) t).content);
            return "TYPE_SIG_REFERENCE|("+off+"<<TYPE_CONTENT_SHIFT)";
        }else if(t instanceof Type.Array){
            typeSignature(((Type.Array) t).content);//ensure type-signature exists
            Long off = typeOffset(((Type.Array) t).content);
            return "TYPE_SIG_ARRAY|("+off+"<<TYPE_CONTENT_SHIFT)";
        }else if(t instanceof Type.Proc){
            //TODO block-type signatures
            throw new UnsupportedOperationException("signatures of Block-Types are currently not implemented");
           // return "TYPE_SIG_PROC|("+off+"<<TYPE_CONTENT_SHIFT)";
        }else if(t instanceof Type.Struct){
            throw new UnsupportedOperationException("signatures of Block-Types are currently not implemented");
          //  return "TYPE_SIG_STRUCT|("+off+"<<TYPE_CONTENT_SHIFT)";
        }
        throw new UnsupportedOperationException("unsupported Type :"+t);
    }


    private void writeFileHeader(long maxArgSize) throws IOException {
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
        writeLine("#define LEN_MASK_IN_PLACE 0x"+Long.toHexString(LEN_MASK_IN_PLACE));
        writeLine("#define LEN_MASK_CONST    0x"+Long.toHexString(LEN_MASK_CONST));
        writeLine("#define LEN_MASK_LOCAL    0x"+Long.toHexString(LEN_MASK_LOCAL));
        writeLine("#define LEN_MASK_TMP      0x"+Long.toHexString(LEN_MASK_TMP));
        out.newLine();
        //Type enum
        comment("Type Definitions");
        writeLine("typedef uint64_t Type;");
        writeLine("#define TYPE_SIG_MASK       0xff");
        writeLine("#define TYPE_SIG_EMPTY      0x0");
        writeLine("#define TYPE_SIG_BOOL       0x1");
        writeLine("#define TYPE_SIG_I8         0x2");
        writeLine("#define TYPE_SIG_U8         0x3");
        writeLine("#define TYPE_SIG_I16        0x4");
        writeLine("#define TYPE_SIG_U16        0x5");
        writeLine("#define TYPE_SIG_I32        0x6");
        writeLine("#define TYPE_SIG_U32        0x7");
        writeLine("#define TYPE_SIG_I64        0x8");
        writeLine("#define TYPE_SIG_U64        0x9");
        writeLine("#define TYPE_SIG_F32        0xa");
        writeLine("#define TYPE_SIG_F64        0xb");
        writeLine("#define TYPE_SIG_STRING8    0xc");
        writeLine("#define TYPE_SIG_STRING16   0xd");
        writeLine("#define TYPE_SIG_STRING32   0xe");
        writeLine("#define TYPE_SIG_TYPE       0xf");
        writeLine("#define TYPE_SIG_NONE       0x10");
        writeLine("#define TYPE_SIG_ANY        0x11");
        writeLine("#define TYPE_SIG_OPTIONAL   0x12");//content[u32-off][u16-size]
        writeLine("#define TYPE_SIG_REFERENCE  0x13");//content[u32-off][u16-size]
        writeLine("#define TYPE_SIG_ARRAY      0x14");//content[u32-off][u16-size]
        writeLine("#define TYPE_SIG_PROC       0x15");//signature[u32-off][u16-size]
        writeLine("#define TYPE_SIG_STRUCT     0x16");//contents[u32-off][u16-size]
        writeLine("#define TYPE_CONTENT_SHIFT  8");
        writeLine("#define TYPE_CONTENT_MASK   0xffffffff");
        writeLine("#define TYPE_COUNT_SHIFT    40");
        writeLine("#define TYPE_COUNT_MASK     0xffff");
        comment("Type data for all contained Types");
        writeLine("Type typeData [];");
        typeDataDeclarations=new StringBuilder("Type typeData []={");
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
        comment("value-block definition");
        writeLine("union " + VALUE_BLOCK_NAME + "Impl{");
        writeLine("  bool       asBool;");
        writeLine("  int8_t     asI8;");//addLater use constants/function for type-names
        writeLine("  uint8_t    asU8;");
        writeLine("  int16_t    asI16;");
        writeLine("  uint16_t   asU16;");
        writeLine("  int32_t    asI32;");
        writeLine("  uint32_t   asU32;");
        writeLine("  int64_t    asI64;");
        writeLine("  uint64_t   asU64;");
        writeLine("  float      asF32;");//addLater ensure correct size
        writeLine("  double     asF64;");
        writeLine("  Type       asType;");
        writeLine("  " + PROCEDURE_TYPE + "  asProc;");
        writeLine("  " + VALUE_BLOCK_NAME + "*     asPtr;");//reference
        writeLine("  uint8_t    raw8[8];");
        writeLine("  uint16_t   raw16[4];");
        writeLine("  uint32_t   raw32[2];");
        writeLine("};");
        //multi-value types:
        //  string  ->  len,        val_ptr/raw_data
        //  array   ->  len,        val_ptr/raw_data
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
        writeLine("}LogType;");
        comment("previously used logType");
        writeLine("static LogType prevType = null;");
        comment("definition of NEW_LINE character");//addLater choose system line separator
        writeLine("#define NEW_LINE \"\\n\"");
        for(LogType.Type t:LogType.Type.values()){
            writeLine("FILE* log_"+t+";");
        }
        comment("sets log-streams to their initial value");
        writeLine("void initLogStreams(){");
        for(LogType.Type t:LogType.Type.values()){
            writeLine("  log_"+t+" = "+(t== LogType.Type.ERR?"stderr":"stdout")+";");
        }
        writeLine("}");
        comment("recursive printing of types");//addLater surround types with brackets when printing
        writeLine("void printType(const Type type,FILE* log){");
        writeLine("  switch(type&TYPE_SIG_MASK){");
        writeLine("    case TYPE_SIG_EMPTY:");
        writeLine("      fputs(\""+escapeStr(Type.EMPTY_TYPE)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_BOOL:");
        writeLine("      fputs(\""+escapeStr(Type.Primitive.BOOL)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_I8:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.INT8)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_U8:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.UINT8)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_I16:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.INT16)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_U16:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.UINT16)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_I32:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.INT32)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_U32:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.UINT32)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_I64:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.INT64)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_U64:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.UINT64)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_F32:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.FLOAT32)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_F64:");
        writeLine("      fputs(\""+escapeStr(Type.Numeric.FLOAT64)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_STRING8:");
        writeLine("      fputs(\""+escapeStr(Type.NoRetString.STRING8)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_STRING16:");
        writeLine("      fputs(\""+escapeStr(Type.NoRetString.STRING16)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_STRING32:");
        writeLine("      fputs(\""+escapeStr(Type.NoRetString.STRING32)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_TYPE:");
        writeLine("      fputs(\""+escapeStr(Type.TYPE)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_NONE:");
        writeLine("      fputs(\""+escapeStr(Type.NONE_TYPE)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_ANY:");
        writeLine("      fputs(\""+escapeStr(Type.Primitive.ANY)+"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_OPTIONAL:");
        writeLine("      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log);");
        writeLine("      fputs(\"?\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_REFERENCE:");
        writeLine("      fputs(\"@\",log);");
        writeLine("      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_ARRAY:");
        writeLine("      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log);");
        writeLine("      fputs(\"[]\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_PROC:");
        writeLine("    case TYPE_SIG_STRUCT:");
        writeLine("      assert(false && \" unreachable \");");
        writeLine("      break;");
        writeLine("  }");
        writeLine("}");
        comment("log-Method");
        writeLine("void logValue(LogType logType,bool append,const Type type,const "+VALUE_BLOCK_NAME+"* value){");
        writeLine("  if(prevType!=null){");
        writeLine("    if((logType!=prevType)||(!append)){");
        writeLine("      switch(prevType){");
        writeLine("        case null:");
        writeLine("          break;");
        for(LogType.Type t:LogType.Type.values()){
            writeLine("        case "+t+":");
            writeLine("          fputs(NEW_LINE,log_"+t+");");
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
            writeLine("      log=log_"+t+";");
            //TODO type specific prefixes
            writeLine("      break;");
        }
        writeLine("  }");
        writeLine("  switch(type&TYPE_SIG_MASK){");
        writeLine("    case TYPE_SIG_EMPTY:");
        writeLine("      fputs(\"unexpected Value-Type in log: \\\"EMPTY\\\"\",log_"+LogType.Type.ERR+");");
        writeLine("      exit(-1);");//addLater error code-handling
        writeLine("      break;");
        writeLine("    case TYPE_SIG_BOOL:");
        writeLine("      fputs(value->asBool?\"true\":\"false\",log);");
        writeLine("      break;");
        for(int n=8;n<100;n*=2){
            writeLine("    case TYPE_SIG_I"+n+":");
            writeLine("      fprintf(log,\"%\"PRIi"+n+",value->asI"+n+");");
            writeLine("      break;");
            writeLine("    case TYPE_SIG_U"+n+":");
            writeLine("      fprintf(log,\"%\"PRIu"+n+",value->asI"+n+");");
            writeLine("      break;");
        }
        writeLine("    case TYPE_SIG_F32:");
        writeLine("      fprintf(log,\"%f\",value->asF32);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_F64:");
        writeLine("      fprintf(log,\"%f\",value->asF64);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_NONE:");
        writeLine("      fputs(\"\\\"none\\\"\",log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_STRING8:");
        writeLine("    case TYPE_SIG_STRING16:");
        writeLine("    case TYPE_SIG_STRING32:");
        //TODO print strings
        writeLine("      assert(false && \"unimplemented\");");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_TYPE:");
        writeLine("      printType(value->asType,log);");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_ANY:");
        writeLine("       prevType=logType;");
        writeLine("       logValue(logType,true,value->asType,value+1/*content*/);");//TODO dereference content if pointer
        writeLine("       break;");
        writeLine("    case TYPE_SIG_OPTIONAL:");
        writeLine("      if(value[0].asBool){");
        writeLine("        prevType=logType;");
        writeLine("        fputs(\"Optional{\",log);");
        writeLine("        logValue(logType,true,typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],value+1/*value*/);");
        writeLine("        fputs(\"}\",log);");
        writeLine("      }else{");
        writeLine("        fputs(\"Optional{}\",log);");
        writeLine("      }");
        writeLine("      break;");
        writeLine("    case TYPE_SIG_REFERENCE:");
        writeLine("    case TYPE_SIG_ARRAY:");
        writeLine("    case TYPE_SIG_PROC:");
        writeLine("    case TYPE_SIG_STRUCT:");
        //TODO Print Containers
        writeLine("      assert(false && \" unimplemented \");");
        writeLine("      break;");
        writeLine("    default:");
        writeLine("      assert(false && \" unreachable \");");
        writeLine("      break;");
        writeLine("  }");
        writeLine("  prevType=logType;");
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

    private void writeNumber(StringBuilder out, Value v, DataOut dataOut, boolean incOff, boolean prefix) {
        Type.Numeric t=(Type.Numeric) v.getType();
        if(prefix){
            out.append(CAST_BLOCK);
        }
        if(t.isFloat){
            out.append("{.asF").append(8 * (1 << t.level)).append("=").append(((Value.Primitive) v).getValue()).append("}");
        }else{
            out.append("{.as").append(t.signed ? "I" : "U").append(8 * (1 << t.level)).append("=")
                    .append(((Value.Primitive) v).getValue()).append("}");
        }
        if(incOff){
            dataOut.off++;}
    }

    /**Writes a value as a array of union declarations*/
    private void writeConstValueAsUnion(StringBuilder out, Value v, DataOut dataOut, boolean isFirst, boolean inPlaceValues,
                                        boolean incOff,boolean prefix) throws IOException{
        if(!isFirst){
            out.append(',');
        }
        if(v.getType()== Type.Primitive.ANY){
            Type contentType = ((Value.AnyValue) v).content.getType();
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asType=").append(typeSignature(contentType)).append("}");
            if(incOff){
                dataOut.off++;}
            if(incOff){
                dataOut.off++;}//increment before store
            if(contentType.blockCount>1) {
                out.append(',');
                if (prefix) {
                    out.append(CAST_BLOCK);
                }
                out.append("{.asPtr=(").append(dataOut.name).append("+").append(dataOut.off).append(")}");
                writeConstValueAsUnion(dataOut.build, ((Value.AnyValue) v).content, dataOut, dataOut.off == 0, true, true, prefix);
                out.append(',');
            }else{
                writeConstValueAsUnion(out,((Value.AnyValue) v).content,dataOut,false,false,incOff,prefix);
            }
        }else if(v.getType() == Type.Primitive.BOOL){
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asBool=").append(((Value.Primitive) v).getValue()).append("}");
            if(incOff){
                dataOut.off++;}
        }else if(v.getType() == Type.TYPE){
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asType=").append(typeSignature(((Value.TypeValue)v).value)).append("}");
            if(incOff){
                dataOut.off++;}
        }else if(v.getType() instanceof Type.Numeric){
            writeNumber(out, v, dataOut, incOff,prefix);
        }else if(v.getType()== Type.NoRetString.STRING8){
            byte[] bytes=((Value.StringValue) v).utf8Bytes();
            if(inPlaceValues){
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|bytes.length)).append("}");
                if(incOff){
                    dataOut.off++;}
                out.append(',');
            }else{
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(dataOut.mask|bytes.length)).append("}");
                if(incOff){
                    dataOut.off++;}
                if(incOff){
                    dataOut.off++;}//increment before store
                out.append(',');
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asPtr=(").append(dataOut.name).append("+").append(dataOut.off).append(")}");
                out= dataOut.build;
                incOff=true;
                if(dataOut.off>0){
                    out.append(',');
                }
            }
            for(int i=0;i< bytes.length;i+=8){
                if(i>0){out.append(','); }
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.raw8={");
                for(int j=0;j<8;j++){
                    if(j>0){
                        out.append(',');
                    }
                    out.append("0x").append((i + j < bytes.length) ? Integer.toHexString(bytes[i + j] & 0xff) : "0");
                }
                out.append("}}");
                if(incOff){ dataOut.off++;}
            }
        }else if(v.getType()== Type.NoRetString.STRING16){
            char[] chars=((Value.StringValue) v).chars();
            if(inPlaceValues){
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|chars.length)).append("}");
                if(incOff){
                    dataOut.off++;}
                out.append(',');
            }else{
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(dataOut.mask|chars.length)).append("}");
                if(incOff){
                    dataOut.off++;}
                if(incOff){
                    dataOut.off++;}//increment before store
                out.append(',');
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asPtr=(").append(dataOut.name).append("+").append(dataOut.off).append(")}");
                out= dataOut.build;
                incOff=true;
                if(dataOut.off>0){
                    out.append(',');
                }
            }
            for(int i=0;i< chars.length;i+=4){
                if(i>0){out.append(','); }
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.raw16={");
                for(int j=0;j<4;j++){
                    if(j>0){
                        out.append(',');
                    }
                    out.append("0x").append((i + j < chars.length) ? Integer.toHexString(chars[i + j] & 0xffff) : "0");
                }
                out.append("}}");
                if(incOff){
                    dataOut.off++;}
            }
        }else if(v.getType()== Type.NoRetString.STRING32){
            int[] codePoints=((Value.StringValue) v).codePoints();
            if(inPlaceValues){
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|codePoints.length)).append("}");
                if(incOff){
                    dataOut.off++;}
                out.append(',');
            }else{
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(dataOut.mask|codePoints.length)).append("}");
                if(incOff){
                    dataOut.off++;}
                if(incOff){
                    dataOut.off++;}//increment before store
                out.append(',');
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asPtr=(").append(dataOut.name).append("+").append(dataOut.off).append(")}");
                out= dataOut.build;
                incOff=true;
                if(dataOut.off>0){
                    out.append(',');
                }
            }
            for(int i=0;i< codePoints.length;i+=2){
                if(i>0){out.append(','); }
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.raw32={");
                for(int j=0;j<2;j++){
                    if(j>0){
                        out.append(',');
                    }
                    out.append("0x").append((i + j < codePoints.length) ? Integer.toHexString(codePoints[i + j]) : "0");
                }
                out.append("}}");
                if(incOff){
                    dataOut.off++;}
            }
        }else if(v instanceof Value.Array){
            if(inPlaceValues) {
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|((Value.Array) v).elements().length)).append("}");
                if(incOff){
                    dataOut.off++;}
                //ensure constant block-size (1 for bool,float[N],[u]int[N],reference  2 for string, array, any, optional)
                for (Value elt : ((Value.Array) v).elements()) {
                    writeConstValueAsUnion(out, elt, dataOut, false, false, incOff,prefix);
                }
            }else{
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asU64=0x").append(Long.toHexString(dataOut.mask|((Value.Array) v).elements().length)).append("}");
                if(incOff){
                    dataOut.off++;}
                if(incOff){
                    dataOut.off++;}//increment before write
                out.append(',');
                if(prefix){out.append(CAST_BLOCK); }
                out.append("{.asPtr=(").append(dataOut.name).append("+").append(dataOut.off).append(")}");
                for (Value elt : ((Value.Array) v).elements()) {
                    writeConstValueAsUnion(dataOut.build,elt, dataOut, dataOut.off==0, true, true,prefix);
                }
            }
        }else if(v instanceof Value.Struct){
            throw new UnsupportedEncodingException("structs are currently not supported");
            //write fields one by one
            //write fields preceded with types if in any
        }else if(v == Value.NONE){
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asBool=false/*none*/}");
        }else{
            throw new UnsupportedEncodingException(v.getType()+" is currently not supported in the compiler");
        }
    }

    //TODO allow links to other constants in constants
    private void writeConstant(String name, Value value, DataOut constData) throws IOException {
        comment("const "+value.getType()+" : "+name+" = "+value.stringRepresentation());
        out.write("const " + VALUE_BLOCK_NAME + " " + CONST_PREFIX +asciify(name));
        out.write(" []={");
        StringBuilder tmp=new StringBuilder();
        writeConstValueAsUnion(tmp, value, constData, true, true, false,false);
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
            if (blockSize == 1) {
                line.append(indent).append("  ").append(argsOut).append("[").append(argOff).append("]=");
                writeExpression("  ", initLines, line, firstArg, 0, name, argNames);
                line.append(";");
            } else {
                line.append(indent).append("  memcpy,").append(argsOut).append("[").append(argOff).append("],");
                writeExpression("  ", initLines, line, firstArg, 0, name, argNames);
                line.append(",").append(blockSize).append(");");
            }
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
            if(argTypes[i].blockCount==1){
                argNames[i]="(*(argsIn+"+(off++)+"))";
            }else{
                argNames[i]= "(argsIn+"+(off)+")";
                off+=argTypes[i].blockCount;
            }
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
                    if(blockCount ==1){
                        comment("  " + VALUE_BLOCK_NAME + " var" +valCount+";",
                                "("+((ValDef) a).getType()+")");
                        line.append("    var").append(valCount).append("=");
                    }else{
                        comment("  " + VALUE_BLOCK_NAME + " var" +valCount+" ["+ blockCount +"];",
                                "("+((ValDef) a).getType()+")");
                        line.append("    memcpy(var").append(valCount).append(",");
                    }
                    comment("  {","Initialize: "+((ValDef) a).getInitValue());
                    writeExpression("    ",initLines,line,((ValDef) a).getInitValue(),0, name, argNames);
                    for(String l:initLines){
                        writeLine(l);
                    }
                    initLines.clear();
                    if(blockCount==1){
                        writeLine(line.append(';').toString());
                    }else{
                        writeLine(line.append(",").append(blockCount).append(");").toString());
                    }
                    writeLine("  }");
                    valCount++;
                }else if(a instanceof Assignment){
                    comment("  {","Assign: "+a);
                    line.setLength(0);
                    line.append("    ");
                    //prepare target TODO writeMutableExpression mode, that stores all expressions as pointer to their location
                    writeExpression("    ",initLines,line,((Assignment) a).target,0, name, argNames);
                    line.append(" = ");
                    //preform assignment
                    writeExpression("    ",initLines,line,((Assignment) a).expr,0, name, argNames);
                    for(String l:initLines){
                        writeLine(l);
                    }
                    initLines.clear();
                    writeLine(line.append(';').toString());
                    writeLine("  }");
                }else if(a instanceof LogAction){
                    comment("  {","Log: "+a);
                    line.setLength(0);
                    line.append("    logValue(").append(((LogAction) a).type.type).append(",")
                            .append(((LogAction) a).type.append).append(',')
                            .append(typeSignature(((LogAction) a).expr.expectedType()))
                            .append(((LogAction) a).expr.expectedType().blockCount==1?",&":",");
                    writeExpression("    ",initLines,line,((LogAction) a).expr,0, name, argNames);
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
                //TODO allow multiple children procedure-calls (pthreads)
                //writeAllOtherCalls
                for(int i=0;i<children.size();i++){
                    Procedure.ProcChild c=children.get(i);
                    comment("  ","Call:"+c);
                    if(c instanceof Procedure.StaticProcChild||
                            (c instanceof Procedure.DynamicProcChild&&!((Procedure.DynamicProcChild) c).isOptional)){
                        writeLine("  {");
                        writeLine("    "+VALUE_BLOCK_NAME+"* newArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");//TODO check pointer
                        writeArgs("    ",childArgs.get(i),initLines,line,name,argNames,"newArgs");
                        //addLater call function (via pthreads)
                        writeLine("    assert(false && \"unimplemented\");");
                        writeLine("  }");
                        //static procedure
                        break;
                    }else{
                        writeLine("  if("+argNames[((Procedure.DynamicProcChild)children.get(i)).varId]+"[0].asBool){");
                        writeLine("    "+VALUE_BLOCK_NAME+"* newArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");//TODO check pointer
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
                    writeLine("  return "+argNames[((Procedure.DynamicProcChild)firstStatic).varId]+".asProc");
                }
            }else{
                //all children are optional
                writeLine("  "+PROCEDURE_TYPE+" ret=NULL;");
                for(int i=0;i<children.size();i++){
                    comment("  ","Call:"+children.get(i));
                    writeLine("  if("+argNames[((Procedure.DynamicProcChild)children.get(i)).varId]+"[0].asBool){");
                    writeLine("    if(ret==NULL){");
                    writeArgs("      ",childArgs.get(i), initLines, line, name, argNames, "argsOut");
                    writeLine("      ret="+argNames[((Procedure.DynamicProcChild)children.get(i)).varId]+"[1].asProc;");
                    writeLine("    }else{");
                    writeLine("    "+VALUE_BLOCK_NAME+"* newArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");//TODO check pointer
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
            writeLine("  return NULL;");
        }
        writeLine("}");
        out.newLine();
    }

    private String typeField(Type.Numeric type){
        if(type.isFloat){
            return ".asF"+8 * (1 << type.level);
        }else{
            return ".as"+(type.signed?"I":"U")+8 * (1 << type.level);
        }
    }
    private String cTypeName(Type.Numeric type){
        if(type.isFloat){
            if(type.level<=2){
                return "float";
            }else if(type.level==3){
                return "double";
            }else{
                throw new SyntaxError("float out of range:"+type);
            }
        }else{
            return (type.signed?"int":"uint")+8 * (1 << type.level)+"_t";
        }
    }

    private String[] unOpParts(LeftUnaryOp op) {
        String[] parts=new String[2];
        Type inType=op.expr.expectedType();
        Type outType=op.expectedType();
        switch (op.op){
            case PLUS:
                if(inType instanceof Type.Numeric){
                    parts[0]=parts[1]="";
                    return parts;
                }else{
                    throw new SyntaxError("unsupported type for +:"+inType);
                }
            case MINUS:
                if(inType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=-("+ cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)inType)+"}";
                    return parts;
                }else{
                    throw new SyntaxError("unsupported type for -:"+inType);
                }
            case FLIP:
                if(inType instanceof Type.Numeric&&!((Type.Numeric)outType).isFloat){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=~("+ cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)inType)+"}";
                    return parts;
                }else{
                    throw new SyntaxError("unsupported type for ~:"+inType);
                }
            case NOT:
                if(inType == Type.Primitive.BOOL){
                    parts[0]=CAST_BLOCK+"{.asBool=!";
                    parts[1]=".asBool}";
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

    private String[] binOpParts(BinOp op) {
        String[] parts=new String[3];
        Type lType=op.left.expectedType();
        Type rType=op.right.expectedType();
        Type outType=op.expectedType();
        switch (op.op){
            case PLUS:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")+(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case MINUS:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")-(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case MULT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")*(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case DIV:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")/(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case MOD:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")%(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case INT_DIV:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    if((((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                        throw new UnsupportedOperationException("unimplemented");
                    }else{
                        parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                        parts[1]=typeField((Type.Numeric)lType)+")/(("+cTypeName((Type.Numeric)outType)+")";
                        parts[2]=typeField((Type.Numeric)rType)+")}";
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
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")&(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case OR:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")|(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case XOR:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")^(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case LSHIFT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")<<(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case RSHIFT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric&&
                        !(((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat)){
                    parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)outType)+"=(("+cTypeName((Type.Numeric)outType)+")";
                    parts[1]=typeField((Type.Numeric)lType)+")>>(("+cTypeName((Type.Numeric)outType)+")";
                    parts[2]=typeField((Type.Numeric)rType)+")}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case FAST_AND://addLater own handling of fastAnd if(cond1){res=cond2}else{res=false}
            case FAST_OR://addLater own handling of fastOr if(cond1){res=true}else{res=cond2}
            case NE:
            case EQ:
                throw new UnsupportedOperationException("unimplemented:"+op.op);
            case GT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{.asBool=";
                    parts[1]=typeField((Type.Numeric)lType)+">";
                    parts[2]=typeField((Type.Numeric)rType)+"}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case GE:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{.asBool=";
                    parts[1]=typeField((Type.Numeric)lType)+">=";
                    parts[2]=typeField((Type.Numeric)rType)+"}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case LT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{.asBool=";
                    parts[1]=typeField((Type.Numeric)lType)+"<";
                    parts[2]=typeField((Type.Numeric)rType)+"}";
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case LE:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{.asBool=";
                    parts[1]=typeField((Type.Numeric)lType)+"<=";
                    parts[2]=typeField((Type.Numeric)rType)+"}";
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
    private String[] typecastParts(Type to, Type from) {
        String[] parts=new String[2];
        if(from instanceof Type.Numeric&&to instanceof Type.Numeric){
            parts[0]=CAST_BLOCK+"{"+typeField((Type.Numeric)to)+"=("+cTypeName((Type.Numeric)to)+")(";
            parts[1]=")"+typeField((Type.Numeric)from)+"}";
            return parts;
        }else if(to instanceof Type.Optional&&from.blockCount==1){
            parts[0]="("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{.asBool="+(from!=Type.NONE_TYPE)+"},";
            parts[1]="}";
            return parts;
        }else if(to == Type.Primitive.ANY&&from.blockCount==1){
            parts[0]="("+VALUE_BLOCK_NAME+"[]){"+CAST_BLOCK+"{.asType="+typeSignature(from)+"},";
            parts[1]="}";
            return parts;
        }else if(to== Type.Primitive.BOOL &&from instanceof Type.Optional){
            parts[0]="(";
            parts[1]=")[0]";
            return parts;
        }else{
            throw new UnsupportedOperationException("Cast from "+from+" to "+to+" is currently not implemented");
        }
    }

    private int writeExpression(String indent, ArrayList<String> initLines, StringBuilder line, Expression expr, int tmpCount, String procName, String[] varNames) throws IOException {
        if(expr instanceof VarExpression){
            line.append(varNames[((VarExpression)expr).varId]);
            return tmpCount;
        }else if(expr instanceof ThisExpr){
            line.append(CAST_BLOCK+"{.asProc=&" + PROC_PREFIX).append(procName).append("}");
            return tmpCount;
        }else if(expr instanceof ValueExpression){
            if(((ValueExpression) expr).constId!=null){
                line.append(CONST_PREFIX).append(asciify(((ValueExpression) expr).constId));
                return tmpCount;
            }else if(expr.expectedType().varSize){
                int blockCount=expr.expectedType().blockCount;
                DataOut data=new DataOut("data={",0,"tmp",LEN_MASK_TMP);//TODO handle data
                StringBuilder tmp;
                if(blockCount>1){
                    initLines.add(indent+"Value tmp"+tmpCount+" ["+blockCount+"];");
                    tmp=new StringBuilder("  memcpy(tmp"+tmpCount+",("+VALUE_BLOCK_NAME+"[]){");
                }else{
                    initLines.add(indent+"Value tmp"+tmpCount+";");
                    tmp=new StringBuilder("  tmp"+tmpCount+"=");
                }
                writeConstValueAsUnion(tmp,((ValueExpression)expr).getValue(),data,true,false,false,true);
                if(blockCount>1){
                    tmp.append("},").append(blockCount).append(");");
                }else{
                    tmp.append(";");
                }
                initLines.add(indent+"{");
                initLines.add(indent+tmp);
                initLines.add(indent+"  // TODO handle data");
                initLines.add(indent+"  // "+data.build.append("}"));
                initLines.add(indent+"}");
                line.append("tmp").append(tmpCount);
                return tmpCount+1;
            }else{
                boolean multiBlock=expr.expectedType().blockCount>1;
                line.append(multiBlock?'{':'(');
                writeConstValueAsUnion(line,((ValueExpression)expr).getValue(),null,true,false,false,true);
                line.append(multiBlock?'}':')');
                return tmpCount;
            }
        }else if(expr instanceof BinOp){
            String[] parts=binOpParts(((BinOp) expr));
            line.append(parts[0]);
            tmpCount=writeExpression(indent,initLines,line,((BinOp)expr).left,tmpCount, procName, varNames);
            line.append(parts[1]);
            tmpCount=writeExpression(indent,initLines,line,((BinOp)expr).right,tmpCount, procName, varNames);
            line.append(parts[2]);
            return tmpCount;
        }else if(expr instanceof LeftUnaryOp){
            String[] parts=unOpParts(((LeftUnaryOp) expr));
            line.append(parts[0]);
            tmpCount=writeExpression(indent,initLines,line,((LeftUnaryOp)expr).expr,tmpCount, procName, varNames);
            line.append(parts[1]);
            return tmpCount;
        }else if(expr instanceof TypeCast){
            String[] parts=typecastParts(((TypeCast) expr).type,((TypeCast) expr).value.expectedType());
            line.append(parts[0]);
            tmpCount=writeExpression(indent,initLines,line,((TypeCast)expr).value,tmpCount, procName, varNames);
            line.append(parts[1]);
            return tmpCount;
        }else if(expr instanceof IfExpr){
            //addLater use ?: if both arguments can be inlined
            int blockCount = expr.expectedType().blockCount;
            initLines.add(indent+"Value tmp"+tmpCount+(blockCount!=1?" ["+blockCount+"];":";"));
            int prevTmp=tmpCount++;
            initLines.add(indent+"{");
            StringBuilder tmp=new StringBuilder(indent+"  if(");
            tmpCount=writeExpression(indent+"    ",initLines,tmp,((IfExpr)expr).cond,tmpCount, procName, varNames);
            initLines.add(tmp.append(".asBool){").toString());
            if(blockCount==1) {
                tmp = new StringBuilder(indent + "    tmp" + prevTmp + "=");
            }else{
                tmp = new StringBuilder(indent + "    memcpy(tmp" + prevTmp + ",");
            }
            tmpCount=writeExpression(indent,initLines,tmp,((IfExpr)expr).ifVal,tmpCount, procName, varNames);
            if(blockCount!=1) {
                tmp.append(",").append(blockCount).append(')');
            }
            initLines.add(tmp.append(";").toString());
            initLines.add(indent+"  }else{");
            if(blockCount==1) {
                tmp = new StringBuilder(indent + "    tmp" + prevTmp + "=");
            }else{
                tmp = new StringBuilder(indent + "    memcpy(tmp" + prevTmp + ",");
            }
            writeExpression(indent+"    ",initLines,tmp,((IfExpr)expr).elseVal,tmpCount, procName, varNames);
            if(blockCount!=1) {
                tmp.append(",").append(blockCount).append(')');
            }
            initLines.add(tmp.append(";").toString());
            initLines.add(indent+"  }");
            initLines.add(indent+"}");
            line.append("tmp").append(prevTmp);
            return prevTmp;
        }else if(expr instanceof GetField){
            if(((GetField) expr).fieldName.equals(Type.FIELD_NAME_TYPE)){
                if(((GetField) expr).value.expectedType()== Type.Primitive.ANY){
                    //TODO read type from any
                    throw new UnsupportedOperationException("\"any.type\" is currently not supported");
                }else{
                    line.append(CAST_BLOCK + "{.asType=").append(typeSignature(((GetField) expr).value.expectedType())).append('}');
                    return tmpCount;
                }
            }else if(((GetField) expr).value.expectedType() instanceof Type.Array&&
                    ((GetField) expr).fieldName.equals(Type.FIELD_NAME_LENGTH)){
                line.append('(');
                writeExpression(indent,initLines,line, ((GetField) expr).value, tmpCount,procName,varNames);
                line.append(")[0]");//array.length
                return tmpCount;
            }else if(((GetField) expr).value.expectedType() instanceof Type.Optional&&
                    ((GetField) expr).fieldName.equals(Type.FIELD_NAME_VALUE)){
                //TODO check value before access, dereference pointers
                line.append('(');
                writeExpression(indent,initLines,line, ((GetField) expr).value, tmpCount,procName,varNames);
                line.append(")[1]");//optional.length
                return tmpCount;
            }else{
                throw new UnsupportedOperationException(expr.getClass().getSimpleName()+" is currently not supported");
            }
            //return tmpCount;
        }else {
            //TODO other expressions
            throw new UnsupportedOperationException(expr.getClass().getSimpleName()+" is currently not supported");
            //return tmpCount;
        }
    }


    private void writeRunSignature() throws IOException {
        comment(" main procedure handling function (written in a way that allows easy usage in pthreads)");
        out.write("void* run(void* initState)");
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
        writeLine("    " + VALUE_BLOCK_NAME + "* argData=*(("+VALUE_BLOCK_NAME+"**)initState);");
        writeLine("    initState+=sizeof(" + VALUE_BLOCK_NAME + "*);");
        writeLine("    " + VALUE_BLOCK_NAME + "* argsO=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
        writeLine("    " + VALUE_BLOCK_NAME + "* argsTmp;");
        writeLine("    if(argsO==NULL){");
        writeLine("        return (void*)-1;");//addLater useful handling of return codes
        writeLine("    }");
        writeLine("    // initArgs");
        writeLine("    memcpy(argsI,initState,argCount*sizeof(" + VALUE_BLOCK_NAME + "));");
        writeLine("    do{");
        writeLine("        f=(" + PROCEDURE_TYPE + ")f(argsI,argsO,&argData);");
        comment("        ","swap args");
        writeLine("        argsTmp=argsI;");
        writeLine("        argsI=argsO;");
        writeLine("        argsO=argsTmp;");
        writeLine("    }while(f!=NULL);");
        writeLine("    return (void*)0;");
        writeLine("}");
        out.newLine();
        comment("main method of the C representation: ");
        comment("  transforms the input arguments and starts the run function on this thread");
        if(hasArgs){
            writeLine("int main(int argc,char** argv){");
            //ignore first argument for consistency with interpreter
        }else{
            writeLine("int main(){");
        }
        comment("  ","[proc_ptr,args_ptr,arg_data]");
        writeLine("  void* init=malloc(sizeof(" + PROCEDURE_TYPE + ")+2*sizeof("+VALUE_BLOCK_NAME+"*));");//TODO replace malloc with local array
        writeLine("  size_t off=0;");
        writeLine("  *((" + PROCEDURE_TYPE + "*)init)=&" + PROC_PREFIX + "start;");
        writeLine("  off+=sizeof(" + PROCEDURE_TYPE + ");");
        writeLine("  " + VALUE_BLOCK_NAME + "* initArgs=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
        writeLine("  if(initArgs==NULL){");
        writeLine("    return -1;");//addLater useful handling of return codes
        writeLine("  }");
        writeLine("  *((" + VALUE_BLOCK_NAME + "**)(init+off))=initArgs;");
        writeLine("  off+=sizeof(" + VALUE_BLOCK_NAME + "*);");
        writeLine("  " + VALUE_BLOCK_NAME + "* argData=malloc(ARG_DATA_INIT_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
        writeLine("  if(argData==NULL){");
        writeLine("    return -1;");//addLater useful handling of return codes
        writeLine("  }");
        writeLine("  *((" + VALUE_BLOCK_NAME + "**)(init+off))=argData;");
        writeLine("  off+=sizeof(" + VALUE_BLOCK_NAME + "*);");
        if(hasArgs){
            comment("  ","prepare program Arguments");
            //this code only works if argv iss encoded with UTF-8
            comment("  ","!!! currently only UTF-8 encoding is supported !!!");//addLater support for other encodings of argv
            writeLine("  assert(false && \"unimplemented\");");
            /* TODO store argument data in argsData
            writeLine("  int l;");
            writeLine("  int k0=0;");
            writeLine("  for(int i=1;i<argc;i++){");//skip first argument
            writeLine("    int l=strlen(argv[i]);");
            writeLine("    *((" + VALUE_BLOCK_NAME + "*)(init+off))="+CAST_BLOCK+"{.asU64=LEN_MASK_LOCAL|l};");//store lengths of arguments
            writeLine("    off+=sizeof(" + VALUE_BLOCK_NAME + ");");
            writeLine("    *((" + VALUE_BLOCK_NAME + "*)(init+off))="+CAST_BLOCK+"{.asPtr=argData+k0};");//store pointer to data
            writeLine("    off+=sizeof(" + VALUE_BLOCK_NAME + ");");
            writeLine("    for(int j=0,k=0;j+k<l;j++){");
            writeLine("      if(j==8){");
            writeLine("        j=0;");
            writeLine("        k++;");
            writeLine("        k0++;");
            writeLine("      }");
            writeLine("      argData[k0].raw8[j]=argv[i][j+k];");
            writeLine("    }");
            writeLine("    k0++;");
            writeLine("  }");*/
        }
        writeLine("  initLogStreams();");
        writeLine("  run(init);");
        comment("  puts(\"\");","finish last line in stdout");
        writeLine("  return 0;");
        writeLine("}");
    }

    public void compile(Parser.ParserContext context) throws IOException {
        writeFileHeader(context.maxArgSize());
        DataOut constData=new DataOut(CONST_DATA_SIGNATURE + "={",0,CONST_DATA_NAME,LEN_MASK_CONST);
        writeLine(CONST_DATA_SIGNATURE+";");
        for(Map.Entry<String, Value> e:context.constants.entrySet()){
            writeConstant(e.getKey(),e.getValue(),constData);
        }
        comment("data for values used in constants");
        writeLine(constData.build.append("};").toString());
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
        comment("declarations of all used type Signatures");
        writeLine(typeDataDeclarations.append("};").toString());//declare type signatures after all NoRet code-sections are compiled
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
