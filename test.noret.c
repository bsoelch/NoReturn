// Auto generated code from NoRet compiler

#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <inttypes.h>

#define MAX_ARG_SIZE 0x2000

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
Type[] typeData;

// Value-Block Definition
typedef union ValueImpl Value;
union ValueImpl{
  bool     asBool;
  int8_t   asI8;
  uint8_t  asU8;
  int16_t  asI16;
  uint16_t asU16;
  int32_t  asI32;
  uint32_t asU32;
  int64_t  asI64;
  uint64_t asU64;
  float    asF32;
  double   asF64;
  Type     asType;
  Value*   asPtr;
  uint8_t  raw8[8];
  uint16_t raw16[4];
  uint32_t raw32[2];
};

// Procedure Type
typedef void*(*Procedure)(Value*,size_t*,Value**);
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
// log-Method
void logValue(LogType logType,bool append,Type type,Value* value){
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
      assert false&&"unimplemented";
      break;
    case TYPE_SIG_TYPE:
      assert false&&"unimplemented";
      break;
    case TYPE_SIG_ANY:
       prevType=logType;
       logValue(logType,true,*value/*type*/,value+1/*content*/)
       break;
    case TYPE_SIG_OPTIONAL:
    case TYPE_SIG_REFERENCE:
    case TYPE_SIG_ARRAY:
    case TYPE_SIG_PROC:
    case TYPE_SIG_STRUCT:
      assert false&&"unimplemented";
      break;
    default:
      assert false&&"unreachable";
      break;
  }
  prevType=logType;
}

Value constData [];
// const Type:int8 : constant = 42
const Value const_constant []={{.asI8=42}};
// const Type:any : array_test = {{1,2,3},{4,5},{6}}
const Value const_array__test []={{.asType=TYPE_SIG_ARRAY|(null<<TYPE_CONTENT_SHIFT)},{.asPtr=(constData+0)},};
// const Type:string8[] : str_test = {"str1","str2"}
const Value const_str__test []={{.asU64=0x2},{.asU64=0x8000000000000004},{.asPtr=(constData+13)},{.asU64=0x8000000000000004},{.asPtr=(constData+14)}};
// const Type:int32[] : y = {2112454933,2,3}
const Value const_y []={{.asU64=0x3},{.asI32=2112454933},{.asI32=2},{.asI32=3}};
// const Type:(((int32[])[])?)[] : type_sig_test = {}
const Value const_type__sig__test []={{.asU64=0x0}};
// data for values used in constants
Value constData []={{.asU64=0x3},{.asU64=0x8000000000000003},{.asPtr=(constData+3)},{.asI32=1},{.asI32=2},{.asI32=3},{.asU64=0x8000000000000002},{.asPtr=(constData+8)},{.asI32=4},{.asI32=5},{.asU64=0x8000000000000001},{.asPtr=(constData+12)},{.asI32=6},{.raw8={0x73,0x74,0x72,0x31,0x0,0x0,0x0,0x0}},{.raw8={0x73,0x74,0x72,0x32,0x0,0x0,0x0,0x0}}};

// loop(Type:int8)
void* proc_loop(Value* args,size_t* argCount,Value** argData);
// start(Type:string8[])
void* proc_start(Value* args,size_t* argCount,Value** argData);
// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount,Value** argData);
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* run(void* initState);

// loop(Type:int8)
void* proc_loop(Value* args,size_t* argCount,Value** argData){
  // var0:(*(args+0))
  {// Log: Log[DEFAULT]{VarExpression{0}}
    logValue(DEFAULT,false,TYPE_SIG_I8,(*(args+0)));
  }
  Value var1 [2];// (Type:((Type:int8)=>?)?)
  {// Initialize: IfExpr{BinOp{VarExpression{0} LT ValueExpression{5}}?TypeCast{Type:((Type:int8)=>?)?:this}:TypeCast{Type:((Type:int8)=>?)?:ValueExpression{none}}}
    Value tmp0 [2];
    {
      if((Value){.asBool=(*(args+0)).asI8<((Value){.asI32=5}).asI32}){
        tmp0=/*TODO typeCast:Type:((Type:int8)=>?)?*/(&proc_loop);
      }else{
        tmp0=/*TODO typeCast:Type:((Type:int8)=>?)?*/((Value){.asBool=false/*none*/});
      }
    }
    var1=tmp0;
  }
  return NULL;
}

// start(Type:string8[])
void* proc_start(Value* args,size_t* argCount,Value** argData){
  // var0:(*((Value[2])(args+0)))
  Value var1 [2];// (Type:string8)
  {// Initialize: ValueExpression{"UTF8-String"}
    Value tmp0 [2];
    {
      tmp0={(Value){.asU64=0xc00000000000000b},(Value){.asPtr=(tmp+0)}}
      // data={(Value){.raw8={0x55,0x54,0x46,0x38,0x2d,0x53,0x74,0x72}},(Value){.raw8={0x69,0x6e,0x67,0x0,0x0,0x0,0x0,0x0}}}
    }
    var1=tmp0;
  }
  Value var2 [2];// (Type:string16)
  {// Initialize: ValueExpression{"UTF16-String"}
    Value tmp0 [2];
    {
      tmp0={(Value){.asU64=0xc00000000000000c},(Value){.asPtr=(tmp+0)}}
      // data={(Value){.raw16={0x55,0x54,0x46,0x31}},(Value){.raw16={0x36,0x2d,0x53,0x74}},(Value){.raw16={0x72,0x69,0x6e,0x67}}}
    }
    var2=tmp0;
  }
  Value var3 [2];// (Type:string32)
  {// Initialize: ValueExpression{"UTF32-String"}
    Value tmp0 [2];
    {
      tmp0={(Value){.asU64=0xc00000000000000c},(Value){.asPtr=(tmp+0)}}
      // data={(Value){.raw32={0x55,0x54}},(Value){.raw32={0x46,0x33}},(Value){.raw32={0x32,0x2d}},(Value){.raw32={0x53,0x74}},(Value){.raw32={0x72,0x69}},(Value){.raw32={0x6e,0x67}}}
    }
    var3=tmp0;
  }
  Value var4;// (Type:int32)
  {// Initialize: BinOp{ValueExpression{1} PLUS TypeCast{Type:int32:GetField{VarExpression{0}.length}}}
    // TODO GetField is currently not supported
    var4=(Value){.asI32=((Value){.asI32=1}).asI32+/*TODO typeCast:Type:int32*/.asI32};
  }
  Value var5 [2];// (Type:int32[])
  {// Initialize: ValueExpression{{1,-2,3,42}}
    Value tmp0 [2];
    {
      tmp0={(Value){.asU64=0xc000000000000004},(Value){.asPtr=(tmp+0)}}
      // data={(Value){.asI32=1},(Value){.asI32=-2},(Value){.asI32=3},(Value){.asI8=42}}
    }
    var5=tmp0;
  }
  {// Log: Log[DEFAULT]{VarExpression{5}}
    logValue(DEFAULT,false,TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT),var5);
  }
  {// Log: Log[DEFAULT]{GetField{VarExpression{5}.type}}
    // TODO GetField is currently not supported
    logValue(DEFAULT,false,TYPE_SIG_TYPE,);
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:(((int32[])[])?)[]}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,((Value){.asType=TYPE_SIG_ARRAY|(null<<TYPE_CONTENT_SHIFT)}));
  }
  Value var6;// (Type:uint64)
  {// Initialize: GetField{VarExpression{5}.length}
    // TODO GetField is currently not supported
    var6=;
  }
  {// Log: Log[DEFAULT]{VarExpression{6}}
    logValue(DEFAULT,false,TYPE_SIG_U64,var6);
  }
  {// Assign: Assignment:{GetIndex{VarExpression{5}[ValueExpression{0}]}=ValueExpression{123456789}}
    // TODO GetIndex is currently not supported
     = ((Value){.asI32=123456789});
  }
  Value var7 [2];// (Type:any)
  {// Initialize: TypeCast{Type:any:GetIndex{VarExpression{5}[ValueExpression{1}]}}
    // TODO GetIndex is currently not supported
    var7=/*TODO typeCast:Type:any*/;
  }
  {// Log: Log[DEFAULT]{VarExpression{7}}
    logValue(DEFAULT,false,TYPE_SIG_ANY,var7);
  }
  {// Log: Log[DEFAULT]{ValueExpression{{2112454933,2,3}}}
    logValue(DEFAULT,false,TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT),const_y);
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:"none"}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,((Value){.asType=TYPE_SIG_NONE}));
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:"empty"[]}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,((Value){.asType=TYPE_SIG_ARRAY|(null<<TYPE_CONTENT_SHIFT)}));
  }
  Value var8 [2];// (Type:int32?)
  {// Initialize: TypeCast{Type:int32?:ValueExpression{none}}
    var8=/*TODO typeCast:Type:int32?*/((Value){.asBool=false/*none*/});
  }
  {// Log: Log[DEFAULT]{VarExpression{8}}
    logValue(DEFAULT,false,TYPE_SIG_OPTIONAL|(0<<TYPE_CONTENT_SHIFT),var8);
  }
  {// Log: Log[DEFAULT]{IfExpr{TypeCast{Type:bool:VarExpression{8}}?TypeCast{Type:any:GetField{VarExpression{8}.value}}:TypeCast{Type:any:ValueExpression{"empty"}}}}
    // TODO GetField is currently not supported
    Value tmp0 [2];
    {
      if(/*TODO typeCast:Type:bool*/var8){
        tmp0=/*TODO typeCast:Type:any*/;
      }else{
        Value tmp1 [2];
        {
          tmp1={(Value){.asU64=0xc000000000000005},(Value){.asPtr=(tmp+0)}}
          // data={(Value){.raw8={0x65,0x6d,0x70,0x74,0x79,0x0,0x0,0x0}}}
        }
        tmp0=/*TODO typeCast:Type:any*/tmp1;
      }
    }
    logValue(DEFAULT,false,TYPE_SIG_ANY,tmp0);
  }
  return NULL;
}

// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount,Value** argData){
  // var0:(*((Value[2])(args+0)))
  // var1:(*(args+2))
  // Native
  return NULL;
}

// declarations of all used type Signatures
Type[] typeData={TYPE_SIG_I32,TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT),TYPE_SIG_ARRAY|(1<<TYPE_CONTENT_SHIFT),TYPE_SIG_OPTIONAL|(2<<TYPE_CONTENT_SHIFT),TYPE_SIG_EMPTY};
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* run(void* initState){
    Procedure f=*((Procedure*)initState);
    initState+=sizeof(Procedure);
    size_t argCount=*((size_t*)initState);
    initState+=sizeof(size_t);
    Value* argData=*((Value**)initState);
    initState+=sizeof(Value*);
    Value* argCache=malloc(MAX_ARG_SIZE*sizeof(Value));
    if(argCache==NULL){
        return (void*)-1;
    }
    // initArgs
    memcpy(argCache,initState,argCount*sizeof(Value));
    do{
        f=(Procedure)f(argCache,&argCount,&argData);
    }while(f!=NULL);
    return (void*)0;
}

// main method of the C representation: 
//   transforms the input arguments and starts the run function on this thread
int main(int argc,char** argv){
  void* init=malloc(sizeof(Procedure)+sizeof(size_t)+sizeof(Value*)+((1+2*(argc-1))*sizeof(Value)));
  size_t off=0;
  *((Procedure*)init)=&proc_start;
  off+=sizeof(Procedure);
  *((size_t*)(init+off))=1;
  off+=sizeof(size_t);
  Value* argData=malloc(MAX_ARG_SIZE*sizeof(Value));
  if(argData==NULL){
    return -1;
  }
  *((Value**)(init+off))=argData;
  off+=sizeof(Value*);
  // prepare program Arguments
  // !!! currently only UTF-8 encoding is supported !!!
  int l;
  int k0=0;
  for(int i=1;i<argc;i++){
    int l=strlen(argv[i]);
    *((Value*)(init+off))=(Value){.asU64=LEN_MASK_LOCAL|l};
    off+=sizeof(Value);
    *((Value*)(init+off))=(Value){.asPtr=argData+k0};
    off+=sizeof(Value);
    for(int j=0,k=0;j+k<l;j++){
      if(j==8){
        j=0;
        k++;
        k0++;
      }
      argData[k0].raw8[j]=argv[i][j+k];
    }
    k0++;
  }
  run(init);
  return 0;
}
