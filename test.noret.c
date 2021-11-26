// Auto generated code from NoRet compiler

#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <inttypes.h>
#include <assert.h>

#define MAX_ARG_SIZE       0x3
#define ARG_DATA_INIT_SIZE 0x1000

#define LEN_MASK_IN_PLACE 0x0
#define LEN_MASK_CONST    0x8000000000000000
#define LEN_MASK_LOCAL    0x4000000000000000
#define LEN_MASK_TMP      0xc000000000000000

// Type Definitions
typedef uint64_t Type;
#define TYPE_SIG_MASK       0xff
#define TYPE_SIG_EMPTY      0x0
#define TYPE_SIG_BOOL       0x1
#define TYPE_SIG_I8         0x2
#define TYPE_SIG_U8         0x3
#define TYPE_SIG_I16        0x4
#define TYPE_SIG_U16        0x5
#define TYPE_SIG_I32        0x6
#define TYPE_SIG_U32        0x7
#define TYPE_SIG_I64        0x8
#define TYPE_SIG_U64        0x9
#define TYPE_SIG_F32        0xa
#define TYPE_SIG_F64        0xb
#define TYPE_SIG_STRING8    0xc
#define TYPE_SIG_STRING16   0xd
#define TYPE_SIG_STRING32   0xe
#define TYPE_SIG_TYPE       0xf
#define TYPE_SIG_NONE       0x10
#define TYPE_SIG_ANY        0x11
#define TYPE_SIG_OPTIONAL   0x12
#define TYPE_SIG_REFERENCE  0x13
#define TYPE_SIG_ARRAY      0x14
#define TYPE_SIG_PROC       0x15
#define TYPE_SIG_STRUCT     0x16
#define TYPE_CONTENT_SHIFT  8
#define TYPE_CONTENT_MASK   0xffffffff
#define TYPE_COUNT_SHIFT    40
#define TYPE_COUNT_MASK     0xffff
// Type data for all contained Types
Type typeData [];

// value-block type
typedef union ValueImpl Value;
// procedure type (the return-type is void* instead of Procedure* to avoid a recursive type definition)
typedef void*(*Procedure)(Value*,Value*,Value**);
// value-block definition
union ValueImpl{
  bool       asBool;
  int8_t     asI8;
  uint8_t    asU8;
  int16_t    asI16;
  uint16_t   asU16;
  int32_t    asI32;
  uint32_t   asU32;
  int64_t    asI64;
  uint64_t   asU64;
  float      asF32;
  double     asF64;
  Type       asType;
  Procedure  asProc;
  Value*     asPtr;
  uint8_t    raw8[8];
  uint16_t   raw16[4];
  uint32_t   raw32[2];
};

// definitions and functions for log
typedef enum{
  null,// blank type for initial value
  DEFAULT,
  ERR,
  DEBUG,
  INFO,
}LogType;
// previously used logType
static LogType prevType = null;
// definition of NEW_LINE character
#define NEW_LINE "\n"
FILE* log_DEFAULT;
FILE* log_ERR;
FILE* log_DEBUG;
FILE* log_INFO;
// sets log-streams to their initial value
void initLogStreams(){
  log_DEFAULT = stdout;
  log_ERR = stderr;
  log_DEBUG = stdout;
  log_INFO = stdout;
}
// recursive printing of types
void printType(const Type type,FILE* log){
  switch(type&TYPE_SIG_MASK){
    case TYPE_SIG_EMPTY:
      fputs("Type:\"empty\"",log);
      break;
    case TYPE_SIG_BOOL:
      fputs("Type:bool",log);
      break;
    case TYPE_SIG_I8:
      fputs("Type:int8",log);
      break;
    case TYPE_SIG_U8:
      fputs("Type:uint8",log);
      break;
    case TYPE_SIG_I16:
      fputs("Type:int16",log);
      break;
    case TYPE_SIG_U16:
      fputs("Type:uint16",log);
      break;
    case TYPE_SIG_I32:
      fputs("Type:int32",log);
      break;
    case TYPE_SIG_U32:
      fputs("Type:uint32",log);
      break;
    case TYPE_SIG_I64:
      fputs("Type:int64",log);
      break;
    case TYPE_SIG_U64:
      fputs("Type:uint64",log);
      break;
    case TYPE_SIG_F32:
      fputs("Type:float32",log);
      break;
    case TYPE_SIG_F64:
      fputs("Type:float64",log);
      break;
    case TYPE_SIG_STRING8:
      fputs("Type:string8",log);
      break;
    case TYPE_SIG_STRING16:
      fputs("Type:string16",log);
      break;
    case TYPE_SIG_STRING32:
      fputs("Type:string32",log);
      break;
    case TYPE_SIG_TYPE:
      fputs("Type:type",log);
      break;
    case TYPE_SIG_NONE:
      fputs("Type:\"none\"",log);
      break;
    case TYPE_SIG_ANY:
      fputs("Type:any",log);
      break;
    case TYPE_SIG_OPTIONAL:
      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log);
      fputs("?",log);
      break;
    case TYPE_SIG_REFERENCE:
      fputs("@",log);
      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log);
      break;
    case TYPE_SIG_ARRAY:
      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log);
      fputs("[]",log);
      break;
    case TYPE_SIG_PROC:
    case TYPE_SIG_STRUCT:
      assert(false && " unreachable ");
      break;
  }
}
// log-Method
void logValue(LogType logType,bool append,const Type type,const Value* value){
  if(prevType!=null){
    if((logType!=prevType)||(!append)){
      switch(prevType){
        case null:
          break;
        case DEFAULT:
          fputs(NEW_LINE,log_DEFAULT);
          break;
        case ERR:
          fputs(NEW_LINE,log_ERR);
          break;
        case DEBUG:
          fputs(NEW_LINE,log_DEBUG);
          break;
        case INFO:
          fputs(NEW_LINE,log_INFO);
          break;
      }
    }
  }
  FILE* log;
  switch(logType){
    case null:
    case DEFAULT:
      log=log_DEFAULT;
      break;
    case ERR:
      log=log_ERR;
      break;
    case DEBUG:
      log=log_DEBUG;
      break;
    case INFO:
      log=log_INFO;
      break;
  }
  switch(type&TYPE_SIG_MASK){
    case TYPE_SIG_EMPTY:
      fputs("unexpected Value-Type in log: \"EMPTY\"",log_ERR);
      exit(-1);
      break;
    case TYPE_SIG_BOOL:
      fputs(value->asBool?"true":"false",log);
      break;
    case TYPE_SIG_I8:
      fprintf(log,"%"PRIi8,value->asI8);
      break;
    case TYPE_SIG_U8:
      fprintf(log,"%"PRIu8,value->asI8);
      break;
    case TYPE_SIG_I16:
      fprintf(log,"%"PRIi16,value->asI16);
      break;
    case TYPE_SIG_U16:
      fprintf(log,"%"PRIu16,value->asI16);
      break;
    case TYPE_SIG_I32:
      fprintf(log,"%"PRIi32,value->asI32);
      break;
    case TYPE_SIG_U32:
      fprintf(log,"%"PRIu32,value->asI32);
      break;
    case TYPE_SIG_I64:
      fprintf(log,"%"PRIi64,value->asI64);
      break;
    case TYPE_SIG_U64:
      fprintf(log,"%"PRIu64,value->asI64);
      break;
    case TYPE_SIG_F32:
      fprintf(log,"%f",value->asF32);
      break;
    case TYPE_SIG_F64:
      fprintf(log,"%f",value->asF64);
      break;
    case TYPE_SIG_NONE:
      fputs("\"none\"",log);
      break;
    case TYPE_SIG_STRING8:
    case TYPE_SIG_STRING16:
    case TYPE_SIG_STRING32:
      assert(false && "unimplemented");
      break;
    case TYPE_SIG_TYPE:
      printType(value->asType,log);
      break;
    case TYPE_SIG_ANY:
       prevType=logType;
       logValue(logType,true,value->asType,value+1/*content*/);
       break;
    case TYPE_SIG_OPTIONAL:
      if(value[0].asBool){
        prevType=logType;
        fputs("Optional{",log);
        logValue(logType,true,typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],value+1/*value*/);
        fputs("}",log);
      }else{
        fputs("Optional{}",log);
      }
      break;
    case TYPE_SIG_REFERENCE:
    case TYPE_SIG_ARRAY:
    case TYPE_SIG_PROC:
    case TYPE_SIG_STRUCT:
      assert(false && " unimplemented ");
      break;
    default:
      assert(false && " unreachable ");
      break;
  }
  prevType=logType;
}

Value constData [];
// const Type:int8 : constant = 42
const Value const_constant []={{.asI8=42}};
// const Type:any : array_test = {{1,2,3},{4,5},{6}}
const Value const_array__test []={{.asType=TYPE_SIG_ARRAY|(1<<TYPE_CONTENT_SHIFT)},{.asPtr=(constData+0)},};
// const Type:string8[] : str_test = {"str1","str2"}
const Value const_str__test []={{.asU64=0x2},{.asU64=0x8000000000000004},{.asPtr=(constData+13)},{.asU64=0x8000000000000004},{.asPtr=(constData+14)}};
// const Type:int32[] : y = {2112454933,2,3}
const Value const_y []={{.asU64=0x3},{.asI32=2112454933},{.asI32=2},{.asI32=3}};
// const Type:(((int32[])[])?)[] : type_sig_test = {}
const Value const_type__sig__test []={{.asU64=0x0}};
// data for values used in constants
Value constData []={{.asU64=0x3},{.asU64=0x8000000000000003},{.asPtr=(constData+3)},{.asI32=1},{.asI32=2},{.asI32=3},{.asU64=0x8000000000000002},{.asPtr=(constData+8)},{.asI32=4},{.asI32=5},{.asU64=0x8000000000000001},{.asPtr=(constData+12)},{.asI32=6},{.raw8={0x73,0x74,0x72,0x31,0x0,0x0,0x0,0x0}},{.raw8={0x73,0x74,0x72,0x32,0x0,0x0,0x0,0x0}}};

// start(Type:string8[])
void* proc_start(Value* argsIn,Value* argsOut,Value** argData);
// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* argsIn,Value* argsOut,Value** argData);
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* run(void* initState);

// start(Type:string8[])
void* proc_start(Value* argsIn,Value* argsOut,Value** argData){
  // var0:(argsIn+0)
  Value var1 [2];// (Type:string8)
  {// Initialize: ValueExpression{"UTF8-String"}
    Value tmp0 [2];
    {
      memcpy(tmp0,(Value[]){(Value){.asU64=0xc00000000000000b},(Value){.asPtr=(tmp+0)}},2);
      // TODO handle data
      // data={(Value){.raw8={0x55,0x54,0x46,0x38,0x2d,0x53,0x74,0x72}},(Value){.raw8={0x69,0x6e,0x67,0x0,0x0,0x0,0x0,0x0}}}
    }
    memcpy(var1,tmp0,2);
  }
  Value var2 [2];// (Type:string16)
  {// Initialize: ValueExpression{"UTF16-String"}
    Value tmp0 [2];
    {
      memcpy(tmp0,(Value[]){(Value){.asU64=0xc00000000000000c},(Value){.asPtr=(tmp+0)}},2);
      // TODO handle data
      // data={(Value){.raw16={0x55,0x54,0x46,0x31}},(Value){.raw16={0x36,0x2d,0x53,0x74}},(Value){.raw16={0x72,0x69,0x6e,0x67}}}
    }
    memcpy(var2,tmp0,2);
  }
  Value var3 [2];// (Type:string32)
  {// Initialize: ValueExpression{"UTF32-String"}
    Value tmp0 [2];
    {
      memcpy(tmp0,(Value[]){(Value){.asU64=0xc00000000000000c},(Value){.asPtr=(tmp+0)}},2);
      // TODO handle data
      // data={(Value){.raw32={0x55,0x54}},(Value){.raw32={0x46,0x33}},(Value){.raw32={0x32,0x2d}},(Value){.raw32={0x53,0x74}},(Value){.raw32={0x72,0x69}},(Value){.raw32={0x6e,0x67}}}
    }
    memcpy(var3,tmp0,2);
  }
  Value var4;// (Type:int32)
  {// Initialize: BinOp{ValueExpression{1} PLUS TypeCast{Type:int32:GetField{VarExpression{0}.length}}}
    var4=(Value){.asI32=((int32_t)((Value){.asI32=1}).asI32)+((int32_t)(Value){.asI32=(int32_t)(((argsIn+0))[0]).asU64}.asI32)};
  }
  Value var5;// (Type:int32)
  {// Initialize: BinOp{BinOp{VarExpression{4} MULT VarExpression{4}} MINUS BinOp{VarExpression{4} INT_DIV ValueExpression{2}}}
    var5=(Value){.asI32=((int32_t)(Value){.asI32=((int32_t)var4.asI32)*((int32_t)var4.asI32)}.asI32)-((int32_t)(Value){.asI32=((int32_t)var4.asI32)/((int32_t)((Value){.asI32=2}).asI32)}.asI32)};
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:int32[]}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,&((Value){.asType=TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT)}));
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:(((int32[])[])?)[]}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,&((Value){.asType=TYPE_SIG_ARRAY|(3<<TYPE_CONTENT_SHIFT)}));
  }
  Value var6;// (Type:uint64)
  {// Initialize: ValueExpression{3}
    var6=((Value){.asU64=3});
  }
  {// Log: Log[DEFAULT]{VarExpression{6}}
    logValue(DEFAULT,false,TYPE_SIG_U64,&var6);
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:"none"}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,&((Value){.asType=TYPE_SIG_NONE}));
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:"empty"[]}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,&((Value){.asType=TYPE_SIG_ARRAY|(4<<TYPE_CONTENT_SHIFT)}));
  }
  Value var7 [2];// (Type:int32?)
  {// Initialize: TypeCast{Type:int32?:ValueExpression{none}}
    memcpy(var7,(Value[]){(Value){.asBool=false},((Value){.asBool=false/*none*/})},2);
  }
  {// Log: Log[DEFAULT]{VarExpression{7}}
    logValue(DEFAULT,false,TYPE_SIG_OPTIONAL|(0<<TYPE_CONTENT_SHIFT),var7);
  }
  {// Log: Log[DEFAULT]{IfExpr{TypeCast{Type:bool:VarExpression{7}}?TypeCast{Type:int32?:GetField{VarExpression{7}.value}}:TypeCast{Type:int32?:ValueExpression{none}}}}
    Value tmp0 [2];
    {
      if((var7)[0].asBool){
        memcpy(tmp0,(Value[]){(Value){.asBool=true},(var7)[1]},2);
      }else{
        memcpy(tmp0,(Value[]){(Value){.asBool=false},((Value){.asBool=false/*none*/})},2);
      }
    }
    logValue(DEFAULT,false,TYPE_SIG_OPTIONAL|(0<<TYPE_CONTENT_SHIFT),tmp0);
  }
  Procedure ret=NULL;
  return ret;
}

// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* argsIn,Value* argsOut,Value** argData){
  // var0:(argsIn+0)
  // var1:(*(argsIn+2))
  // Native
  return NULL;
}

// declarations of all used type Signatures
Type typeData []={TYPE_SIG_I32,TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT),TYPE_SIG_ARRAY|(1<<TYPE_CONTENT_SHIFT),TYPE_SIG_OPTIONAL|(2<<TYPE_CONTENT_SHIFT),TYPE_SIG_EMPTY};
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* run(void* initState){
    Procedure f=*((Procedure*)initState);
    initState+=sizeof(Procedure);
    Value* argsI=*((Value**)initState);
    initState+=sizeof(Value*);
    Value* argData=*((Value**)initState);
    initState+=sizeof(Value*);
    Value* argsO=malloc(MAX_ARG_SIZE*sizeof(Value));
    Value* argsTmp;
    if(argsO==NULL){
        return (void*)-1;
    }
    // initArgs
    memcpy(argsI,initState,argCount*sizeof(Value));
    do{
        f=(Procedure)f(argsI,argsO,&argData);
        // swap args
        argsTmp=argsI;
        argsI=argsO;
        argsO=argsTmp;
    }while(f!=NULL);
    return (void*)0;
}

// main method of the C representation: 
//   transforms the input arguments and starts the run function on this thread
int main(int argc,char** argv){
  // [proc_ptr,args_ptr,arg_data]
  void* init=malloc(sizeof(Procedure)+2*sizeof(Value*));
  size_t off=0;
  *((Procedure*)init)=&proc_start;
  off+=sizeof(Procedure);
  Value* initArgs=malloc(MAX_ARG_SIZE*sizeof(Value));
  if(initArgs==NULL){
    return -1;
  }
  *((Value**)(init+off))=initArgs;
  off+=sizeof(Value*);
  Value* argData=malloc(ARG_DATA_INIT_SIZE*sizeof(Value));
  if(argData==NULL){
    return -1;
  }
  *((Value**)(init+off))=argData;
  off+=sizeof(Value*);
  // prepare program Arguments
  // !!! currently only UTF-8 encoding is supported !!!
  assert(false && "unimplemented");
  initLogStreams();
  run(init);
  puts("");// finish last line in stdout
  return 0;
}
