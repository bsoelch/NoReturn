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
// const Type:(int32[])[] : array_test = {{1,2,3},{4,5},{6}}
const Value const_array__test []={{.asU64=0x3},{.asU64=0x8000000000000003},{.asPtr=(constData+0)},{.asU64=0x8000000000000002},{.asPtr=(constData+3)},{.asU64=0x8000000000000001},{.asPtr=(constData+5)}};
// const Type:int32[] : y = {2112454933,2,3}
const Value const_y []={{.asU64=0x3},{.asI32=2112454933},{.asI32=2},{.asI32=3}};
// data for values used in constants
Value constData []={{.asI32=1},{.asI32=2},{.asI32=3},{.asI32=4},{.asI32=5},{.asI32=6}};

// start(Type:string8[])
void* proc_start(Value* args,size_t* argCount,Value** argData);
// readLine(Generic: $a, Type:(Type:string8, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount,Value** argData);
//  main procedure handling function (written in a way that allows easy usage in pthreads)
void* run(void* initState);

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
