// Auto generated code from NoRet compiler

#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <inttypes.h>
#include <assert.h>

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
Type typeData [];

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
      assert(false&&"unimplemented");
      break;
    case TYPE_SIG_TYPE:
      assert(false&&"unimplemented");
      break;
    case TYPE_SIG_ANY:
       prevType=logType;
       logValue(logType,true,value->asType,value+1/*content*/);
       break;
    case TYPE_SIG_OPTIONAL:
    case TYPE_SIG_REFERENCE:
    case TYPE_SIG_ARRAY:
    case TYPE_SIG_PROC:
    case TYPE_SIG_STRUCT:
      assert(false&&"unimplemented");
      break;
    default:
      assert(false&&"unreachable");
      break;
  }
  prevType=logType;
}

Value constData [];
// data for values used in constants
Value constData []={};

// printFib(Type:int64, Type:int64, Type:int64)
void* proc_printFib(Value* args,size_t* argCount,Value** argData);
// start()
void* proc_start(Value* args,size_t* argCount,Value** argData);
// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount,Value** argData);
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* run(void* initState);

// printFib(Type:int64, Type:int64, Type:int64)
void* proc_printFib(Value* args,size_t* argCount,Value** argData){
  // var0:(*(args+0))
  // var1:(*(args+1))
  // var2:(*(args+2))
  Value var3 [2];// (Type:((Type:int64, Type:int64, Type:int64)=>?)?)
  {// Initialize: IfExpr{BinOp{VarExpression{2} GT ValueExpression{0}}?TypeCast{Type:((Type:int64, Type:int64, Type:int64)=>?)?:this}:TypeCast{Type:((Type:int64, Type:int64, Type:int64)=>?)?:ValueExpression{none}}}
    Value tmp0 [2];
    {
      if((Value){.asBool=(*(args+2)).asI64<((Value){.asI32=0}).asI32}.asBool){
        tmp0=/*TODO typeCast:Type:((Type:int64, Type:int64, Type:int64)=>?)?*/(&proc_printFib);
      }else{
        tmp0=/*TODO typeCast:Type:((Type:int64, Type:int64, Type:int64)=>?)?*/((Value){.asBool=false/*none*/});
      }
    }
    var3=tmp0;
  }
  {// Assign: Assignment:{VarExpression{2}=BinOp{VarExpression{2} MINUS ValueExpression{1}}}
    (*(args+2)) = (Value){.asBool=(*(args+2)).asI64<((Value){.asI32=1}).asI32};
  }
  Value var4;// (Type:int64)
  {// Initialize: BinOp{VarExpression{0} PLUS VarExpression{1}}
    var4=(Value){.asI64=(*(args+0)).asI64+(*(args+1)).asI64};
  }
  {// Log: Log[DEFAULT]{VarExpression{0}}
    logValue(DEFAULT,false,TYPE_SIG_I64,&(*(args+0)));
  }
  return NULL;
}

// start()
void* proc_start(Value* args,size_t* argCount,Value** argData){
  Value var0;// (Type:int64)
  {// Initialize: TypeCast{Type:int64:ValueExpression{0}}
    var0=/*TODO typeCast:Type:int64*/((Value){.asI32=0});
  }
  Value var1;// (Type:int64)
  {// Initialize: TypeCast{Type:int64:ValueExpression{1}}
    var1=/*TODO typeCast:Type:int64*/((Value){.asI32=1});
  }
  Value var2;// (Type:int64)
  {// Initialize: TypeCast{Type:int64:ValueExpression{75}}
    var2=/*TODO typeCast:Type:int64*/((Value){.asI32=75});
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
Type typeData []={};
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
int main(){
  void* init=malloc(sizeof(Procedure)+sizeof(size_t));
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
  run(init);
  return 0;
}
