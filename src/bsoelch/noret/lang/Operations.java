package bsoelch.noret.lang;

import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Operations {
    private Operations(){}

    static Value wrap(float f){
        return Value.createPrimitive(Type.Numeric.FLOAT32,f);
    }
    static Value wrap(double d){
        return Value.createPrimitive(Type.Numeric.FLOAT64,d);
    }
    static Value wrap(boolean signed,byte b){
        return Value.createPrimitive(signed?Type.Numeric.INT8:
                Type.Numeric.UINT8,b);
    }
    static Value wrap(boolean signed,short s){
        return Value.createPrimitive(signed?Type.Numeric.INT16:
                Type.Numeric.UINT16,s);
    }
    static Value wrap(boolean signed,int i){
        return Value.createPrimitive(signed?Type.Numeric.INT32:
                Type.Numeric.UINT32,i);
    }
    static Value wrap(boolean signed,long l) {
        return Value.createPrimitive(signed ? Type.Numeric.INT64 :
                Type.Numeric.UINT64, l);
    }


    public static <T> T performBinaryOperation(String opName, Value l, Value r,
                                               BiFunction<Byte,Byte,Function<Boolean,T>> i8Op,
                                               BiFunction<Short,Short,Function<Boolean,T>> i16Op,
                                               BiFunction<Integer,Integer,Function<Boolean,T>> i32Op,
                                               BiFunction<Long,Long,Function<Boolean,T>> i64Op,
                                               BiFunction<Float,Float,T> f32Op,
                                               BiFunction<Double,Double,T> f64Op,
                                               BiFunction<Value,Value,T> valueOp
                                      ){
        T res;
        if(l.type instanceof Type.Numeric&&r.type instanceof Type.Numeric){
            int level= Math.max(((Type.Numeric) l.type).level,((Type.Numeric) r.type).level);
            boolean isFloat=((Type.Numeric) l.type).isFloat||((Type.Numeric)  r.type).isFloat;
            boolean signed=((Type.Numeric) l.type).signed||((Type.Numeric)  r.type).signed;
            if(isFloat){
                switch (level){
                    case 0:
                    case 1:
                    case 2:
                        res=f32Op.apply((Float)((Value.Primitive)
                                        l. castTo(Type.Numeric.FLOAT32)).value,
                                (Float)((Value.Primitive)
                                        r.castTo(Type.Numeric.FLOAT32)).value);
                        break;
                    case 3:
                        res=f64Op.apply((Double) ((Value.Primitive)
                                        l. castTo(Type.Numeric.FLOAT64)).value,
                                (Double) ((Value.Primitive)
                                        r.castTo(Type.Numeric.FLOAT64)).value);
                        break;
                    default:
                        throw new SyntaxError("exceeded maximum number capacity");
                }
            }else{
                switch (level){
                    case 0:
                        res=i8Op.apply((Byte) ((Value.Primitive)
                                        l. castTo(Type.Numeric.INT8)).value,
                                (Byte) ((Value.Primitive)
                                        r.castTo(Type.Numeric.INT8)).value)
                                .apply(signed);
                        break;
                    case 1:
                        res=i16Op.apply((Short) ((Value.Primitive)
                                        l. castTo(Type.Numeric.INT16)).value,
                                (Short) ((Value.Primitive)
                                        r.castTo(Type.Numeric.INT16)).value)
                                .apply(signed);
                        break;
                    case 2:
                        res=i32Op.apply((Integer) ((Value.Primitive)
                                        l. castTo(Type.Numeric.INT32)).value,
                                (Integer) ((Value.Primitive)
                                        r.castTo(Type.Numeric.INT32)).value)
                                .apply(signed);
                        break;
                    case 3:
                        res=i64Op.apply((Long) ((Value.Primitive)
                                        l. castTo(Type.Numeric.INT64)).value,
                                (Long) ((Value.Primitive)
                                        r.castTo(Type.Numeric.INT64)).value)
                                .apply(signed);
                        break;
                    default:
                        throw new SyntaxError("exceeded maximum number capacity");
                }
            }
        }else{
            res=valueOp.apply(l,r);
        }
        if(res==null){
            throw new TypeError("Unsupported types for "+opName+":"+l.type+", "+r.type);
        }else{
            return res;
        }
    }

    public static Value plus(Value l,Value r){
        return performBinaryOperation("+",l,r,
            (b1,b2)->(s)->wrap(s,(byte)(b1+b2)),
            (s1,s2)->(s)->wrap(s,(short) (s1+s2)),
            (i1,i2)->(s)->wrap(s,i1+i2),
            (l1,l2)->(s)->wrap(s,l1+l2),
            (f1,f2)->wrap(f1+f2),
            (d1,d2)->wrap(d1+d2),
            (lVal,rVal)->{
                if(lVal.type== Type.Primitive.STRING){
                    String s1=((Value.StringValue)lVal).stringValue();
                    String s2=((Value.StringValue)rVal.castTo(Type.Primitive.STRING)).stringValue();
                    return Value.createPrimitive(Type.Primitive.STRING,s1+s2);
                }else if(rVal.type== Type.Primitive.STRING){
                    String s1=((Value.StringValue)lVal.castTo(Type.Primitive.STRING)).stringValue();
                    String s2=((Value.StringValue)rVal).stringValue();;
                    return Value.createPrimitive(Type.Primitive.STRING,s1+s2);
                }else if(lVal.type instanceof Type.Array&&rVal.type instanceof Type.Array){
                    Value[] newArray=new Value[((Value.Array)lVal).elements.length+
                            ((Value.Array)rVal).elements.length];
                    System.arraycopy(((Value.Array)lVal).elements,0,
                            newArray,0,((Value.Array)lVal).elements.length);
                    System.arraycopy(((Value.Array)rVal).elements,0,
                            newArray,((Value.Array)lVal).elements.length
                            ,((Value.Array)rVal).elements.length);
                    return new Value.Array(newArray);
                }
                return null;
            }
        );
    }
    public static Value minus(Value l,Value r){
        return performBinaryOperation("-",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(b1-b2)),
                (s1,s2)->(s)->wrap(s,(short) (s1-s2)),
                (i1,i2)->(s)->wrap(s,i1-i2),
                (l1,l2)->(s)->wrap(s,l1-l2),
                (f1,f2)->wrap(f1-f2),
                (d1,d2)->wrap(d1-d2),
                (x1,x2)->null
        );
    }
    public static Value multiply(Value l,Value r){
        return performBinaryOperation("*",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(b1*b2)),
                (s1,s2)->(s)->wrap(s,(short) (s1*s2)),
                (i1,i2)->(s)->wrap(s,i1*i2),
                (l1,l2)->(s)->wrap(s,l1*l2),
                (f1,f2)->wrap(f1*f2),
                (d1,d2)->wrap(d1*d2),
                (x1,x2)->null
        );
    }
    private static double toDouble(long l) {
        //unsigned shift to remove sign, then *2 to balance shift out
        return 2*((double)(l>>>1));
    }

    public static Value div(Value l,Value r){
        return performBinaryOperation("/",l,r,
                (b1,b2)->(s)->wrap(s?((float)b1)/b2:
                        ((float)(b1&0xff))/(b2&0xff)),
                (s1,s2)->(s)->wrap(s?((float)s1)/s2:
                        ((float)(s1&0xffff))/(s2&0xffff)),
                (i1,i2)->(s)->wrap(s?((double)i1)/i2:
                        ((double)(i1&0xffffffffL))/(i2&0xffffffffL)),
                (l1,l2)->(s)->wrap(s?((double)l1)/l2:
                        toDouble(l1) /
                                toDouble(l2)),
                (f1,f2)->wrap(f1/f2),
                (d1,d2)->wrap(d1/d2),
                (x1,x2)->null
        );
    }
    public static Value intDiv(Value l,Value r){
        return performBinaryOperation("/",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(s?b1/b2:
                        ((b1&0xff)/(b2&0xff)))),
                (s1,s2)->(s)->wrap(s,(short) (s?s1/s2:
                        ((s1&0xffff)/(s2&0xffff)))),
                (i1,i2)->(s)->wrap(s,(int)(s?i1/i2:
                        ((i1&0xffffffffL)/(i2&0xffffffffL)))),
                (l1,l2)->(s)->wrap(s,s?l1/l2:Long.divideUnsigned(l1,l2)),
                (f1,f2)->wrap((float)Math.floor(f1/f2)),
                (d1,d2)->wrap(Math.floor(d1/d2)),
                (x1,x2)->null
        );
    }
    public static Value mod(Value l,Value r){
        return performBinaryOperation("%",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(s?b1%b2:
                        ((b1&0xff)%(b2&0xff)))),
                (s1,s2)->(s)->wrap(s,(short) (s?s1%s2:
                        ((s1&0xffff)%(s2&0xffff)))),
                (i1,i2)->(s)->wrap(s,(int)(s?i1%i2:
                        ((i1&0xffffffffL)%(i2&0xffffffffL)))),
                (l1,l2)->(s)->wrap(s,s?l1%l2:
                        Long.remainderUnsigned(l1,l2)),
                (f1,f2)->wrap(f1%f2),
                (d1,d2)->wrap(d1%d2),
                (x1,x2)->null
        );
    }
    public static Value pow(Value l, Value r) {
        return performBinaryOperation("&",l,r,
                (b1,b2)->(s)->wrap(s?Math.pow(b1,b2):Math.pow(b1&0xff,b2&0xff)),
                (s1,s2)->(s)->wrap(s?Math.pow(s1,s2):Math.pow(s1&0xffff,s2&0xffff)),
                (i1,i2)->(s)->wrap(s?Math.pow(i1,i2):Math.pow(i1&0xffffffffL,i1&0xffffffffL)),
                (l1,l2)->(s)->wrap(s?Math.pow(l1,l2):Math.pow(toDouble(l1)
                        , toDouble(l2))),
                (f1,f2)->wrap(Math.pow(f1,f2)),
                (d1,d2)->wrap(Math.pow(d1,d2)),
                (x1,x2)->null
        );
    }


    public static Value and(Value l, Value r) {
        return performBinaryOperation("&",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(b1&b2)),
                (s1,s2)->(s)->wrap(s,(short) (s1&s2)),
                (i1,i2)->(s)->wrap(s,i1&i2),
                (l1,l2)->(s)->wrap(s,l1&l2),
                (f1,f2)->null,//no and/or/xor for floats
                (d1,d2)->null,
                (x1,x2)->null
        );
    }
    public static Value or(Value l, Value r) {
        return performBinaryOperation("|",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(b1|b2)),
                (s1,s2)->(s)->wrap(s,(short) (s1|s2)),
                (i1,i2)->(s)->wrap(s,i1|i2),
                (l1,l2)->(s)->wrap(s,l1|l2),
                (f1,f2)->null,//no and/or/xor for floats
                (d1,d2)->null,
                (x1,x2)->null
        );
    }
    public static Value xor(Value l, Value r) {
        return performBinaryOperation("^",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(b1^b2)),
                (s1,s2)->(s)->wrap(s,(short) (s1^s2)),
                (i1,i2)->(s)->wrap(s,i1^i2),
                (l1,l2)->(s)->wrap(s,l1^l2),
                (f1,f2)->null,//no and/or/xor for floats
                (d1,d2)->null,
                (x1,x2)->null
        );
    }


    public static Value lshift(Value l, Value r) {
        return performBinaryOperation("<<",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(b1<<b2)),
                (s1,s2)->(s)->wrap(s,(short) (s1<<s2)),
                (i1,i2)->(s)->wrap(s,i1<<i2),
                (l1,l2)->(s)->wrap(s,l1<<l2),
                (f1,f2)->null,//no shift for floats
                (d1,d2)->null,
                (x1,x2)-> {
                       if(x1.type instanceof Type.Array){//pushLast
                           Value[] newElements=new Value[((Value.Array)x1).elements.length+1];
                           System.arraycopy(newElements,0,((Value.Array)x1).elements,
                                   0,((Value.Array)x1).elements.length);
                           newElements[((Value.Array)x1).elements.length]=x2;
                           return new Value.Array(newElements);
                       }
                       return null;
                }
        );
    }
    public static Value rshift(Value l, Value r) {
        return performBinaryOperation(">>",l,r,
                (b1,b2)->(s)->wrap(s,(byte)(s?b1>>b2:b1>>>b2)),
                (s1,s2)->(s)->wrap(s,(short) (s?s1>>s2:s1>>>s2)),
                (i1,i2)->(s)->wrap(s,s?i1>>i2:i1>>>i2),
                (l1,l2)->(s)->wrap(s,s?l1>>l2:l1>>>l2),
                (f1,f2)->null,//no shift for floats
                (d1,d2)->null,
                (x1,x2)-> {
                    if(x2.type instanceof Type.Array){//pushFirst
                        Value[] newElements=new Value[((Value.Array)x2).elements.length+1];
                        System.arraycopy(newElements,1,((Value.Array)x2).elements,
                                0,((Value.Array)x2).elements.length);
                        newElements[0]=x1;
                        return new Value.Array(newElements);
                    }
                    return null;
                }
        );
    }

    public static int compare(Value l, Value r) {
        return (Integer) performBinaryOperation("compare",l,r,
                (b1,b2)->(s)->s?b1.compareTo(b2):
                        Integer.compare(b1&0xff,b2&0xff),
                (s1,s2)->(s)->s?s1.compareTo(s2):
                        Integer.compare(s1&0xffff,s2&0xffff),
                (i1,i2)->(s)->s?i1.compareTo(i2):
                        Long.compare(i1&0xffffffffL,i2&0xffffffffL),
                (l1,l2)->(s)->s?l1.compareTo(l2):
                    Long.compareUnsigned(l1,l2),
                (f1,f2)->wrap(f1.compareTo(f2)),
                (d1,d2)->wrap(d1.compareTo(d2)),
                (x1,x2)-> {
                    if(x1.type== Type.Primitive.STRING&&
                        x2.type== Type.Primitive.STRING){
                        return ((Value.StringValue)x1).compareTo(((Value.StringValue) x2));
                    }else{
                        return null;
                    }
                }
        );
    }
    public static boolean eq(Value l, Value r) {
        return performBinaryOperation("compare",l,r,
                (b1,b2)->(s)->b1.equals(b2),
                (s1,s2)->(s)->s1.equals(s2),
                (i1,i2)->(s)->i1.equals(i2),
                (l1,l2)->(s)->l1.equals(l2),
                Float::equals,
                Double::equals,
                Value::equals
        );
    }

    public static boolean asBool(Value value) {
        if(value.type== Type.Primitive.BOOL){
            return (Boolean)((Value.Primitive)value).value;
        }
        throw new UnsupportedOperationException("Unimplemented");
    }

    public static Value unaryOperation(String opName,Value v,
                                BiFunction<Boolean,Byte,Value>    i8Op,
                                BiFunction<Boolean,Short,Value>   i16Op,
                                BiFunction<Boolean,Integer,Value> i32Op,
                                BiFunction<Boolean,Long,Value>    i64Op,
                                Function<Float,Value>   f32Op,
                                Function<Double,Value>  f64Op,
                                Function<Value,Value>   valueOp
                                ){
        Value res;
        if(v.type instanceof Type.Numeric){
            if(((Type.Numeric) v.type).isFloat){
                switch (((Type.Numeric) v.type).level){
                    case 0:
                    case 1:
                    case 2:
                        res=f32Op.apply((Float)((Value.NumericValue)v).value);
                        break;
                    case 3:
                        res=f64Op.apply((Double) ((Value.NumericValue)v).value);
                        break;
                    default:
                        throw new SyntaxError("exceeded maximum number capacity");
                }
            }else{
                boolean s=((Type.Numeric) v.type).signed;
                switch (((Type.Numeric) v.type).level){
                    case 0:
                        res=i8Op.apply(s,(Byte) ((Value.NumericValue)v).value);
                        break;
                    case 1:
                        res=i16Op.apply(s,(Short) ((Value.NumericValue)v).value);
                        break;
                    case 2:
                        res=i32Op.apply(s,(Integer) ((Value.NumericValue)v).value);
                        break;
                    case 3:
                        res=i64Op.apply(s,(Long) ((Value.NumericValue)v).value);
                        break;
                    default:
                        throw new SyntaxError("exceeded maximum number capacity");
                }
            }
        }else{
            res=valueOp.apply(v);
        }
        if(res==null){
            throw new TypeError("Unsupported type for "+opName+ ":"+v.type);
        }else{
            return res;
        }
    }

    public static Value not(Value lVal) {
        if(Type.canAssign(Type.Primitive.BOOL,lVal.type,null)){
            return Value.createPrimitive(Type.Primitive.BOOL,!asBool(lVal));
        }else{
            throw new TypeError("unsupported type for !:"+lVal.type);
        }
    }

    public static Value negate(Value lVal) {
        return unaryOperation("-",lVal,
                (s,b)->s?wrap(true,(byte)-b):wrap(true,(short)-b),
                (s,sV)->s?wrap(true,(short) -sV):wrap(true,-sV),
                (s,i)->s?wrap(true, -i):wrap(true,-(long)i),
                (s,l)->wrap(s, -l),
                f->wrap(-f),
                d->wrap(-d),
                x->null
                );
    }

    public static Value flip(Value lVal) {
        return unaryOperation("~",lVal,
                (s,b)->wrap(s,(byte)~b),
                (s,sV)->wrap(s,(short) ~sV),
                (s,i)->wrap(s, ~i),
                (s,l)->wrap(s, ~l),
                f->null,
                d->null,
                x->null
        );
    }


    public static Type typePlus(Type lType, Type rType) {
        //String + ... => String
        if((lType== Type.Primitive.STRING&&Type.canCast(Type.Primitive.STRING,rType,null))||
              (rType== Type.Primitive.STRING&&Type.canCast(Type.Primitive.STRING,lType,null))){
            return Type.Primitive.STRING;
        }else if(lType instanceof Type.Array&&rType instanceof Type.Array){
            return new Type.Array(Type.commonSupertype(((Type.Array) lType).content,
                    ((Type.Array) rType).content));
        }else{
            return typeCalc("+", lType, rType);
        }
    }

    public static Type.Numeric typeCalc(String opName,Type lType, Type rType) {
        if(lType instanceof Type.Numeric &&rType instanceof Type.Numeric){
            int level= Math.max(((Type.Numeric) lType).level,((Type.Numeric) rType).level);
            boolean isFloat=((Type.Numeric) lType).isFloat||((Type.Numeric) rType).isFloat;
            boolean signed=((Type.Numeric) lType).signed||((Type.Numeric) rType).signed;
            if(isFloat){
                switch (level){
                    case 0:
                    case 1:
                    case 2:
                        return Type.Numeric.FLOAT32;
                    case 3:
                        return Type.Numeric.FLOAT64;
                    default:
                        throw new SyntaxError("exceeded maximum number capacity");
                }
            }else{
                switch (level){
                    case 0:
                        return signed?Type.Numeric.INT8: Type.Numeric.UINT8;
                    case 1:
                        return signed?Type.Numeric.INT16: Type.Numeric.UINT16;
                    case 2:
                        return signed?Type.Numeric.INT32: Type.Numeric.UINT32;
                    case 3:
                        return signed?Type.Numeric.INT64: Type.Numeric.UINT64;
                    default:
                        throw new SyntaxError("exceeded maximum number capacity");
                }
            }
        }else{
            throw new TypeError("Unsupported types for operation"+opName+": "+lType+", "+rType);
        }
    }

    public static Type typeBiIntOp(String opName,Type lType, Type rType){
        Type.Numeric t=typeCalc(opName, lType, rType);
        if(t.isFloat){
            throw new TypeError("Unsupported types for operation"+opName+": "+lType+", "+rType);
        }
        return t;
    }
    public static Type typeLShift(Type lType, Type rType) {
        if(lType instanceof Type.Array){//pushLast
            return Type.commonSupertype(((Type.Array) lType).content,rType);
        }else{
            return typeBiIntOp("<<",lType,rType);
        }
    }
    public static Type typeRShift(Type lType, Type rType) {
        if(rType instanceof Type.Array){//pushFirst
            return Type.commonSupertype(((Type.Array) rType).content,lType);
        }else{
            return typeBiIntOp(">>",lType,rType);
        }
    }

    public static Type typeDiv(Type lType, Type rType) {
        Type.Numeric calc=typeCalc("/",lType,rType);
        if(calc.isFloat){
            return calc;
        }else{
            switch (calc.level){
                case 0:
                case 1:
                case 2:
                    return Type.Numeric.FLOAT32;
                case 3:
                    return Type.Numeric.FLOAT64;
                default:
                    throw new SyntaxError("exceeded maximum number capacity");
            }
        }
    }

    public static Type typePow(Type lType, Type rType) {
        if(lType instanceof Type.Numeric&&rType instanceof Type.Numeric){
            return Type.Numeric.FLOAT64;
        }else{
            throw new TypeError("Unsupported types for operation ** : "+lType+", "+rType);
        }
    }


    public static boolean typeCheckCompare(Type lType, Type rType) {
        //addLater? more comparable types
        if(lType instanceof Type.Numeric &&rType instanceof Type.Numeric){
            return true;
        }else return lType == Type.Primitive.STRING && rType == Type.Primitive.STRING;
    }

}
