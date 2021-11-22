// Auto generated code from NoRet compiler

#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <inttypes.h>

#define MAX_ARG_SIZE 0x2000

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
// const Type:int32[] : y = {2112454933,2,3}
const Value const_y []={{.asU64=3},{.asI32=2112454933},{.asI32=2},{.asI32=3}};
// const Type:any[] : hello = {"Hello",1,{1,"World"}}
const Value const_hello []={{.asU64=3},{.asType=0/*Type:string8*/},{.asPtr=(constData+0)},{.asType=0/*Type:int32*/},{.asI32=1},{.asType=0/*Type:any[]*/},{.asPtr=(constData+2)}};
// data for values used in constants
Value constData []={{.asU64=5},{.raw8={0x48,0x65,0x6c,0x6c,0x6f,0x0,0x0,0x0}},{.asU64=2},{.asType=0/*Type:int32*/},{.asI32=1},{.asType=0/*Type:string8*/},{.asPtr=(constData+8)},{.asU64=5},{.raw8={0x57,0x6f,0x72,0x6c,0x64,0x0,0x0,0x0}}};

// print(Type:any)
void* proc_print(Value* args,size_t* argCount,Value** argData);
// start(Type:string8[])
void* proc_start(Value* args,size_t* argCount,Value** argData);
// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount,Value** argData);
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* run(void* initState);

// print(Type:any)
void* proc_print(Value* args,size_t* argCount,Value** argData){
return NULL;
}

// start(Type:string8[])
void* proc_start(Value* args,size_t* argCount,Value** argData){
return NULL;
}

// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount,Value** argData){
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
    //initArgs
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
    *((Value*)(init+off))=(Value){.asU64=l};
    off+=sizeof(Value);
    *((Value*)(init+off))=(Value){.asPtr=argData};
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
