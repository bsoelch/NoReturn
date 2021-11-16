package bsoelch.noret.lang.expression;

import bsoelch.noret.SyntaxError;
import bsoelch.noret.TypeError;
import bsoelch.noret.lang.*;

import java.util.ArrayList;

public class LeftUnaryOp implements Expression {
    final OperatorType op;
    final Expression expr;

    final Type expectedOutput;

    public static Expression create(OperatorType op, Expression expr){
        Type expectedOut=typeCheck(op,expr.expectedType());
        if(expr instanceof ValueExpression){//constant folding
            return new ValueExpression(evaluate(op,((ValueExpression) expr).value));
        }
        return new LeftUnaryOp(op, expr,expectedOut);
    }
    private LeftUnaryOp( OperatorType op, Expression expr,Type expectedOutput) {
        this.op = op;
        this.expr = expr;
        this.expectedOutput=expectedOutput;
    }

    private static Value evaluate(OperatorType op,Value lVal) {
        switch (op){
            case PLUS:
                return lVal;
            case MINUS:
                return Operations.negate(lVal);
            case NOT:
                return Operations.not(lVal);
            case FLIP:
                return Operations.flip(lVal);
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
                throw new SyntaxError(op+" is no left-unary operator");
        }
        throw new SyntaxError(op+" is no valid operator");
    }

    @Override
    public ValueView evaluate(Procedure parent, ArrayList<Value> context) {
        return ValueView.wrap(evaluate(op,expr.evaluate(parent, context).get()));
    }

    @Override
    public Type expectedType() {
        return expectedOutput;
    }

    private static Type typeCheck(OperatorType op,Type argType) {
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
                throw new TypeError("illegal operand for "+(op==OperatorType.PLUS?"+":"-")+":"+argType);
            case FLIP:
                if(argType instanceof Type.Numeric&&(!((Type.Numeric) argType).isFloat))
                    return argType;
                throw new TypeError("illegal operand for flip:"+argType);
            case NOT:
                if(Type.canAssign(Type.Primitive.BOOL,argType,null))
                    return Type.Primitive.BOOL;
                throw new TypeError("illegal operand for not:"+argType);
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
                throw new SyntaxError(op+" is no left-unary operator");
        }
        throw new SyntaxError(op+" is no valid operator");
    }
}

