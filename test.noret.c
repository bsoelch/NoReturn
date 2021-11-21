// Auto generated code from NoRet compiler

#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <inttypes.h>

#define MAX_ARG_SIZE 1024;

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
  bool asBool;
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
  uint64_t raw;
};

typedef void*(*Procedure)(Value*,size_t*);

// const Type:int8 : constant = 42
Value const_constant []={{.asI8=42}};
// const Type:int32[] : y = {2112454933,2,3}
Value const_y []={{.asU64=3},{.asI32=2112454933},{.asI32=2},{.asI32=3}};
// print(Type:any)
void* proc_print(Value* args,size_t* argCount);
// start()
void* proc_start(Value* args,size_t* argCount);
// readLine(Generic: $a, Type:(Type:string, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount);

// print(Type:any)
void* proc_print(Value* args,size_t* argCount){
return NULL;
}

// start()
void* proc_start(Value* args,size_t* argCount){
return NULL;
}

// readLine(Generic: $a, Type:(Type:string, Generic: $a)=>?)
void* proc_readLine(Value* args,size_t* argCount){
return NULL;
}

//  main procedure handling function (written in a way to allow easy usage of pthreads)
void* run(void* initState){
    Procedure f=*((Procedure*)initState);
    initState+=sizeof(Procedure);
    size_t argCount=*((size_t*)initState);
    initState+=sizeof(size_t);
    Value* argCache=malloc(MAX_ARG_SIZE*sizeof(Value));
    if(argCache==NULL){
        return (void*)-1;
    }
    //initArgs
    memcpy(argCache,initState,argCount*sizeof(Value));
    do{
        f=(Procedure)f(argCache,&argCount);
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
	run(init);
	return 0;
}
