package bsoelch.noret.lang.expression;

import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class BinOp implements Expression {
    final Expression left;
    final OperatorType op;
    final Expression right;

    final Type expectedOutput;

    public BinOp(Expression left, OperatorType op, Expression right) {
        this.left = left;
        this.op = op;
        this.right = right;
        expectedOutput=typeCheck();
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        Value lVal=left.evaluate(parent, context).get();
        //don't evaluate r-val to allow fast-and/or
        switch (op){
            case PLUS:
                return ValueView.wrap(Operations.plus(lVal,right.evaluate(parent, context).get()));
            case MINUS:
                return ValueView.wrap(Operations.minus(lVal,right.evaluate(parent, context).get()));
            case MULT:
                return ValueView.wrap(Operations.multiply(lVal,right.evaluate(parent, context).get()));
            case DIV:
                return ValueView.wrap(Operations.div(lVal,right.evaluate(parent, context).get()));
            case INT_DIV:
                return ValueView.wrap(Operations.intDiv(lVal,right.evaluate(parent, context).get()));
            case MOD:
                return ValueView.wrap(Operations.mod(lVal,right.evaluate(parent, context).get()));
            case POW:
                return ValueView.wrap(Operations.pow(lVal,right.evaluate(parent, context).get()));
            case LSHIFT:
                return ValueView.wrap(Operations.lshift(lVal,right.evaluate(parent, context).get()));
            case RSHIFT:
                return ValueView.wrap(Operations.rshift(lVal,right.evaluate(parent, context).get()));
            case AND:
                return ValueView.wrap(Operations.and(lVal,right.evaluate(parent, context).get()));
            case OR:
                return ValueView.wrap(Operations.or(lVal,right.evaluate(parent, context).get()));
            case XOR:
                return ValueView.wrap(Operations.xor(lVal,right.evaluate(parent, context).get()));
            case FAST_AND:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.asBool(lVal)&&
                                Operations.asBool(right.evaluate(parent, context).get())));
            case FAST_OR:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.asBool(lVal)||
                                Operations.asBool(right.evaluate(parent, context).get())));
            case NE:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.eq(lVal,right.evaluate(parent, context).get())));
            case EQ:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        !Operations.eq(lVal,right.evaluate(parent, context).get())));
            case GT:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,right.evaluate(parent, context).get())>0));
            case GE:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,right.evaluate(parent, context).get())>=0));
            case LE:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,right.evaluate(parent, context).get())<=0));
            case LT:
                return ValueView.wrap(Value.createPrimitive(Type.Primitive.BOOL,
                        Operations.compare(lVal,right.evaluate(parent, context).get())<0));
            case IF:
            case NOT:
            case FLIP:
                throw new SyntaxError(op+" is no binary operator");
        }
        throw new SyntaxError(op+" is no valid operator");
    }

    @Override
    public Type expectedType() {
        return expectedOutput;
    }

    private Type typeCheck() {
        Type lType=left.expectedType();
        Type rType=right.expectedType();
        switch (op){
            case PLUS:
                return Operations.typePlus(lType,rType);
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
                return Operations.typeLShift(lType,rType);
            case RSHIFT:
                return Operations.typeRShift(lType,rType);
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
                    throw new TypeError(lType+" and "+rType+" are not compareable");
                }
                return Type.Primitive.BOOL;
            case IF:
            case NOT:
            case FLIP:
                throw new SyntaxError(op+" is no binary operator");
        }
        return null;
    }
}

