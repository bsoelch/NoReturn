package bsoelch.noret;

public class CompileToC {
    //TODO compile code representation to C

    //ProcCall:
    // 8-byte: fktPtr
    // 8-byte: argsLen
    // N-byte: args

    //procedure:
    //ProcCall* proc_NAME(char[] argData)

    //fixed size values:
    // bool,int8,uint8,...,uint64 -> primitive
    // float32,float64 -> primitive, ?depending on system
    // struct->{elt1,elt2,...,eltN}
    // proc->{ptr,args}
    // type->?
    //dynamic size values:
    // string->{len,val_ptr}
    // array->{typeID,depth,len,val_ptr}
    // any->{typeID,val_ptr}
    // opt->{typeID,val_ptr}
    // ref->{typeID,val_ptr}


}
