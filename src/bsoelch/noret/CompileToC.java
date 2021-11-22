package bsoelch.noret;

import bsoelch.noret.lang.Procedure;
import bsoelch.noret.lang.Type;
import bsoelch.noret.lang.Value;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

public class CompileToC {

    private static final String[] procedureArgs     ={"Value*", "size_t*",  "Value**"};
    private static final String[] procedureArgNames ={"args",   "argCount", "argData"};
    private static final String procedureOut="void*";

    private static final String CONST_DATA_SIGNATURE = "Value constData []";
    private static final int MAX_ARG_SIZE = 0x2000;

    private static final long LEN_MASK_IN_PLACE = 0x0000000000000000L;
    private static final long LEN_MASK_CONST    = 0x8000000000000000L;
    private static final long LEN_MASK_LOCAL    = 0x4000000000000000L;
    private static final long LEN_MASK_TMP      = 0xC000000000000000L;

    private static class ConstData{
        StringBuilder build=new StringBuilder(CONST_DATA_SIGNATURE + "={");
        long off=0;
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
        writeLine("typedef union ValueImpl Value;");
        writeLine("union ValueImpl{");
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
        writeLine("  Value*   asPtr;");//reference
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

    private void writeNumber(StringBuilder out, Value v, ConstData constData, boolean incOff) {
        Type.Numeric t=(Type.Numeric) v.getType();
        if(t.isFloat){
            out.append("{.asF").append(8 * (1 << t.level)).append("=").append(((Value.Primitive) v).getValue()).append("}");
        }else{
            out.append("{.as").append(t.signed ? "I" : "U").append(8 * (1 << t.level)).append("=")
                    .append(((Value.Primitive) v).getValue()).append("}");
        }
        if(incOff){
            constData.off++;}
    }

    /**Writes a value as a array of union declarations*/
    private void writeConstValueAsUnion(StringBuilder out, Value v, ConstData constData, boolean isFirst, boolean inPlaceValues,
                                        boolean incOff, boolean wrapAny) throws IOException{
        if(!isFirst){
            out.append(',');
        }
        if(wrapAny){
            //TODO store Types
            out.append("{.asType=0/*").append(v.getType()).append("*/}");
            if(incOff){constData.off++;}
            if(incOff){constData.off++;}//increment before store
            if(v.getType()==Type.Primitive.BOOL){
                out.append(",{.asBool=").append(((Value.Primitive) v).getValue()).append("}");
                if(incOff){constData.off++;}
            }else if(v.getType() instanceof Type.Numeric){
                out.append(',');
                writeNumber(out, v, constData, incOff);
            }else{//addLater different handling for optional/reference
                out.append(",{.asPtr=(constData+").append(constData.off).append(")}");
                writeConstValueAsUnion(constData.build, v, constData, constData.off==0, true, true, false);
            }
        }else if(v.getType() == Type.Primitive.BOOL){
            out.append("{.asBool=").append(((Value.Primitive) v).getValue()).append("}");
            if(incOff){constData.off++;}
        }else if(v.getType() instanceof Type.Numeric){
            writeNumber(out, v, constData, incOff);
        }else if(v.getType()== Type.NoRetString.STRING8){
            byte[] bytes=((Value.StringValue) v).utf8Bytes();
            if(inPlaceValues){
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|bytes.length)).append("}");
                if(incOff){constData.off++;}
                out.append(',');
            }else{
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_CONST|bytes.length)).append("}");
                if(incOff){constData.off++;}
                if(incOff){constData.off++;}//increment before store
                out.append(",{.asPtr=(constData+").append(constData.off).append(")}");
                out= constData.build;
                incOff=true;
                if(constData.off>0){
                    out.append(',');
                }
            }
            for(int i=0;i< bytes.length;i+=8){
                out.append("{.raw8={");
                for(int j=0;j<8;j++){
                    if(j>0){
                        out.append(',');
                    }
                    out.append("0x").append((i + j < bytes.length) ? Integer.toHexString(bytes[i + j] & 0xff) : "0");
                }
                out.append("}}");
                if(incOff){constData.off++;}
            }
        }else if(v.getType()== Type.NoRetString.STRING16){
            char[] chars=((Value.StringValue) v).chars();
            if(inPlaceValues){
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|chars.length)).append("}");
                if(incOff){constData.off++;}
                out.append(',');
            }else{
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_CONST|chars.length)).append("}");
                if(incOff){constData.off++;}
                if(incOff){constData.off++;}//increment before store
                out.append(",{.asPtr=(constData+").append(constData.off).append(")}");
                out= constData.build;
                incOff=true;
                if(constData.off>0){
                    out.append(',');
                }
            }
            for(int i=0;i< chars.length;i+=4){
                out.append("{.raw16={");
                for(int j=0;j<4;j++){
                    if(j>0){
                        out.append(',');
                    }
                    out.append("0x").append((i + j < chars.length) ? Integer.toHexString(chars[i + j] & 0xffff) : "0");
                }
                out.append("}}");
                if(incOff){constData.off++;}
            }
        }else if(v.getType()== Type.NoRetString.STRING32){
            int[] codePoints=((Value.StringValue) v).codePoints();
            if(inPlaceValues){
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|codePoints.length)).append("}");
                if(incOff){constData.off++;}
                out.append(',');
            }else{
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_CONST|codePoints.length)).append("}");
                if(incOff){constData.off++;}
                if(incOff){constData.off++;}//increment before store
                out.append(",{.asPtr=(constData+").append(constData.off).append(")}");
                out= constData.build;
                incOff=true;
                if(constData.off>0){
                    out.append(',');
                }
            }
            for(int i=0;i< codePoints.length;i+=2){
                out.append("{.raw32={");
                for(int j=0;j<2;j++){
                    if(j>0){
                        out.append(',');
                    }
                    out.append("0x").append((i + j < codePoints.length) ? Integer.toHexString(codePoints[i + j]) : "0");
                }
                out.append("}}");
                if(incOff){constData.off++;}
            }
        }else if(v instanceof Value.Array){
            boolean isAny=(((Type.Array)v.getType()).content== Type.Primitive.ANY);
            if(inPlaceValues) {
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_IN_PLACE|((Value.Array) v).elements().length)).append("}");
                if(incOff){constData.off++;}
                //ensure constant block-size (1 for bool,float[N],[u]int[N],reference  2 for string, array, any, optional)
                for (Value elt : ((Value.Array) v).elements()) {
                    writeConstValueAsUnion(out, elt, constData, false, false, incOff, isAny);
                }
            }else{
                out.append("{.asU64=0x").append(Long.toHexString(LEN_MASK_CONST|((Value.Array) v).elements().length)).append("}");
                if(incOff){constData.off++;}
                if(incOff){constData.off++;}//increment before write
                out.append(",{.asPtr=(constData+").append(constData.off).append(")}");
                for (Value elt : ((Value.Array) v).elements()) {
                    writeConstValueAsUnion(constData.build, elt, constData, constData.off==0, true, true, isAny);
                }
            }
        }else {
            throw new UnsupportedEncodingException(v.getType()+" is currently not supported in the compiler");
        }
    }

    private void writeConstant(String name, Value value, ConstData constData) throws IOException {
        comment("const "+value.getType()+" : "+name+" = "+value.stringRepresentation());
        out.write("const Value const_");
        out.write(asciify(name));
        out.write(" []={");
        StringBuilder tmp=new StringBuilder();
        writeConstValueAsUnion(tmp, value, constData, true, true, false, false);
        writeLine(tmp.append("};").toString());
    }

    private void writeProcSignature(String name, Procedure proc) throws IOException{
        String tmp=Arrays.toString(proc.argTypes());
        comment(name+"("+tmp.substring(1,tmp.length()-1)+")");
        out.write(procedureOut+" proc_"+asciify(name)+"(");
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
        //TODO procedure Body
        writeLine("return NULL;");//addLater return next procedure
        writeLine("}");
        out.newLine();
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
        writeLine("    Value* argData=*((Value**)initState);");
        writeLine("    initState+=sizeof(Value*);");
        writeLine("    Value* argCache=malloc(MAX_ARG_SIZE*sizeof(Value));");
        writeLine("    if(argCache==NULL){");
        writeLine("        return (void*)-1;");//addLater useful handling of return codes
        writeLine("    }");
        writeLine("    //initArgs");
        writeLine("    memcpy(argCache,initState,argCount*sizeof(Value));");
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
            writeLine("  void* init=malloc(sizeof(Procedure)+sizeof(size_t)+sizeof(Value*)+((1+2*(argc-1))*sizeof(Value)));");
        }else{
            writeLine("int main(){");
            writeLine("  void* init=malloc(sizeof(Procedure)+sizeof(size_t));");
        }
        writeLine("  size_t off=0;");
        writeLine("  *((Procedure*)init)=&proc_start;");
        writeLine("  off+=sizeof(Procedure);");
        writeLine("  *((size_t*)(init+off))=1;");
        writeLine("  off+=sizeof(size_t);");
        writeLine("  Value* argData=malloc(MAX_ARG_SIZE*sizeof(Value));");//TODO handling of argData
        writeLine("  if(argData==NULL){");
        writeLine("    return -1;");//addLater useful handling of return codes
        writeLine("  }");
        writeLine("  *((Value**)(init+off))=argData;");
        writeLine("  off+=sizeof(Value*);");
        if(hasArgs){
            comment("  ","prepare program Arguments");
            //this code only works if argv iss encoded with UTF-8
            comment("  ","!!! currently only UTF-8 encoding is supported !!!");//addLater support for other encodings of argv
            writeLine("  int l;");
            writeLine("  int k0=0;");
            writeLine("  for(int i=1;i<argc;i++){");//skip first argument
            writeLine("    int l=strlen(argv[i]);");
            writeLine("    *((Value*)(init+off))=(Value){.asU64=LEN_MASK_LOCAL|l};");//store lengths of arguments
            writeLine("    off+=sizeof(Value);");
            writeLine("    *((Value*)(init+off))=(Value){.asPtr=argData+k0};");//store pointer to data
            writeLine("    off+=sizeof(Value);");
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
        ConstData constData=new ConstData();
        writeLine(CONST_DATA_SIGNATURE+";");
        for(Map.Entry<String, Value> e:context.constants.entrySet()){
            writeConstant(e.getKey(),e.getValue(),constData);
        }
        comment("data for values used in constants");
        writeLine(constData.build.append("};").toString());
        out.newLine();
        //TODO handle native procedures
        for(Map.Entry<String, Procedure> e:context.procNames.entrySet()){
            writeProcDeclaration(e.getKey(),e.getValue());
        }
        Procedure start= context.getProc("start");
        if(start!=null){
            declareRun();
        }
        out.newLine();
        for(Map.Entry<String, Procedure> e:context.procNames.entrySet()){
            writeProcImplementation(e.getKey(),e.getValue());
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
