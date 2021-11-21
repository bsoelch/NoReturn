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

    private static final String[] procedureArgs     ={"Value*", "size_t*"   };
    private static final String[] procedureArgNames ={"args",   "argCount"  };
    private static final String procedureOut="void*";

    private static final String CONST_DATA_SIGNATURE = "Value constData []";

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
        writeLine("#define MAX_ARG_SIZE 1024;");
        out.newLine();
        //Type enum //TODO better storage structure
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
        writeLine("  char     raw[8];");
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

    /**Writes a value as a array of union declarations*/
    private void writeConstValueAsUnion(boolean isFirst,StringBuilder out,Value v, boolean inPlaceValues,boolean incOff,ConstData constData) throws IOException{
        if(!isFirst){
            out.append(',');
        }
        if(v.getType() == Type.Primitive.BOOL){
            out.append("{.asBool=").append(((Value.Primitive) v).getValue()).append("}");
            if(incOff){constData.off++;}
        }else if(v.getType() instanceof Type.Numeric){
            Type.Numeric t=(Type.Numeric) v.getType();
            if(t.isFloat){
                out.append("{.asF").append(8 * (1 << t.level)).append("=").append(((Value.Primitive) v).getValue()).append("}");
                if(incOff){constData.off++;}
            }else{
                out.append("{.as").append(t.signed ? "I" : "U").append(8 * (1 << t.level)).append("=")
                        .append(((Value.Primitive) v).getValue()).append("}");
                if(incOff){constData.off++;}
            }
        }else if(v.getType()== Type.Primitive.STRING){
            byte[] bytes=((Value.StringValue) v).utf8Bytes();
            if(inPlaceValues){
                out.append("{.asU64=").append(bytes.length).append("}");
                if(incOff){constData.off++;}
                for(int i=0;i< bytes.length;i+=8){
                    out.append(",{.asRaw={");
                    for(int j=0;j<8;j++){
                        if(j>0){
                            out.append(',');
                        }
                        out.append("0x").append((i + j < bytes.length) ? Integer.toHexString(bytes[i + j] & 0xff) : "00");
                    }
                    out.append("}");
                    if(incOff){constData.off++;}
                }
            }else{
                //TODO mark value to signal storage-location at runtime
                out.append("{.asU64=").append(bytes.length).append("}");
                if(incOff){constData.off++;}
                if(incOff){constData.off++;}//increment before store
                out.append(",{.asPtr=(constData+").append(constData.off).append(")}");
                for(int i=0;i< bytes.length;i+=8){
                    if(constData.off>0){
                        constData.build.append(',');
                    }
                    constData.build.append("{.asRaw={");
                    for(int j=0;j<8;j++){
                        if(j>0){
                            constData.build.append(',');
                        }
                        constData.build.append("0x").append((i + j < bytes.length) ? Integer.toHexString(bytes[i + j] & 0xff) : "00");
                    }
                    constData.build.append("}");
                    constData.off++;
                }
            }
        }else if(v instanceof Value.Array){
            //TODO handle any[]
            if(((Type.Array)v.getType()).content== Type.Primitive.ANY){
                throw new UnsupportedOperationException("Type any is not supported");
            }
            if(inPlaceValues) {
                out.append("{.asU64=").append(((Value.Array) v).elements().length).append("}");
                if(incOff){constData.off++;}
                //ensure constant block-size (1 for bool,float[N],[u]int[N],reference  2 for string, array, any, optional)
                for (Value elt : ((Value.Array) v).elements()) {
                    writeConstValueAsUnion(false,out,elt, false,incOff,constData);
                }
            }else{
                //TODO mark value to signal storage-location at runtime
                out.append("{.asU64=").append(((Value.Array) v).elements().length).append("}");
                if(incOff){constData.off++;}
                if(incOff){constData.off++;}//increment before write
                out.append(",{.asPtr=(constData+").append(constData.off).append(")}");
                for (Value elt : ((Value.Array) v).elements()) {
                    writeConstValueAsUnion(constData.off==0,constData.build, elt, true,true,constData);
                }
            }
        }else {
            throw new UnsupportedEncodingException(v.getType()+" is currently not supported in the compiler");
        }
    }

    private void writeConstant(String name, Value value, ConstData constData) throws IOException {
        comment("const "+value.getType()+" : "+name+" = "+value.stringRepresentation());
        out.write("Value const_");
        out.write(asciify(name));
        out.write(" []={");
        StringBuilder tmp=new StringBuilder();
        writeConstValueAsUnion(true,tmp,value,true,false,constData);
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
    private void writeMain() throws IOException {
        writeRunSignature();writeLine("{");
        writeLine("    Procedure f=*((Procedure*)initState);");
        writeLine("    initState+=sizeof(Procedure);");
        writeLine("    size_t argCount=*((size_t*)initState);");
        writeLine("    initState+=sizeof(size_t);");
        writeLine("    Value* argCache=malloc(MAX_ARG_SIZE*sizeof(Value));");
        writeLine("    if(argCache==NULL){");//TODO better error handling
        writeLine("        return (void*)-1;");//addLater usefull handling of return codes
        writeLine("    }");
        writeLine("    //initArgs");
        writeLine("    memcpy(argCache,initState,argCount*sizeof(Value));");
        writeLine("    do{");
        writeLine("        f=(Procedure)f(argCache,&argCount);");
        writeLine("    }while(f!=NULL);");
        writeLine("    return (void*)0;");
        writeLine("}");
        out.newLine();
        comment("main method of the C representation: ");
        comment("  transforms the input arguments and starts the run function on this thread");
        writeLine("int main(){");//TODO choose main depending on signature of start
        writeLine("	void* init=malloc(sizeof(Procedure)+sizeof(size_t));");//addLater add size of argString
        writeLine("	size_t off=0;");
        writeLine("	*((Procedure*)init)=&proc_start;");
        writeLine("	off+=sizeof(Procedure);");
        writeLine("	*((size_t*)(init+off))=1;");
        writeLine("	off+=sizeof(size_t);");
        //addLater pass Arguments to procedure
        writeLine("	run(init);");
        writeLine("	return 0;");
        writeLine("}");
    }

    //TODO compile code representation to C

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
                        ((Type.Array) startTypes[0]).content != Type.Primitive.STRING) {
                    throw new SyntaxError("wrong signature of start, " +
                            "expected ()=>? or (string[])=>?");
                }
                //TODO main with args
            }else{
                writeMain();
            }
        }
        out.flush();
    }

}
