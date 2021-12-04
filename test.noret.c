// Auto generated code from NoRet compiler

#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <inttypes.h>
#include <assert.h>

#define MAX_ARG_SIZE       0x1
#define ARG_DATA_INIT_SIZE 0x1000

// Type Definitions
typedef uint64_t Type;
#define TYPE_SIG_MASK       0xff
#define TYPE_SIG_BOOL       0x0
#define TYPE_SIG_C8         0x1
#define TYPE_SIG_I8         0x2
#define TYPE_SIG_U8         0x3
#define TYPE_SIG_C16        0x4
#define TYPE_SIG_I16        0x5
#define TYPE_SIG_U16        0x6
#define TYPE_SIG_C32        0x7
#define TYPE_SIG_F32        0x8
#define TYPE_SIG_I32        0x9
#define TYPE_SIG_U32        0xa
#define TYPE_SIG_F64        0xb
#define TYPE_SIG_I64        0xc
#define TYPE_SIG_U64        0xd
#define TYPE_SIG_NONE       0xe
#define TYPE_SIG_TYPE       0xf
#define TYPE_SIG_STRING8    0x10
#define TYPE_SIG_STRING16   0x11
#define TYPE_SIG_STRING32   0x12
#define TYPE_SIG_ARRAY      0x13
#define TYPE_SIG_TUPLE      0x14
#define TYPE_SIG_REFERENCE  0x15
#define TYPE_SIG_PROC       0x16
#define TYPE_SIG_OPTIONAL   0x17
#define TYPE_SIG_ANY        0x18
#define TYPE_SIG_UNION      0x19
#define TYPE_SIG_STRUCT     0x1a
#define TYPE_CONTENT_SHIFT  8
#define TYPE_CONTENT_MASK   0xffffffff
#define TYPE_COUNT_SHIFT    40
#define TYPE_COUNT_MASK     0xffff
// Type data for all contained Types
Type typeData [];

// value-block type
typedef union ValueImpl Value;
// procedure type (the return-type is void* instead of Procedure* to avoid a recursive type definition)
typedef void*(*Procedure)(Value*,Value*);
// float types
typedef float float32_t;
typedef double float64_t;
// value-block definition
union ValueImpl{
  bool       asBool;
  uint8_t    asC8;
  int8_t     asI8;
  uint8_t    asU8;
  uint16_t   asC16;
  int16_t    asI16;
  uint16_t   asU16;
  uint32_t   asC32;
  float32_t  asF32;
  int32_t    asI32;
  uint32_t   asU32;
  float64_t  asF64;
  int64_t    asI64;
  uint64_t   asU64;
  Type       asType;
  Procedure  asProc;
  Value*     asPtr;
  uint8_t    raw8[8];
  uint16_t   raw16[4];
  uint32_t   raw32[2];
  uint64_t   raw64[1];
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
void printType(const Type type,FILE* log,bool recursive){
  switch(type&TYPE_SIG_MASK){
    case TYPE_SIG_BOOL:
      fputs("Type:bool",log);
      break;
    case TYPE_SIG_C8:
      fputs("Type:char8",log);
      break;
    case TYPE_SIG_I8:
      fputs("Type:int8",log);
      break;
    case TYPE_SIG_U8:
      fputs("Type:uint8",log);
      break;
    case TYPE_SIG_C16:
      fputs("Type:char16",log);
      break;
    case TYPE_SIG_I16:
      fputs("Type:int16",log);
      break;
    case TYPE_SIG_U16:
      fputs("Type:uint16",log);
      break;
    case TYPE_SIG_C32:
      fputs("Type:char32",log);
      break;
    case TYPE_SIG_F32:
      fputs("Type:float32",log);
      break;
    case TYPE_SIG_I32:
      fputs("Type:int32",log);
      break;
    case TYPE_SIG_U32:
      fputs("Type:uint32",log);
      break;
    case TYPE_SIG_F64:
      fputs("Type:float64",log);
      break;
    case TYPE_SIG_I64:
      fputs("Type:int64",log);
      break;
    case TYPE_SIG_U64:
      fputs("Type:uint64",log);
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
      if(recursive){fputs("(",log);}
      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log,true);
      fputs(recursive?"?)":"?",log);
      break;
    case TYPE_SIG_REFERENCE:
      fputs(recursive?"(@":"@",log);
      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log,true);
      if(recursive){fputs(")",log);}
      break;
    case TYPE_SIG_ARRAY:
      if(recursive){fputs("(",log);}
      printType(typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK],log,true);
      fputs(recursive?"[])":"[]",log);
      break;
    case TYPE_SIG_TUPLE:
    case TYPE_SIG_STRUCT:
    case TYPE_SIG_UNION:
    case TYPE_SIG_PROC:
      assert(false && " unimplemented ");
      break;
    default:
      assert(false && " unimplemented ");
      break;
  }
}
// recursive printing of values
void printValue(FILE* log,Type type,const void* value){
  Value* data;
  Type valType;
  switch(type&TYPE_SIG_MASK){
    case TYPE_SIG_BOOL:
      fprintf(log,"%s",(*((bool*)value))?"true":"false");
      break;
    case TYPE_SIG_C8:
      fprintf(log,"'%c'",(*((uint8_t*)value)));
      break;
    case TYPE_SIG_I8:
      fprintf(log,"%"PRIi8,(*((int8_t*)value)));
      break;
    case TYPE_SIG_U8:
      fprintf(log,"%"PRIu8,(*((uint8_t*)value)));
      break;
    case TYPE_SIG_C16:
      fprintf(log,"%"PRIu16,(*((uint16_t*)value)));
      break;
    case TYPE_SIG_I16:
      fprintf(log,"%"PRIi16,(*((int16_t*)value)));
      break;
    case TYPE_SIG_U16:
      fprintf(log,"%"PRIu16,(*((uint16_t*)value)));
      break;
    case TYPE_SIG_C32:
      fprintf(log,"%"PRIu32,(*((uint32_t*)value)));
      break;
    case TYPE_SIG_F32:
      fprintf(log,"%f",(*((float32_t*)value)));
      break;
    case TYPE_SIG_I32:
      fprintf(log,"%"PRIi32,(*((int32_t*)value)));
      break;
    case TYPE_SIG_U32:
      fprintf(log,"%"PRIu32,(*((uint32_t*)value)));
      break;
    case TYPE_SIG_F64:
      fprintf(log,"%f",(*((float64_t*)value)));
      break;
    case TYPE_SIG_I64:
      fprintf(log,"%"PRIi64,(*((int64_t*)value)));
      break;
    case TYPE_SIG_U64:
      fprintf(log,"%"PRIu64,(*((uint64_t*)value)));
      break;
    case TYPE_SIG_NONE:
      fputs("\"none\"",log);
      break;
    case TYPE_SIG_STRING8:
      data=*((Value**)value);/*value->asPtr*/
      fprintf(log,"%.*s",(int)(data[2].asU64),(char*)(data+3/*header*/+data[0].asU64/*off*/));
      break;
    case TYPE_SIG_STRING16:
    case TYPE_SIG_STRING32:
      assert(false && "unimplemented");
      break;
    case TYPE_SIG_TYPE:
      printType(*((Type*)value),log,false);
      break;
    case TYPE_SIG_ANY:
      valType=*((Type*)value);
      if((valType&TYPE_SIG_MASK)<23){
        printValue(log,valType,value+sizeof(Value)/*content*/);
      }else{
        printValue(log,valType,*((Value**)(value+sizeof(Value)))/*value*/);
      }
      break;
    case TYPE_SIG_OPTIONAL:
      if(*((bool*)value)){
        fputs("Optional{",log);
        valType=typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK];
        if((valType&TYPE_SIG_MASK)<23){
          printValue(log,valType,value+sizeof(Value)/*value*/);
        }else{
          printValue(log,valType,*((Value**)(value+sizeof(Value)))/*value*/);
        }
        fputs("}",log);
      }else{
        fputs("Optional{}",log);
      }
      break;
    case TYPE_SIG_REFERENCE:
      valType=typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK];
      printValue(log,valType,*((Value**)value)/*content*/);
      break;
    case TYPE_SIG_ARRAY:
      data=*((Value**)value);/*value->asPtr*/
      valType=typeData[(type>>TYPE_CONTENT_SHIFT)&TYPE_CONTENT_MASK];
      size_t w;
      void* min;
      void* max;
      if((valType&TYPE_SIG_MASK)<4){
        w=1;
      }else if((valType&TYPE_SIG_MASK)<7){
        w=2;
      }else if((valType&TYPE_SIG_MASK)<11){
        w=4;
      }else if((valType&TYPE_SIG_MASK)<14){
        w=8;
      }else if((valType&TYPE_SIG_MASK)<23){
        w=sizeof(Value);
      }else{
        w=2*sizeof(Value);
      }
      min=data+3/*header size*/+data[0].asU64/*off*/;
      max=min+w*(data[2].asU64)/*len*/;
      fputs("{",log);
      for(void* p=min;p<max;p+=w){
        if(p>min){fputs(",",log);}
        printValue(log,valType,p/*element*/);
      }
      fputs("}",log);
      break;
    case TYPE_SIG_TUPLE:
    case TYPE_SIG_UNION:
    case TYPE_SIG_STRUCT:
    case TYPE_SIG_PROC:
      assert(false && " unimplemented ");
      break;
    default:
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
      if(!append){
        fputs("Debug: ",log);
      }
      break;
    case INFO:
      log=log_INFO;
      if(!append){
        fputs("Info: ",log);
      }
      break;
  }
  printValue(log,type,value);
  prevType=logType;
}

// read an element from an Array
Value* getElement(Value* array,uint64_t index,uint64_t width){
  if(index<array[2].asU64){
    return (array+3)+(array[0].asU64+index)*width;
  }else{
    fprintf(stderr,"array index out of range:%"PRIu64" length:%"PRIu64"\n",index,array[2].asU64);
    exit(2);
  }
}
// read a raw-element with width byteWidth from an Array
void* getRawElement(Value* array,uint64_t index,int byteWidth){
  if(index<array[2].asU64){
    return ((void*)(array+3))+(array[0].asU64+index)*byteWidth;
  }else{
    fprintf(stderr,"array index out of range:%"PRIu64" length:%"PRIu64"\n",index,array[2].asU64);
    exit(2);
  }
}

// const Type:int8 : constant = 42
const Value const_constant []={{.asI8=42}};
// const Type:any : array_test = {{1,2,3},{4,5},{6}}
static Value tmp_const_array__test1[]={{.asU64=0/*off*/},{.asU64=2/*cap*/},{.asU64=3/*len*/},{.raw32={1,2}},{.raw32={3,0}}};
static Value tmp_const_array__test2[]={{.asU64=0/*off*/},{.asU64=1/*cap*/},{.asU64=2/*len*/},{.raw32={4,5}}};
static Value tmp_const_array__test3[]={{.asU64=0/*off*/},{.asU64=1/*cap*/},{.asU64=1/*len*/},{.raw32={6,0}}};
static Value tmp_const_array__test0[]={{.asU64=0/*off*/},{.asU64=3/*cap*/},{.asU64=3/*len*/},{.asPtr=(tmp_const_array__test1)},{.asPtr=(tmp_const_array__test2)},{.asPtr=(tmp_const_array__test3)}};
const Value const_array__test []={{.asType=TYPE_SIG_ARRAY|(1<<TYPE_CONTENT_SHIFT)},{.asPtr=(tmp_const_array__test0)}};
// const Type:string8[] : str_test = {"str1","str2"}
static Value tmp_const_str__test1[]={{.asU64=0/*off*/},{.asU64=1/*cap*/},{.asU64=4/*len*/},{.raw8={0x73,0x74,0x72,0x31,0x0,0x0,0x0,0x0}}};
static Value tmp_const_str__test2[]={{.asU64=0/*off*/},{.asU64=1/*cap*/},{.asU64=4/*len*/},{.raw8={0x73,0x74,0x72,0x32,0x0,0x0,0x0,0x0}}};
static Value tmp_const_str__test0[]={{.asU64=0/*off*/},{.asU64=2/*cap*/},{.asU64=2/*len*/},{.asPtr=(tmp_const_str__test1)},{.asPtr=(tmp_const_str__test2)}};
const Value const_str__test []={{.asPtr=(tmp_const_str__test0)}};
// const Type:int32[] : y = {2112454933,2,3}
static Value tmp_const_y0[]={{.asU64=0/*off*/},{.asU64=2/*cap*/},{.asU64=3/*len*/},{.raw32={2112454933,2}},{.raw32={3,0}}};
const Value const_y []={{.asPtr=(tmp_const_y0)}};
// const Type:(((int32[])[])?)[] : type_sig_test = {}
static Value tmp_const_type__sig__test0[]={{.asU64=0/*off*/},{.asU64=0/*cap*/},{.asU64=0/*len*/}};
const Value const_type__sig__test []={{.asPtr=(tmp_const_type__sig__test0)}};

// start(Type:string8[])
void* proc_start(Value* argsIn,Value* argsOut);
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* noRet_run(void* initState);

// start(Type:string8[])
void* proc_start(Value* argsIn,Value* argsOut){
  // var0:(argsIn+0)
  {// Log: Log[DEBUG]{VarExpression{0}}
    logValue(DEBUG,false,TYPE_SIG_ARRAY|(2<<TYPE_CONTENT_SHIFT),(argsIn+0));
  }
  {// Log: Log[DEBUG]{ValueExpression{"args[0]="}}
    Value tmp0 [1];
    {
      Value* tmp1=malloc((4)*sizeof(Value));
      tmp1[0]=(Value){.asU64=0}; /*off*/
      tmp1[1]=(Value){.asU64=1}; /*cap*/
      tmp1[2]=(Value){.asU64=8}; /*len*/
      memcpy(tmp1+3,(Value[]){(Value){.raw8={0x61,0x72,0x67,0x73,0x5b,0x30,0x5d,0x3d}}},(1)*sizeof(Value));
      memcpy(tmp0,(Value[]){(Value){.asPtr=(tmp1)}},1*sizeof(Value));
    }
    logValue(DEBUG,false,TYPE_SIG_STRING8,tmp0);
  }
  {// Log: Log[_DEBUG]{IfExpr{BinOp{GetField{VarExpression{0}.length} GT ValueExpression{0}}?GetIndex{VarExpression{0}[ValueExpression{0}]}:ValueExpression{"No Arguments Provided"}}}
    Value tmp0 [1];
    {
      if(((bool)(((argsIn+0)[0].asPtr+2)[0].asU64>((int32_t)(0))))){
        memcpy(tmp0,getElement((argsIn+0)->asPtr,((int32_t)(0)),1),1*sizeof(Value));
      }else{
        Value tmp1 [1];
        {
          Value* tmp2=malloc((6)*sizeof(Value));
          tmp2[0]=(Value){.asU64=0}; /*off*/
          tmp2[1]=(Value){.asU64=3}; /*cap*/
          tmp2[2]=(Value){.asU64=21}; /*len*/
          memcpy(tmp2+3,(Value[]){(Value){.raw8={0x4e,0x6f,0x20,0x41,0x72,0x67,0x75,0x6d}},(Value){.raw8={0x65,0x6e,0x74,0x73,0x20,0x50,0x72,0x6f}},(Value){.raw8={0x76,0x69,0x64,0x65,0x64,0x0,0x0,0x0}}},(3)*sizeof(Value));
          memcpy(tmp1,(Value[]){(Value){.asPtr=(tmp2)}},1*sizeof(Value));
        }
        memcpy(tmp0,tmp1,1*sizeof(Value));
      }
    }
    logValue(DEBUG,true,TYPE_SIG_STRING8,tmp0);
  }
  Value var1 [1];// (Type:string8)
  {// Initialize: ValueExpression{"UTF8-String"}
    Value tmp0 [1];
    {
      Value* tmp1=malloc((5)*sizeof(Value));
      tmp1[0]=(Value){.asU64=0}; /*off*/
      tmp1[1]=(Value){.asU64=2}; /*cap*/
      tmp1[2]=(Value){.asU64=11}; /*len*/
      memcpy(tmp1+3,(Value[]){(Value){.raw8={0x55,0x54,0x46,0x38,0x2d,0x53,0x74,0x72}},(Value){.raw8={0x69,0x6e,0x67,0x0,0x0,0x0,0x0,0x0}}},(2)*sizeof(Value));
      memcpy(tmp0,(Value[]){(Value){.asPtr=(tmp1)}},1*sizeof(Value));
    }
    memcpy(var1,tmp0,1*sizeof(Value));
  }
  Value var2 [1];// (Type:string16)
  {// Initialize: ValueExpression{"UTF16-String"}
    Value tmp0 [1];
    {
      Value* tmp1=malloc((6)*sizeof(Value));
      tmp1[0]=(Value){.asU64=0}; /*off*/
      tmp1[1]=(Value){.asU64=3}; /*cap*/
      tmp1[2]=(Value){.asU64=12}; /*len*/
      memcpy(tmp1+3,(Value[]){(Value){.raw16={0x55,0x54,0x46,0x31}},(Value){.raw16={0x36,0x2d,0x53,0x74}},(Value){.raw16={0x72,0x69,0x6e,0x67}}},(3)*sizeof(Value));
      memcpy(tmp0,(Value[]){(Value){.asPtr=(tmp1)}},1*sizeof(Value));
    }
    memcpy(var2,tmp0,1*sizeof(Value));
  }
  Value var3 [1];// (Type:string32)
  {// Initialize: ValueExpression{"UTF32-String"}
    Value tmp0 [1];
    {
      Value* tmp1=malloc((9)*sizeof(Value));
      tmp1[0]=(Value){.asU64=0}; /*off*/
      tmp1[1]=(Value){.asU64=6}; /*cap*/
      tmp1[2]=(Value){.asU64=12}; /*len*/
      memcpy(tmp1+3,(Value[]){(Value){.raw32={0x55,0x54}},(Value){.raw32={0x46,0x33}},(Value){.raw32={0x32,0x2d}},(Value){.raw32={0x53,0x74}},(Value){.raw32={0x72,0x69}},(Value){.raw32={0x6e,0x67}}},(6)*sizeof(Value));
      memcpy(tmp0,(Value[]){(Value){.asPtr=(tmp1)}},1*sizeof(Value));
    }
    memcpy(var3,tmp0,1*sizeof(Value));
  }
  {// Log: Log[DEFAULT]{VarExpression{1}}
    logValue(DEFAULT,false,TYPE_SIG_STRING8,var1);
  }
  {// Log: Log[DEFAULT]{GetIndex{VarExpression{1}[ValueExpression{3}]}}
    logValue(DEFAULT,false,TYPE_SIG_C8,((Value[]){(Value){.asC8=*((uint8_t*)getRawElement(var1->asPtr,((int32_t)(3)),1))}}));
  }
  {// Log: Log[DEFAULT]{GetField{VarExpression{1}.length}}
    logValue(DEFAULT,false,TYPE_SIG_U64,(var1[0].asPtr+2));
  }
  Value var4 [2];// (Type:any)
  {// Initialize: TypeCast{Type:any:ValueExpression{3}}
    memcpy(var4,((Value[]){(Value){.asType=TYPE_SIG_I32},(Value){.asI32=((int32_t)(3))}}),2*sizeof(Value));
  }
  {// Log: Log[DEFAULT]{GetField{VarExpression{4}.type}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,var4);
  }
  Value var5 [1];// (Type:int32)
  {// Initialize: BinOp{ValueExpression{1} PLUS TypeCast{Type:int32:GetField{VarExpression{0}.length}}}
    memcpy(var5,((Value[]){(Value){.asI32=((int32_t)(((int32_t)(1))+((int32_t)(((argsIn+0)[0].asPtr+2)[0].asU64))))}}),1*sizeof(Value));
  }
  Value var6 [1];// (Type:int32)
  {// Initialize: BinOp{BinOp{VarExpression{5} MULT VarExpression{5}} MINUS BinOp{VarExpression{5} INT_DIV ValueExpression{2}}}
    memcpy(var6,((Value[]){(Value){.asI32=((int32_t)(((int32_t)(var5[0].asI32*var5[0].asI32))-((int32_t)(var5[0].asI32/((int32_t)(2))))))}}),1*sizeof(Value));
  }
  {// Log: Log[DEFAULT]{VarExpression{6}}
    logValue(DEFAULT,false,TYPE_SIG_I32,var6);
  }
  {// Log: Log[DEFAULT]{ValueExpression{{2112454933,2,3}}}
    logValue(DEFAULT,false,TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT),const_y);
  }
  {// Log: Log[DEFAULT]{ValueExpression{2112454933}}
    logValue(DEFAULT,false,TYPE_SIG_I32,((Value[]){(Value){.asI32=2112454933}}));
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:int32[]}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,((Value[]){(Value){.asType=TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT)}}));
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:(((int32[])[])?)[]}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,((Value[]){(Value){.asType=TYPE_SIG_ARRAY|(4<<TYPE_CONTENT_SHIFT)}}));
  }
  Value var7 [1];// (Type:uint64)
  {// Initialize: ValueExpression{3}
    memcpy(var7,((Value[]){(Value){.asU64=3}}),1*sizeof(Value));
  }
  {// Log: Log[DEFAULT]{VarExpression{7}}
    logValue(DEFAULT,false,TYPE_SIG_U64,var7);
  }
  Value var8 [1];// (Type:int32[])
  {// Initialize: ValueExpression{{1,2,3,42}}
    Value tmp0 [1];
    {
      Value* tmp1=malloc((5)*sizeof(Value));
      tmp1[0]=(Value){.asU64=0}; /*off*/
      tmp1[1]=(Value){.asU64=2}; /*cap*/
      tmp1[2]=(Value){.asU64=4}; /*len*/
      memcpy(tmp1+3,(Value[]){(Value){.raw32={1,2}},(Value){.raw32={3,42}}},(2)*sizeof(Value));
      memcpy(tmp0,(Value[]){(Value){.asPtr=(tmp1)}},1*sizeof(Value));
    }
    memcpy(var8,tmp0,1*sizeof(Value));
  }
  {// Assign: Assignment:{GetIndex{VarExpression{8}[ValueExpression{0}]}=ValueExpression{123456789}}
    *((int32_t*)getRawElement(var8->asPtr,((int32_t)(0)),4))=((int32_t)(123456789));
  }
  {// Log: Log[DEFAULT]{GetIndex{VarExpression{8}[ValueExpression{0}]}}
    logValue(DEFAULT,false,TYPE_SIG_I32,((Value[]){(Value){.asI32=*((int32_t*)getRawElement(var8->asPtr,((int32_t)(0)),4))}}));
  }
  {// Log: Log[DEFAULT]{VarExpression{8}}
    logValue(DEFAULT,false,TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT),var8);
  }
  {// Log: Log[DEFAULT]{ValueExpression{Type:"none"}}
    logValue(DEFAULT,false,TYPE_SIG_TYPE,((Value[]){(Value){.asType=TYPE_SIG_NONE}}));
  }
  Value var9 [2];// (Type:int32?)
  {// Initialize: TypeCast{Type:int32?:ValueExpression{4}}
    memcpy(var9,((Value[]){(Value){.asBool=true},(Value){.asI32=((int32_t)(4))}}),2*sizeof(Value));
  }
  {// Log: Log[DEFAULT]{VarExpression{9}}
    logValue(DEFAULT,false,TYPE_SIG_OPTIONAL|(0<<TYPE_CONTENT_SHIFT),var9);
  }
  {// Log: Log[DEFAULT]{IfExpr{TypeCast{Type:bool:VarExpression{9}}?TypeCast{Type:int32?:GetField{VarExpression{9}.value}}:TypeCast{Type:int32?:ValueExpression{none}}}}
    Value tmp0 [2];
    {
      if(((var9)[0].asBool)){
        memcpy(tmp0,((Value[]){(Value){.asBool=true},(Value){.asI32=((var9)+1)[0].asI32}}),2*sizeof(Value));
      }else{
        memcpy(tmp0,((Value[]){(Value){.asBool=false},(((Value[]){(Value){.asPtr=NULL/*none*/}}))[0]}),2*sizeof(Value));
      }
    }
    logValue(DEFAULT,false,TYPE_SIG_OPTIONAL|(0<<TYPE_CONTENT_SHIFT),tmp0);
  }
  Procedure ret=NULL;
  return ret;
}

// declarations of all used type Signatures
Type typeData []={TYPE_SIG_I32,TYPE_SIG_ARRAY|(0<<TYPE_CONTENT_SHIFT),TYPE_SIG_STRING8,TYPE_SIG_ARRAY|(1<<TYPE_CONTENT_SHIFT),TYPE_SIG_OPTIONAL|(3<<TYPE_CONTENT_SHIFT)};
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* noRet_run(void* initState){
    Procedure f=*((Procedure*)initState);
    initState+=sizeof(Procedure);
    Value* argsI=*((Value**)initState);
    initState+=sizeof(Value*);
    Value* argsO=malloc(MAX_ARG_SIZE*sizeof(Value));
    Value* argsTmp;
    if(argsO==NULL){
      fputs("out of memory\n",stderr);
      exit(1);
    }
    do{
        f=(Procedure)f(argsI,argsO);
        // swap args
        argsTmp=argsI;
        argsI=argsO;
        argsO=argsTmp;
    }while(f!=NULL);
    return (void*)0;
}

// main method of the C representation: 
//   transforms the input arguments and starts the noRet_run function on this thread
int main(int argc,char** argv){
  // check type assumptions
  assert(sizeof(Value)==8);
  assert(sizeof(float32_t)==4);
  assert(sizeof(float64_t)==8);
  // [proc_ptr,args_ptr,arg_data]
  char init[sizeof(Procedure)+2*sizeof(Value*)];
  size_t off=0;
  *((Procedure*)init)=&proc_start;
  off+=sizeof(Procedure);
  Value* initArgs=malloc(MAX_ARG_SIZE*sizeof(Value));
  if(initArgs==NULL){
    fputs("out of memory\n",stderr);
    return 1;
  }
  *((Value**)(init+off))=initArgs;
  off+=sizeof(Value*);
  // prepare program Arguments
  // !!! currently only UTF-8 encoding is supported !!!
  Value* argArray=malloc(((argc-1)+3)*sizeof(Value));
  if(argArray==NULL){
    fputs("out of memory\n",stderr);
    return 1;
  }
  argArray[0] = (Value){.asU64=0 /*off*/};
  argArray[1] = (Value){.asU64=(argc-1) /*cap*/};
  argArray[2] = (Value){.asU64=(argc-1) /*len*/};
  initArgs[0] = (Value){.asPtr=argArray};
  for(int i=1;i<argc;i++){
    int l=strlen(argv[i]);
    Value* tmp=malloc(((l+7)/8+3)*sizeof(Value));
    if(tmp==NULL){
      fputs("out of memory\n",stderr);
      return 1;
    }
    tmp[0] = (Value){.asU64=0/*off*/};
    tmp[1] = (Value){.asU64=(l+7)/8 /*cap*/};
    tmp[2] = (Value){.asU64=l /*len*/};
    argArray[i+2] = (Value){.asPtr=tmp};
    off=3;// reuse off variable
    for(int j=0,k=0;j+k<l;j++){
      if(j==8){
        j=0;
        k+=8;
        off++;
      }
      tmp[off].raw8[j]=argv[i][j+k];
    }
    off++;
  }
  initLogStreams();
  noRet_run(init);
  puts("");// finish last line in stdout
  return 0;
}
