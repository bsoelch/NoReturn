package bsoelch.noret.lang.expression;

import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class LeftUnaryOp implements Expression {
    final OperatorType op;
    final Expression expr;

    final Type expectedOutput;

    public LeftUnaryOp( OperatorType op, Expression expr) {
        this.op = op;
        this.expr = expr;
        expectedOutput=typeCheck();
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        Value lVal=expr.evaluate(parent, context).get();
        switch (op){
            case PLUS:
                return ValueView.wrap(lVal);
            case MINUS:
                return ValueView.wrap(Operations.negate(lVal));
            case NOT:
                return ValueView.wrap(Operations.not(lVal));
            case FLIP:
                return ValueView.wrap(Operations.flip(lVal));
            case MULT:
            case DIV:
            case INT_DIV:
            case MOD:
            case POW:
            case LSHIFT:
            case RSHIFT:
            case AND:
            case OR:
            case XOR:
            case FAST_AND:
            case FAST_OR:
            case GT:
            case GE:
            case NE:
            case EQ:
            case LE:
            case LT:
            case IF:
                throw new IllegalArgumentException(op+" is no left-unary operator");
        }
        throw new IllegalArgumentException(op+" is no valid operator");
    }

    @Override
    public Type expectedType() {
        return expectedOutput;
    }

    public Type typeCheck() {
        Type argType=expr.expectedType();
        switch (op){
            case PLUS:
            case MINUS:
                if(argType instanceof Type.Numeric){
                    if(((Type.Numeric) argType).signed){
                        return argType;
                    }else{
                        if(((Type.Numeric) argType).isFloat){
                            throw new UnsupportedOperationException("Unsigned floats " +
                                    "are currently not supported");
                        }
                        switch (((Type.Numeric) argType).level){
                            case 0:
                                return Type.Numeric.INT16;
                            case 1:
                                return Type.Numeric.INT32;
                            default:
                                return Type.Numeric.INT64;
                        }
                    }
                }
                throw new IllegalArgumentException("Type-Error");
            case FLIP:
                if(argType instanceof Type.Numeric&&(!((Type.Numeric) argType).isFloat))
                    return argType;
                throw new IllegalArgumentException("Type-Error");
            case NOT:
                if(Type.canAssign(Type.Primitive.BOOL,argType,null))
                    return Type.Primitive.BOOL;
                throw new IllegalArgumentException("Type-Error");
            case MULT:
            case DIV:
            case INT_DIV:
            case MOD:
            case POW:
            case LSHIFT:
            case RSHIFT:
            case AND:
            case OR:
            case XOR:
            case FAST_AND:
            case FAST_OR:
            case GT:
            case GE:
            case NE:
            case EQ:
            case LE:
            case LT:
            case IF:
                throw new IllegalArgumentException(op+" is no left-unary operator");
        }
        throw new IllegalArgumentException(op+" is no valid operator");
    }
}

