package bsoelch.noret.lang.expression;

import bsoelch.noret.ProgramContext;
import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;
import java.util.function.Supplier;

public class BinOp implements Expression {
    public final Expression left;
    public final OperatorType op;
    public final Expression right;

    final Type expectedOutput;

    public static Expression create(Expression left, OperatorType op, Expression right, ProgramContext context){
        Type lType = left.expectedType();
        Type rType = right.expectedType();
        if(lType instanceof Type.Primitive&&rType instanceof Type.Primitive){
            Type type=typeCheck((Type.Primitive) lType, op, (Type.Primitive) rType);
            if(left.hasValue(context)&&right.hasValue(context)){//fold constants
                return ValueExpression.create(evaluate(left.getValue(context),op,()->right.getValue(context)));
            }
            return new BinOp(left, op, right,type);
        }else{
            switch (op) {
                case LSHIFT:
                    if (lType instanceof Type.NoRetString) {
                        return StringConcat.appendEnd(left,right);
                    } else if (lType instanceof Type.Array) {
                        return TupleConcat.pushEnd(left,right,context);
                    }
                    break;
                case RSHIFT:
                    if (rType instanceof Type.NoRetString) {
                        return StringConcat.appendStart(left,right);
                    } else if (rType instanceof Type.Array) {
                        return TupleConcat.pushStart(left,right,context);
                    }
                    break;
                case PLUS:
                    if (lType instanceof Type.NoRetString || rType instanceof Type.NoRetString) {
                        return StringConcat.concat(left,right);
                    } else if (lType instanceof Type.Array && rType instanceof Type.Array) {
                        return TupleConcat.concat(left,right,context);
                    }
                    break;
                case EQ:
                case NE://eq functions for any expressions
                    if (lType instanceof Type.NoRetString && rType instanceof Type.NoRetString) {
                        return StringCompare.create(left,op,right,context);
                    }else{
                        return new EqualityCheck(left,op==OperatorType.NE,right);
                    }
                case GT:
                case GE:
                case LE:
                case LT:
                    if (lType instanceof Type.NoRetString && rType instanceof Type.NoRetString) {
                        return StringCompare.create(left,op,right,context);
                    }
                    break;
            }
            throw new TypeError("Unsupported types for "+op+":"+lType+", "+rType);
        }
    }
    private static Type typeCheck(Type.Primitive lType,OperatorType op,Type.Primitive rType) {
        switch (op){
            case PLUS:
                return Operations.typeCalc("+", lType, rType);
            case MINUS:
                return Operations.typeCalc("-",lType,rType);
            case MULT:
                return Operations.typeCalc("*",lType,rType);
            case MOD:
                return Operations.typeCalc("%",lType,rType);
            case DIV:
                return Operations.typeDiv(lType,rType);
            case INT_DIV:
                return Operations.typeCalc("//",lType,rType);
            case POW:
                return Operations.typePow(lType,rType);
            case LSHIFT:
                return Operations.typeBiIntOp("<<",lType,rType);
            case RSHIFT:
                return Operations.typeBiIntOp(">>",lType,rType);
            case AND:
                return Operations.typeBiIntOp("&",lType,rType);
            case OR:
                return Operations.typeBiIntOp("|",lType,rType);
            case XOR:
                return Operations.typeBiIntOp("^",lType,rType);
            case FAST_AND:
            case FAST_OR:
                if(Type.canAssign(Type.Primitive.BOOL,lType,null)&&
                        Type.canAssign(Type.Primitive.BOOL,rType,null)){
                    return Type.Primitive.BOOL;
                }else{
                    throw new TypeError("invalid arguments for "+(op==OperatorType.FAST_AND?"&&":"||")+": "+lType+", "+rType);
                }
            case EQ:
            case NE://eq functions for any expressions
                return Type.Primitive.BOOL;
            case GT:
            case GE:
            case LE:
            case LT:
                if(!Operations.typeCheckCompare(lType,rType)){
                    throw new TypeError(lType+" and "+rType+" are not comparable");
                }
                return Type.Primitive.BOOL;
            case IF:
            case NOT:
            case FLIP:
                throw new SyntaxError(op+" is no binary operator");
        }
        return null;
    }

    private BinOp(Expression left, OperatorType op, Expression right,Type expectedOutput) {
        this.left = left;
        this.op = op;
        this.right = right;
        this.expectedOutput=expectedOutput;
    }

    public static Value evaluate(Value lVal,OperatorType op, Supplier<Value> lazyRight){
        //don't evaluate r-val to allow fast-and/or
        switch (op){
            case PLUS:
                return Operations.plus(lVal,lazyRight.get());
            case MINUS:
                return Operations.minus(lVal,lazyRight.get());
            case MULT:
                return Operations.multiply(lVal,lazyRight.get());
            case DIV:
                return Operations.div(lVal,lazyRight.get());
            case INT_DIV:
                return Operations.intDiv(lVal,lazyRight.get());
            case MOD:
                return Operations.mod(lVal,lazyRight.get());
            case POW:
                return Operations.pow(lVal,lazyRight.get());
            case LSHIFT:
                return Operations.lshift(lVal,lazyRight.get());
            case RSHIFT:
                return Operations.rshift(lVal,lazyRight.get());
            case AND:
                return Operations.and(lVal,lazyRight.get());
            case OR:
                return Operations.or(lVal,lazyRight.get());
            case XOR:
                return Operations.xor(lVal,lazyRight.get());
            case FAST_AND:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.asBool(lVal)&&Operations.asBool(lazyRight.get()));
            case FAST_OR:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.asBool(lVal)||Operations.asBool(lazyRight.get()));
            case NE:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.eq(lVal,lazyRight.get()));
            case EQ:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        !Operations.eq(lVal,lazyRight.get()));
            case GT:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,lazyRight.get())>0);
            case GE:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,lazyRight.get())>=0);
            case LE:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,lazyRight.get())<=0);
            case LT:
                return Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,lazyRight.get())<0);
            case IF:
            case NOT:
            case FLIP:
                throw new SyntaxError(op+" is no binary operator");
        }
        throw new SyntaxError(op+" is no valid operator");
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return ValueView.wrap(evaluate(left.evaluate(parent, context).get(),op,()->right.evaluate(parent, context).get()));
    }

    @Override
    public boolean isBound() {
        return false;//bin op always creates a new value
    }

    @Override
    public Type expectedType() {
        return expectedOutput;
    }

    @Override
    public boolean canInline() {
        return left.canInline()&& right.canInline();
    }

    @Override
    public String toString() {
        return "BinOp{" +left + " "+op + " "+ right +'}';
    }

    @Override
    public boolean hasValue(ProgramContext context) {
        return false;//all possible compile-time evaluations are done on initialization
    }

    @Override
    public Value getValue(ProgramContext context) {
        throw new RuntimeException(this+" cannot be evaluated at compile time");
    }
}

