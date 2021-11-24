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

typedef enum{
  EMPTY=0,
  BOOL,
  I8,
  U8,
  I16,
  U16,
  I32,
  U32,
  I64,
  U64,
  F32,
  F64,
  STRING,
  TYPE,
  ANY=0xf,
}Type;

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

typedef void*(*Procedure)(Value*,size_t*,Value**);

Value constData [];
// const Type:int8 : constant = 42
const Value const_constant []={{.asI8=42}};
// const Type:any : array_test = {{1,2,3},{4,5},{6}}
const Value const_array__test []={{.asType=0/*Type:(int32[])[]*/},{.asPtr=(constData+0)},};
// const Type:string8[] : str_test = {"str1","str2"}
const Value const_str__test []={{.asU64=0x2},{.asU64=0x8000000000000004},{.asPtr=(constData+13)},{.asU64=0x8000000000000004},{.asPtr=(constData+14)}};
// const Type:int32[] : y = {2112454933,2,3}
const Value const_y []={{.asU64=0x3},{.asI32=2112454933},{.asI32=2},{.asI32=3}};
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
    log_DEFAULT((*(args+0)));
  }
  Value var1 [2];// (Type:((Type:int8)=>?)?)
  {// Initialize: IfExpr{BinOp{VarExpression{0} LT ValueExpression{5}}?TypeCast{Type:((Type:int8)=>?)?:this}:TypeCast{Type:((Type:int8)=>?)?:ValueExpression{none}}}
    Value tmp0;
    {
      tmp0={.asI32=5}
      // data={}
    }
    Value tmp1;
    {
      tmp1=(Value){.asBool=false/*none*/}
      // data={}
    }
    var1=((Value){.asBool=(*(args+0)).asI8<tmp0.asI32}.asBool?/*TODO typeCast:Type:((Type:int8)=>?)?*/(&proc_loop):/*TODO typeCast:Type:((Type:int8)=>?)?*/tmp1);
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
    // GetField is currently not supported
    Value tmp0;
    {
      tmp0={.asI32=1}
      // data={}
    }
    var4=(Value){.asI32=tmp0.asI32+/*TODO typeCast:Type:int32*/.asI32};
  }
  Value var5 [2];// (Type:any)
  {// Initialize: TypeCast{Type:any:ValueExpression{{1,-2,3,42}}}
    Value tmp0 [2];
    {
      tmp0={(Value){.asU64=0xc000000000000004},(Value){.asPtr=(tmp+0)}}
      // data={{.asI32=1},{.asI32=-2},{.asU32=3},{.asI8=42}}
    }
    var5=/*TODO typeCast:Type:any*/tmp0;
  }
  {// Log: Log[DEFAULT]{VarExpression{5}}
    log_DEFAULT(var5);
  }
  Value var6;// (Type:uint64)
  {// Initialize: GetField{TypeCast{Type:int32[]:VarExpression{5}}.length}
    // GetField is currently not supported
    var6=;
  }
  {// Log: Log[DEFAULT]{VarExpression{6}}
    log_DEFAULT(var6);
  }
  {// Assign: Assignment:{GetIndex{TypeCast{Type:int32[]:VarExpression{5}}[ValueExpression{0}]}=ValueExpression{123456789}}
  }
  Value var7 [2];// (Type:any)
  {// Initialize: TypeCast{Type:any:GetIndex{TypeCast{Type:int32[]:VarExpression{5}}[ValueExpression{1}]}}
    // GetIndex is currently not supported
    var7=/*TODO typeCast:Type:any*/;
  }
  {// Log: Log[DEFAULT]{VarExpression{7}}
    log_DEFAULT(var7);
  }
  {// Log: Log[DEFAULT]{ValueExpression{{2112454933,2,3}}}
    log_DEFAULT(const_y);
  }
  Value var8 [2];// (Type:int32?)
  {// Initialize: TypeCast{Type:int32?:ValueExpression{none}}
    Value tmp0;
    {
      tmp0=(Value){.asBool=false/*none*/}
      // data={}
    }
    var8=/*TODO typeCast:Type:int32?*/tmp0;
  }
  {// Log: Log[DEFAULT]{VarExpression{8}}
    log_DEFAULT(var8);
  }
  {// Log: Log[DEFAULT]{IfExpr{TypeCast{Type:bool:VarExpression{8}}?TypeCast{Type:any:GetField{VarExpression{8}.value}}:TypeCast{Type:any:ValueExpression{"empty"}}}}
    // GetField is currently not supported
    Value tmp0 [2];
    {
      tmp0={(Value){.asU64=0xc000000000000005},(Value){.asPtr=(tmp+0)}}
      // data={(Value){.raw8={0x65,0x6d,0x70,0x74,0x79,0x0,0x0,0x0}}}
    }
    log_DEFAULT((/*TODO typeCast:Type:bool*/var8.asBool?/*TODO typeCast:Type:any*/:/*TODO typeCast:Type:any*/tmp0));
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
