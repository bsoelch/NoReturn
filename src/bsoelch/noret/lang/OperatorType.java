package bsoelch.noret.lang;

public enum OperatorType {
    PLUS, MINUS, MULT, DIV, INT_DIV, MOD, POW, AND, OR, XOR, NOT, FAST_AND, FAST_OR, FLIP,
    GT, GE, NE, EQ, LT, LE, IF, LSHIFT, RSHIFT
    //addLater? intPow
    //addLater? contains, isElement
    //addLater wait[Eq|Ne|Gt|Lt|Ge|Le]  (<int-ref> waitGt <int> freezes current thread while <int-ref> is greater that <int>
    //addLater? make print/read native operators
}
