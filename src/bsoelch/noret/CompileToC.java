package bsoelch.noret;

import bsoelch.noret.lang.*;
import bsoelch.noret.lang.expression.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class CompileToC {
    public static final String VALUE_BLOCK_NAME = "Value";
    public static final String CAST_BLOCK = "("+VALUE_BLOCK_NAME+")";

    private static final String[] procedureArgs     ={VALUE_BLOCK_NAME + "*", "size_t*", VALUE_BLOCK_NAME + "**"};
    private static final String[] procedureArgNames ={"args",   "argCount", "argData"};
    private static final String procedureOut="void*";

    private static final String CONST_DATA_NAME = "constData";
    private static final String CONST_DATA_SIGNATURE = VALUE_BLOCK_NAME + " " +CONST_DATA_NAME+" []";
    private static final int MAX_ARG_SIZE = 0x2000;

    private static final long LEN_MASK_IN_PLACE = 0x0000000000000000L;
    private static final long LEN_MASK_CONST    = 0x8000000000000000L;
    private static final long LEN_MASK_LOCAL    = 0x4000000000000000L;
    private static final long LEN_MASK_TMP      = 0xC000000000000000L;
    public static final String PROC_PREFIX = "proc_";
    public static final String CONST_PREFIX = "const_";

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

    private void writeFileHeader() throws IOException {
        comment("Auto generated code from NoRet compiler");
        //addLater print information about compiled code
        out.newLine();
        //includes
        include("stdlib.h");
        include("stdbool.h");
        include("string.h");
        include("stdio.h");
        include("inttypes.h");
        out.newLine();
        writeLine("#define MAX_ARG_SIZE 0x"+Integer.toHexString(MAX_ARG_SIZE));
        out.newLine();
        writeLine("#define LEN_MASK_IN_PLACE 0x"+Long.toHexString(LEN_MASK_IN_PLACE));
        writeLine("#define LEN_MASK_CONST    0x"+Long.toHexString(LEN_MASK_CONST));
        writeLine("#define LEN_MASK_LOCAL    0x"+Long.toHexString(LEN_MASK_LOCAL));
        writeLine("#define LEN_MASK_TMP      0x"+Long.toHexString(LEN_MASK_TMP));
        out.newLine();
        //Type enum
        writeLine("typedef enum{");//TODO better handling of types
        writeLine("  EMPTY=0,");
        writeLine("  BOOL,");
        writeLine("  I8,");
        writeLine("  U8,");
        writeLine("  I16,");
        writeLine("  U16,");
        writeLine("  I32,");
        writeLine("  U32,");
        writeLine("  I64,");
        writeLine("  U64,");
        writeLine("  F32,");
        writeLine("  F64,");
        writeLine("  STRING,");
        writeLine("  TYPE,");
        writeLine("  ANY=0xf,");
        writeLine("}Type;");
        out.newLine();
        //Value struct
        writeLine("typedef union " + VALUE_BLOCK_NAME + "Impl "+VALUE_BLOCK_NAME+";");
        writeLine("union " + VALUE_BLOCK_NAME + "Impl{");
        writeLine("  bool     asBool;");
        writeLine("  int8_t   asI8;");
        writeLine("  uint8_t  asU8;");
        writeLine("  int16_t  asI16;");
        writeLine("  uint16_t asU16;");
        writeLine("  int32_t  asI32;");
        writeLine("  uint32_t asU32;");
        writeLine("  int64_t  asI64;");
        writeLine("  uint64_t asU64;");
        writeLine("  float    asF32;");//TODO ensure correct size
        writeLine("  double   asF64;");
        writeLine("  Type     asType;");
        writeLine("  " + VALUE_BLOCK_NAME + "*   asPtr;");//reference
        //multi-value types:
        //  string  ->  len,        val_ptr/raw_data
        //  array   ->  len,        val_ptr/raw_data
        //  any     ->  typeID,     val_ptr/raw_data
        //  opt     ->  hasData,    data
        writeLine("  uint8_t  raw8[8];");
        writeLine("  uint16_t raw16[4];");
        writeLine("  uint32_t raw32[2];");
        writeLine("};");
        out.newLine();
        //Procedure Type
        out.write("typedef "+procedureOut+"(*Procedure)(");
        for(int i=0;i<procedureArgs.length;i++){
            out.write((i>0?",":"")+procedureArgs[i]);
        }
        writeLine(");");
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

    private void writeNumber(StringBuilder out, Value v, DataOut dataOut, boolean incOff) {
        Type.Numeric t=(Type.Numeric) v.getType();
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
            //TODO store Types
            Type contentType = ((Value.AnyValue) v).content.getType();
            if(prefix){out.append(CAST_BLOCK); }
            out.append("{.asType=0/*").append(contentType).append("*/}");
            if(incOff){
                dataOut.off++;}
            if(incOff){
                dataOut.off++;}//increment before store
            if(contentType.varSize) {//addLater different handling for optional/reference
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
        }else if(v.getType() instanceof Type.Numeric){
            writeNumber(out, v, dataOut, incOff);
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
            //TODO detect if in any (then the element-types are necessary)
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

    private void writeProcImplementation(String name, Procedure proc) throws IOException{
        writeProcSignature(name, proc);
        writeLine("{");
        Type[] argTypes=proc.argTypes();
        String[] argNames=new String[proc.maxValues];//names of the arg-variables
        long off=0;
        int valCount=0;
        for(int i=0;i<argTypes.length;i++){
            if(argTypes[i].blockCount==1){
                argNames[i]="(*(args+"+(off++)+"))";
            }else{
                argNames[i]= "(*((" + VALUE_BLOCK_NAME + "[" +argTypes[i].blockCount+"])(args+"+(off)+")))";
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
                    if(((ValDef)a).getType().blockCount==1){
                        comment("  " + VALUE_BLOCK_NAME + " var" +valCount+";",
                                "("+((ValDef) a).getType()+")");
                    }else{
                        comment("  " + VALUE_BLOCK_NAME + " var" +valCount+" ["+((ValDef)a).getType().blockCount+"];",
                                "("+((ValDef) a).getType()+")");
                    }
                    comment("  {","Initialize: "+((ValDef) a).getInitValue());
                    line.setLength(0);
                    line.append("    var").append(valCount).append("=");
                    writeExpression("    ",initLines,line,((ValDef) a).getInitValue(),0, name, argNames);
                    for(String l:initLines){
                        writeLine(l);
                    }
                    initLines.clear();
                    writeLine(line.append(';').toString());
                    writeLine("  }");
                    valCount++;
                }else if(a instanceof Assignment){
                    comment("  {","Assign: "+a);
                    //TODO prepare target
                    //TODO preform assignment
                    writeLine("  }");
                }else if(a instanceof LogAction){
                    comment("  {","Log: "+a);
                    int blocks=((LogAction) a).expr.expectedType().blockCount;
                    line.setLength(0);
                    line.append("    log_").append(((LogAction) a).type).append("(");//TODO log Method(s)
                    writeExpression("    ",initLines,line,((LogAction) a).expr,0, name, argNames);
                    for(String l:initLines){
                        writeLine(l);
                    }
                    initLines.clear();
                    writeLine(line.append(");").toString());
                    writeLine("  }");
                }else{
                    comment("  ",a.toString());
                }
                //TODO compile action
            }
        }else{
            comment("  ","Native");
            //TODO implementations of native procedures
        }
        //TODO return id of next procedure
        writeLine("  return NULL;");
        writeLine("}");
        out.newLine();
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
                    throw new UnsupportedOperationException("unimplemented");
                }
            case MINUS:
                if(inType instanceof Type.Numeric){
                    if(((Type.Numeric)outType).isFloat){
                        parts[0]=CAST_BLOCK+"{.asF"+8 * (1 << ((Type.Numeric)outType).level)+"=-";
                    }else{
                        parts[0]=CAST_BLOCK+"{.as"+(((Type.Numeric)outType).signed?"I":"U")+8 * (1 << ((Type.Numeric)outType).level)+"=-";
                    }
                    if(((Type.Numeric)inType).isFloat){
                        parts[1]=".asF"+8 * (1 << ((Type.Numeric)inType).level)+"}";
                    }else{
                        parts[1]=".as"+(((Type.Numeric)inType).signed?"I":"U")+8 * (1 << ((Type.Numeric)outType).level)+"}";
                    }
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case DIV:
            case INT_DIV:
            case MULT:
            case MOD:
            case POW:
            case AND:
            case OR:
            case XOR:
            case NOT:
            case FAST_AND:
            case FAST_OR:
            case FLIP:
            case GT:
            case GE:
            case NE:
            case EQ:
            case LT:
            case LE:
            case IF:
            case LSHIFT:
            case RSHIFT:
                throw new UnsupportedOperationException("unimplemented");
        }
        throw new RuntimeException("Unreachable");
    }

    private String[] binOpParts(BinOp op) {
        String[] parts=new String[3];
        Type lType=op.left.expectedType();
        Type rType=op.right.expectedType();
        Type outType=op.expectedType();
        switch (op.op){
            case PLUS://TODO cast arguments
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    if(((Type.Numeric)outType).isFloat){
                        parts[0]=CAST_BLOCK+"{.asF"+8 * (1 << ((Type.Numeric)outType).level)+"=";
                    }else{
                        parts[0]=CAST_BLOCK+"{.as"+(((Type.Numeric)outType).signed?"I":"U")+8 * (1 << ((Type.Numeric)outType).level)+"=";
                    }
                    if(((Type.Numeric)lType).isFloat){
                        parts[1]=".asF"+8 * (1 << ((Type.Numeric)lType).level)+"+";
                    }else{
                        parts[1]=".as"+(((Type.Numeric)lType).signed?"I":"U")+8 * (1 << ((Type.Numeric)lType).level)+"+";
                    }
                    if(((Type.Numeric)rType).isFloat){
                        parts[2]=".asF"+8 * (1 << ((Type.Numeric)rType).level)+"+";
                    }else{
                        parts[2]=".as"+(((Type.Numeric)rType).signed?"I":"U")+8 * (1 << ((Type.Numeric)rType).level)+"}";
                    }
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case DIV:
            case INT_DIV:
            case MINUS:
            case MULT:
            case MOD:
            case POW:
            case AND:
            case OR:
            case XOR:
            case NOT:
            case FAST_AND:
            case FAST_OR:
            case FLIP:
            case GT:
            case GE:
            case NE:
            case EQ:
            case LT:
                if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
                    parts[0]=CAST_BLOCK+"{.asBool=";
                    if(((Type.Numeric)lType).isFloat){
                        parts[1]=".asF"+8 * (1 << ((Type.Numeric)lType).level)+"<";
                    }else{
                        parts[1]=".as"+(((Type.Numeric)lType).signed?"I":"U")+8 * (1 << ((Type.Numeric)lType).level)+"<";
                    }
                    if(((Type.Numeric)rType).isFloat){
                        parts[2]=".asF"+8 * (1 << ((Type.Numeric)rType).level)+"<";
                    }else{
                        parts[2]=".as"+(((Type.Numeric)rType).signed?"I":"U")+8 * (1 << ((Type.Numeric)rType).level)+"}";
                    }
                    return parts;
                }else{
                    throw new UnsupportedOperationException("unimplemented");
                }
            case LE:
            case IF:
            case LSHIFT:
            case RSHIFT:
                throw new UnsupportedOperationException("unimplemented");
        }
        throw new RuntimeException("Unreachable");
    }


    //addLater inlineExpressions to allow assignments i.e. target=A[i] instead of tmp=A,target=tmp[i]
    private int writeExpression(String indent, ArrayList<String> initLines, StringBuilder line, Expression expr, int tmpCount, String procName, String[] varNames) throws IOException {
        if(expr instanceof VarExpression){
            line.append(varNames[((VarExpression)expr).varId]);
            return tmpCount;
        }else if(expr instanceof ThisExpr){
            line.append("(&" + PROC_PREFIX).append(procName).append(")");
            return tmpCount;
        }else if(expr instanceof ValueExpression){
            if(((ValueExpression) expr).constId!=null){
                line.append(CONST_PREFIX).append(asciify(((ValueExpression) expr).constId));
                return tmpCount;
            }else{//TODO inline values without vardata
                int blockCount=expr.expectedType().blockCount;
                DataOut data=new DataOut("data={",0,"tmp",LEN_MASK_TMP);//TODO handle dataOut
                if(blockCount>1){
                    initLines.add(indent+"Value tmp"+tmpCount+" ["+blockCount+"];");
                }else{
                    initLines.add(indent+"Value tmp"+tmpCount+";");
                }
                StringBuilder tmp=new StringBuilder("  tmp"+tmpCount+"=");
                if(blockCount>1){
                    tmp.append('{');
                }
                writeConstValueAsUnion(tmp,((ValueExpression)expr).getValue(),data,true,false,false,true);
                if(blockCount>1){
                    tmp.append('}');
                }
                initLines.add(indent+"{");
                initLines.add(indent+tmp);
                initLines.add(indent+"  // "+data.build.append("}"));
                initLines.add(indent+"}");
                line.append("tmp").append(tmpCount);
                return tmpCount+1;
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
            line.append("/*TODO typeCast:").append(expr.expectedType()).append("*/");
            tmpCount=writeExpression(indent,initLines,line,((TypeCast)expr).value,tmpCount, procName, varNames);
            //TODO perform typecasts
            return tmpCount;
        }else if(expr instanceof IfExpr){
            //TODO? use if instead of ?: to prevent unnecessary execution of code sections
            line.append('(');
            tmpCount=writeExpression(indent,initLines,line,((IfExpr)expr).cond,tmpCount, procName, varNames);
            line.append(".asBool?");
            tmpCount=writeExpression(indent,initLines,line,((IfExpr)expr).ifVal,tmpCount, procName, varNames);
            line.append(':');
            tmpCount=writeExpression(indent,initLines,line,((IfExpr)expr).elseVal,tmpCount, procName, varNames);
            line.append(')');
            return tmpCount;
        }else {
            //TODO other expressions value
            comment(indent,expr.getClass().getSimpleName()+" is currently not supported");
            return tmpCount;
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
        writeLine("    Procedure f=*((Procedure*)initState);");
        writeLine("    initState+=sizeof(Procedure);");
        writeLine("    size_t argCount=*((size_t*)initState);");
        writeLine("    initState+=sizeof(size_t);");
        writeLine("    " + VALUE_BLOCK_NAME + "* argData=*(("+VALUE_BLOCK_NAME+"**)initState);");
        writeLine("    initState+=sizeof(" + VALUE_BLOCK_NAME + "*);");
        writeLine("    " + VALUE_BLOCK_NAME + "* argCache=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");
        writeLine("    if(argCache==NULL){");
        writeLine("        return (void*)-1;");//addLater useful handling of return codes
        writeLine("    }");
        writeLine("    // initArgs");
        writeLine("    memcpy(argCache,initState,argCount*sizeof(" + VALUE_BLOCK_NAME + "));");
        writeLine("    do{");
        writeLine("        f=(Procedure)f(argCache,&argCount,&argData);");
        writeLine("    }while(f!=NULL);");
        writeLine("    return (void*)0;");
        writeLine("}");
        out.newLine();
        comment("main method of the C representation: ");
        comment("  transforms the input arguments and starts the run function on this thread");
        if(hasArgs){
            writeLine("int main(int argc,char** argv){");
            //ignore first argument for consistency with interpreter
            writeLine("  void* init=malloc(sizeof(Procedure)+sizeof(size_t)+sizeof(" + VALUE_BLOCK_NAME + "*)+((1+2*(argc-1))*sizeof("+VALUE_BLOCK_NAME+")));");
        }else{
            writeLine("int main(){");
            writeLine("  void* init=malloc(sizeof(Procedure)+sizeof(size_t));");
        }
        writeLine("  size_t off=0;");
        writeLine("  *((Procedure*)init)=&" + PROC_PREFIX + "start;");
        writeLine("  off+=sizeof(Procedure);");
        writeLine("  *((size_t*)(init+off))=1;");
        writeLine("  off+=sizeof(size_t);");
        writeLine("  " + VALUE_BLOCK_NAME + "* argData=malloc(MAX_ARG_SIZE*sizeof("+VALUE_BLOCK_NAME+"));");//TODO handling of argData
        writeLine("  if(argData==NULL){");
        writeLine("    return -1;");//addLater useful handling of return codes
        writeLine("  }");
        writeLine("  *((" + VALUE_BLOCK_NAME + "**)(init+off))=argData;");
        writeLine("  off+=sizeof(" + VALUE_BLOCK_NAME + "*);");
        if(hasArgs){
            comment("  ","prepare program Arguments");
            //this code only works if argv iss encoded with UTF-8
            comment("  ","!!! currently only UTF-8 encoding is supported !!!");//addLater support for other encodings of argv
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
            writeLine("  }");
        }
        writeLine("  run(init);");
        writeLine("  return 0;");
        writeLine("}");
    }

    public void compile(Parser.ParserContext context) throws IOException {
        writeFileHeader();
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
