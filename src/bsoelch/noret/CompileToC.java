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
        writeLine("typedef enum{");
        writeLine("  EMPTY=0,");//addLater: extract type aliases to variables in Compiler
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
        writeLine("  bool asBool;");
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
        //addLater? more types
        // type->?
        // multi-value types:
        // string->len,    val_ptr/raw_data
        // array->len,     val_ptr/raw_data
        // any->typeID,    val_ptr/raw_data
        // opt->hasData,   data
        writeLine("  uint64_t raw;");
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

    private void writeValueAsUnion(Value v) throws IOException{
        if(v.getType() == Type.Primitive.BOOL){
            out.write("{.asBool="+((Value.Primitive)v).getValue()+"}");
        }else if(v.getType() instanceof Type.Numeric){
            Type.Numeric t=(Type.Numeric) v.getType();
            if(t.isFloat){
                out.write("{.asF"+(8*(1<<t.level))+"="+((Value.Primitive)v).getValue()+"}");
            }else{
                out.write("{.as"+(t.signed?"I":"U")+(8*(1<<t.level))+"="+((Value.Primitive)v).getValue()+"}");
            }
        }else if(v instanceof Value.Array){
            out.write("{.asU64="+((Value.Array) v).elements().length+"}");//TODO use length in Value-Blocks not length of array
            //addLater: variant that does not store contents in place
            for(Value elt:((Value.Array)v).elements()){
                out.write(',');
                writeValueAsUnion(elt);
            }
        }else {
            throw new UnsupportedEncodingException(v.getType()+" is currently not supported in the compiler");
        }
    }

    private void writeConstant(String name, Value value) throws IOException {
        comment("const "+value.getType()+" : "+name+" = "+value.stringRepresentation());
        out.write("Value const_");
        out.write(asciify(name));
        out.write(" []={");
        writeValueAsUnion(value);
        writeLine("};");
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
        writeLine("return NULL;");//tmp body
        writeLine("}");
        out.newLine();
    }
    private void writeRunSignature() throws IOException {
        out.write("void* run(void* initState)");
    }
    private void declareRun() throws IOException {
        writeRunSignature();
        writeLine(";");
    }
    /**writes an integrated interpreted that runs the NoRet C-Representation as a C-Program*/
    private void writeMain() throws IOException {
        comment(" main procedure handling function (written in a way to allow easy usage of pthreads)");
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
        for(Map.Entry<String, Value> e:context.constants.entrySet()){
            writeConstant(e.getKey(),e.getValue());
        }
        //TODO handle native procedures
        for(Map.Entry<String, Procedure> e:context.procNames.entrySet()){
            writeProcDeclaration(e.getKey(),e.getValue());
        }
        out.newLine();
        for(Map.Entry<String, Procedure> e:context.procNames.entrySet()){
            writeProcImplementation(e.getKey(),e.getValue());
        }
        Procedure start= context.getProc("start");
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
