package bsoelch.noret;

import bsoelch.noret.lang.expression.VarExpression;
import bsoelch.noret.lang.*;
import bsoelch.noret.lang.expression.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

public class Parser {

    enum WordState{
        ROOT,STRING,COMMENT,LINE_COMMENT
    }

    enum ParserTokenType {
        OPEN_BRACKET,CLOSE_BRACKET,OPEN_SQ_BRACKET,CLOSE_SQ_BRACKET,OPEN_CR_BRACKET,CLOSE_CR_BRACKET,
        OPERATOR,DOLLAR,OPTION,AT,SEPARATOR,END, ASSIGN,DOT,COMMA,MAPS_TO,WORD,TYPE,EXPRESSION,
        INDEX,RANGE,STRUCT_DEFINITION
    }

    static class TokenPosition{
        //addLater file
        final long line;
        final int posInLine;

        TokenPosition(long line, int posInLine) {
            this.line = line;
            this.posInLine = posInLine;
        }

        @Override
        public String toString() {
            return line+":"+posInLine;
        }
    }
    static class ParserToken{
        final ParserTokenType tokenType;
        final TokenPosition pos;
        ParserToken(ParserTokenType tokenType, TokenPosition pos) {
            this.tokenType = tokenType;
            this.pos = pos;
        }
        @Override
        public String toString() {
            return tokenType.toString();
        }
    }
    static class NamedToken extends ParserToken{
        final String value;
        NamedToken(ParserTokenType type, String value, TokenPosition pos) {
            super(type, pos);
            this.value=value;
        }
        @Override
        public String toString() {
            return tokenType.toString()+": \""+value+"\"";
        }
    }
    static class Operator extends ParserToken{
        final OperatorType opType;
        Operator(OperatorType opType, TokenPosition pos) {
            super(ParserTokenType.OPERATOR, pos);
            this.opType=opType;
        }
        @Override
        public String toString() {
            return tokenType.toString()+": "+opType.toString();
        }
    }
    static class TypeToken extends ParserToken{
        final Type type;
        TypeToken(Type type, TokenPosition pos) {
            super(ParserTokenType.TYPE,pos);
            this.type=type;
        }
        @Override
        public String toString() {
            return tokenType.toString()+": \""+type+"\"";
        }
    }
    static class ExprToken extends ParserToken{
        final Expression expr;
        ExprToken(Value value, String constId, TokenPosition pos) {
            this(false,ValueExpression.create(value, constId), pos);
        }
        ExprToken(Expression expr, TokenPosition pos) {
            this(false,expr, pos);
        }
        ExprToken(boolean isIndex,Expression expr, TokenPosition pos) {
            super(isIndex?ParserTokenType.INDEX:ParserTokenType.EXPRESSION, pos);
            this.expr=expr;
        }
        @Override
        public String toString() {
            return tokenType.toString()+": \""+expr+"\"";
        }
    }

    private static class Tokenizer{
        private final Reader input;
        private final StringBuilder buffer=new StringBuilder();
        private final ArrayDeque<ParserToken> tokenBuffer =new ArrayDeque<>();


        static final String DEC_DIGIT = "[0-9]";
        static final String BIN_DIGIT = "[01]";
        static final String HEX_DIGIT = "[0-9a-fA-F]";
        static final String UNSIGNED_POSTFIX = "[u|U]?";
        static final String BIN_PREFIX = "0b";
        static final String HEX_PREFIX = "0x";
        static final String DEC_INT_PATTERN = DEC_DIGIT + "+";
        static final String BIN_INT_PATTERN = BIN_PREFIX + BIN_DIGIT + "+";
        static final String HEX_INT_PATTERN = HEX_PREFIX + HEX_DIGIT + "+";
        static final Pattern intDec=Pattern.compile(DEC_INT_PATTERN+UNSIGNED_POSTFIX);
        static final Pattern intBin=Pattern.compile(BIN_INT_PATTERN+UNSIGNED_POSTFIX);
        static final Pattern intHex=Pattern.compile(HEX_INT_PATTERN+UNSIGNED_POSTFIX);
        static final Pattern floatDotPrefix =Pattern.compile(//pattern for prefixes of . or , that lead to floats
                "("+BIN_INT_PATTERN+")|("+DEC_INT_PATTERN+")|("+HEX_INT_PATTERN+")");
        static final String DEC_FLOAT_MAGNITUDE = DEC_INT_PATTERN + "\\.?"+DEC_DIGIT+"*";
        static final String BIN_FLOAT_MAGNITUDE = BIN_INT_PATTERN + "\\.?"+BIN_DIGIT+"*";
        static final String HEX_FLOAT_MAGNITUDE = HEX_INT_PATTERN + "\\.?"+HEX_DIGIT+"*";
        static final Pattern floatDec=Pattern.compile("NaN|Infinity|("+
                                                      DEC_FLOAT_MAGNITUDE+"([Ee][+-]?"+DEC_DIGIT+"+)?)");
        static final Pattern floatHex=Pattern.compile(HEX_FLOAT_MAGNITUDE+"([Pp][+-]?"+HEX_DIGIT+"+)?");
        static final Pattern floatBin=Pattern.compile(BIN_FLOAT_MAGNITUDE+"([Ee][+-]?"+BIN_DIGIT+"+)?");
        static final Pattern floatExpPrefix =Pattern.compile("((("+BIN_FLOAT_MAGNITUDE+")|("
                +DEC_FLOAT_MAGNITUDE+"))[Ee])|("+HEX_FLOAT_MAGNITUDE+"[Pp])");

        enum StringType{
            AUTO,UTF8,UTF16,UTF32
        }

        private WordState state=WordState.ROOT;
        private int stringStart=-1;
        private StringType stringType;
        private int cached=-1;

        private int line =0;
        private int posInLine =0;

        private Tokenizer(Reader input) {
            this.input = input;
        }

        ParserToken getNextToken() throws IOException {
            if(tokenBuffer.size()==0){
                prepareToken();
            }
            return tokenBuffer.pollFirst();
        }

        private int nextChar() throws IOException {
            if(cached>=0){
                int c=cached;
                cached = -1;
                return c;
            }else{
                return input.read();
            }
        }
        private int forceNextChar() throws IOException {
            int c=nextChar();
            if (c < 0) {
                throw new SyntaxError("Unexpected end of File");
            }
            return c;
        }
        //addLater better error feedback
        private void prepareToken() throws IOException {
            int c;
            while((c=nextChar())>=0){
                posInLine++;
                if(c=='\n'){//addLater? support for \r line separator
                    line++;
                    posInLine=0;
                }
                switch(state){
                    case ROOT:
                        if(Character.isWhitespace(c)){
                            if(finishWord(tokenBuffer, buffer)){
                                return;
                            }
                        }else{
                            switch(c){
                                case '"':
                                case '\'':
                                    state=WordState.STRING;
                                    stringStart=(char)c;
                                    switch (buffer.toString()){
                                        case "":
                                            stringType=StringType.AUTO;
                                            break;
                                        case "u8":
                                            stringType=StringType.UTF8;
                                            buffer.setLength(0);
                                            break;
                                        case "u16":
                                            stringType=StringType.UTF16;
                                            buffer.setLength(0);
                                            break;
                                        case "u32":
                                            stringType=StringType.UTF32;
                                            buffer.setLength(0);
                                            break;
                                        default:
                                            throw new SyntaxError("Illegal string prefix:\""+buffer+"\"");
                                    }
                                    break;
                                case '#':
                                    c = forceNextChar();
                                    if(c=='#'){
                                        state=WordState.LINE_COMMENT;
                                        if(finishWord(tokenBuffer, buffer)){
                                            return;
                                        }
                                    }else if(c=='_'){
                                        state=WordState.COMMENT;
                                        if(finishWord(tokenBuffer, buffer)){
                                            return;
                                        }
                                    }else{
                                        buffer.append('#').append((char)c);
                                    }
                                    break;
                                case '(':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.OPEN_BRACKET,currentPos()));
                                    return;
                                case ')':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.CLOSE_BRACKET,currentPos()));
                                    return;
                                case '[':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.OPEN_SQ_BRACKET,currentPos()));
                                    return;
                                case ']':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.CLOSE_SQ_BRACKET,currentPos()));
                                    return;
                                case '{':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.OPEN_CR_BRACKET,currentPos()));
                                    return;
                                case '}':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.CLOSE_CR_BRACKET,currentPos()));
                                    return;
                                case '@':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.AT, currentPos()));
                                    return;
                                case '$':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.DOLLAR,currentPos()));
                                    return;
                                case '+':
                                case '-':
                                    if(floatExpPrefix.matcher(buffer).matches()){
                                        buffer.append((char)c);
                                        break;
                                    }
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new Operator(c=='+'?OperatorType.PLUS:OperatorType.MINUS,
                                            currentPos()));
                                    return;
                                case '%':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new Operator(OperatorType.MOD,currentPos()));
                                    return;
                                case '~':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new Operator(OperatorType.FLIP,currentPos()));
                                    return;
                                case '^':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new Operator(OperatorType.XOR,currentPos()));
                                    return;
                                case '?':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.OPTION,currentPos()));
                                    return;
                                case ':':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.SEPARATOR,currentPos()));
                                    return;
                                case ';':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.END,currentPos()));
                                    return;
                                case '.':
                                    if(floatDotPrefix.matcher(buffer).matches()){
                                        buffer.append('.');
                                        break;//. after int -> floating point number
                                    }
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.DOT, currentPos()));
                                    return;
                                case ',':
                                    finishWord(tokenBuffer, buffer);
                                    tokenBuffer.addLast(new ParserToken(ParserTokenType.COMMA, currentPos()));
                                    return;
                                //detect multi-char operators
                                case '*'://* **
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '*':
                                            tokenBuffer.addLast(new Operator(OperatorType.POW,currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new Operator(OperatorType.MULT, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS,currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new Operator(OperatorType.MULT, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS, currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new Operator(OperatorType.MULT,currentPos()));
                                            cached=c;
                                            return;
                                    }
                                case '/':// / //
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '/':
                                            tokenBuffer.addLast(new Operator(OperatorType.INT_DIV,currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new Operator(OperatorType.DIV,currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS, currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new Operator(OperatorType.DIV, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS, currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new Operator(OperatorType.DIV, currentPos()));
                                            cached=c;
                                            return;
                                    }
                                case '&':// & &&
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '&':
                                            tokenBuffer.addLast(new Operator(OperatorType.FAST_AND, currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new Operator(OperatorType.AND,currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS, currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new Operator(OperatorType.AND,  currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS, currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new Operator(OperatorType.AND, currentPos()));
                                            cached=c;
                                            return;
                                    }
                                case '|':// | ||
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '|':
                                            tokenBuffer.addLast(new Operator(OperatorType.FAST_OR, currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new Operator(OperatorType.OR, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS, currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new Operator(OperatorType.OR, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS, currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new Operator(OperatorType.OR, currentPos()));
                                            cached=c;
                                            return;
                                    }
                                case '=':// = == =>
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '>':
                                            tokenBuffer.addLast(new ParserToken(ParserTokenType.MAPS_TO, currentPos()));
                                            return;
                                        case '=':
                                            tokenBuffer.addLast(new Operator(OperatorType.EQ, currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new ParserToken(ParserTokenType.ASSIGN, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS, currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new ParserToken(ParserTokenType.ASSIGN, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS, currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new ParserToken(ParserTokenType.ASSIGN, currentPos()));
                                            cached=c;
                                            return;
                                    }
                                case '!':// ! !=
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '=':
                                            tokenBuffer.addLast(new Operator(OperatorType.NE, currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new Operator(OperatorType.NOT,  currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS,currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new Operator(OperatorType.NOT, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS,currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new Operator(OperatorType.NOT, currentPos()));
                                            cached=c;
                                            return;
                                    }
                                case '<':// < <= <<
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '=':
                                            tokenBuffer.addLast(new Operator(OperatorType.LE,    currentPos()));
                                            return;
                                        case '<':
                                            tokenBuffer.addLast(new Operator(OperatorType.LSHIFT,currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new Operator(OperatorType.LT,    currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS, currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new Operator(OperatorType.LT,    currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS,  currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new Operator(OperatorType.LT,    currentPos()));
                                            cached=c;
                                            return;
                                    }
                                case '>':// > >= >>
                                    finishWord(tokenBuffer, buffer);
                                    c = forceNextChar();
                                    switch(c){
                                        case '=':
                                            tokenBuffer.addLast(new Operator(OperatorType.GE, currentPos()));
                                            return;
                                        case '>':
                                            tokenBuffer.addLast(new Operator(OperatorType.RSHIFT,currentPos()));
                                            return;
                                        case '-':
                                            tokenBuffer.addLast(new Operator(OperatorType.GT, currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.MINUS, currentPos()));
                                            return;
                                        case '+':
                                            tokenBuffer.addLast(new Operator(OperatorType.GT,  currentPos()));
                                            tokenBuffer.addLast(new Operator(OperatorType.PLUS,currentPos()));
                                            return;
                                        default:
                                            tokenBuffer.addLast(new Operator(OperatorType.GT, currentPos()));
                                            cached=c;
                                            return;
                                    }
                                default:
                                    buffer.append((char)c);
                            }
                        }
                        break;
                    case STRING:
                        if(c==stringStart){
                            if(c=='\''){//char literal
                                if(buffer.codePoints().count()==1){
                                    int codePoint = buffer.codePointAt(0);
                                    tokenBuffer.addLast(new ExprToken(Value.createPrimitive(
                                            codePoint<0x80?Type.Numeric.CHAR8:codePoint<0x10000?Type.Numeric.CHAR16:
                                                    Type.Numeric.CHAR32, codePoint), null, currentPos()));
                                }else{
                                    throw new SyntaxError("A char-literal must contain exactly one character");
                                }
                            }else{
                                String value = buffer.toString();
                                Type.NoRetString sType;
                                switch (stringType){
                                    case AUTO:
                                        int maxCp=value.codePoints().max().orElse(0);
                                        sType = maxCp < 0x80 ? Type.NoRetString.STRING8 :
                                                maxCp < 0x10000 ? Type.NoRetString.STRING16 : Type.NoRetString.STRING32;
                                        break;
                                    case UTF8:
                                        sType= Type.NoRetString.STRING8;
                                        break;
                                    case UTF16:
                                        sType= Type.NoRetString.STRING16;
                                        break;
                                    case UTF32:
                                        sType= Type.NoRetString.STRING32;
                                        break;
                                    default:
                                        throw new RuntimeException("unreachable");
                                }
                                tokenBuffer.addLast(new ExprToken(Value.createString(sType, value), null, currentPos()));
                            }
                            buffer.setLength(0);
                            state=WordState.ROOT;
                            return;
                        }else{
                            if(c=='\\'){
                                c = forceNextChar();
                                switch (c){
                                    case '\\':
                                    case '\'':
                                    case '"':
                                        buffer.append((char)c);
                                        break;
                                    case 'n':
                                        buffer.append('\n');
                                        break;
                                    case 't':
                                        buffer.append('\t');
                                        break;
                                    case 'r':
                                        buffer.append('\r');
                                        break;
                                    case 'b':
                                        buffer.append('\b');
                                        break;
                                    case 'f':
                                        buffer.append('\f');
                                        break;
                                    case '0':
                                        buffer.append('\0');
                                        break;
                                    case 'u':
                                    case 'U':
                                        throw new UnsupportedOperationException("unicode-escape sequence are currently not implemented");
                                    //addLater more escape sequences
                                    default:
                                        throw new IllegalArgumentException("The escape sequence: '\\"+c+"' is not supported");
                                }
                            }else{
                                buffer.append((char)c);
                            }
                        }
                        break;
                    case COMMENT:
                        if(c=='_'){
                            c = forceNextChar();
                            if(c=='#'){
                                state=WordState.ROOT;
                            }
                        }
                        break;
                    case LINE_COMMENT:
                        if(c=='\n'||c=='\r'){
                            state=WordState.ROOT;
                        }
                        break;
                }
            }
            if(state!=WordState.ROOT){
                throw new SyntaxError("Unexpected end of File");
            }
            finishWord(tokenBuffer,buffer);
        }

        private TokenPosition currentPos() {
            return new TokenPosition(line, posInLine);
        }

        /**@return false if the value was an integer otherwise true*/
        private boolean tryParseInt(ArrayDeque<ParserToken> tokens,String str){
            if(intDec.matcher(str).matches()){//dez-Int
                parseInt(tokens, str, 10);
                return false;
            }else if(intBin.matcher(str).matches()){//bin-Int
                str=str.replaceAll(BIN_PREFIX,"");//remove header
                parseInt(tokens, str, 2);
                return false;
            }else if(intHex.matcher(str).matches()){ //hex-Int
                str=str.replaceAll(HEX_PREFIX,"");//remove header
                parseInt(tokens, str, 16);
                return false;
            }
            return true;
        }

        private boolean finishWord(ArrayDeque<ParserToken> tokens, StringBuilder buffer) {
            if (buffer.length() > 0) {
                String str=buffer.toString();
                try{
                    if (tryParseInt(tokens, str)) {
                        if(floatDec.matcher(str).matches()){
                            //dez-Float
                            double d = Double.parseDouble(str);
                            tokens.addLast(new ExprToken(
                                    Value.createPrimitive(Type.Numeric.FLOAT64, d),null, currentPos()));
                        }else if(floatBin.matcher(str).matches()){
                            //bin-Float
                            double d=parseBinFloat(
                                    str.replaceAll(BIN_PREFIX,"")//remove header
                            );
                            tokens.addLast(new ExprToken(
                                    Value.createPrimitive(Type.Numeric.FLOAT64, d), null, currentPos()));
                        }else if(floatHex.matcher(str).matches()){
                            //hex-Float
                            double d=parseHexFloat(
                                    str.replaceAll(HEX_PREFIX,"")//remove header
                            );
                            tokens.addLast(new ExprToken(
                                    Value.createPrimitive(Type.Numeric.FLOAT64, d),null, currentPos()));
                        }else {
                            {//split string at . , + -
                                int i = str.indexOf('.');
                                if (i != -1) {
                                    if (i > 0) {
                                        String tmp=str.substring(0, i);
                                        if (tryParseInt(tokens, tmp)){
                                            addWord(tokens, tmp);
                                        }
                                    }
                                    tokens.addLast(new ParserToken(ParserTokenType.DOT, currentPos()));
                                    str = str.substring(i + 1);
                                }
                                i = str.indexOf('+');
                                if (i != -1) {
                                    if (i > 0)
                                        addWord(tokens, str.substring(0, i));
                                    tokens.addLast(new Operator(OperatorType.PLUS, currentPos()));
                                    str = str.substring(i + 1);
                                }
                                i = str.indexOf('-');
                                if (i != -1) {
                                    if (i > 0)
                                        addWord(tokens, str.substring(0, i));
                                    tokens.addLast(new Operator(OperatorType.MINUS,currentPos()));
                                    str = str.substring(i + 1);
                                }
                            }
                            if(str.length()>0)
                                addWord(tokens, str);
                        }
                    }
                }catch (NumberFormatException nfe){
                    throw new SyntaxError(nfe);
                }
                buffer.setLength(0);
                return true;
            }
            return false;
        }

        private void addWord(ArrayDeque<ParserToken> tokens, String str) {
            switch(str){
                case "none":
                    tokens.addLast(new ExprToken(Value.NONE, null, currentPos()));
                    break;
                case "true":
                    tokens.addLast(new ExprToken(Value.TRUE, null, currentPos()));
                    break;
                case "false":
                    tokens.addLast(new ExprToken(Value.FALSE, null, currentPos()));
                    break;
                case "struct"://named list of fields
                case "tuple"://list of anonymous fields
                case "union"://multiple fields in same storage location
                    tokens.addLast(new NamedToken(ParserTokenType.STRUCT_DEFINITION, str, currentPos()));
                    break;
                default:
                    tokens.addLast(new NamedToken(ParserTokenType.WORD, str, currentPos()));
            }
        }

        private void parseInt(ArrayDeque<ParserToken> tokens, String str, int base) {
            if(str.endsWith("u")||str.endsWith("U")){//unsigned
                str=str.substring(0,str.length()-1);
                try {
                    int i = Integer.parseUnsignedInt(str, base);
                    tokens.addLast(new ExprToken(Value.createPrimitive(Type.Numeric.UINT32, i), null, currentPos()));
                } catch (NumberFormatException nfeI) {
                    try {
                        long l = Long.parseUnsignedLong(str, base);
                        tokens.addLast(new ExprToken(Value.createPrimitive(Type.Numeric.UINT64, l),null, currentPos()));
                    } catch (NumberFormatException nfeL) {
                        throw new SyntaxError("Number out of Range:"+str);
                    }
                }
            }else{
                try {
                    int i = Integer.parseInt(str, base);
                    tokens.addLast(new ExprToken(Value.createPrimitive(Type.Numeric.INT32, i),null, currentPos()));
                } catch (NumberFormatException nfeI) {
                    try {
                        long l = Long.parseLong(str, base);
                        tokens.addLast(new ExprToken(Value.createPrimitive(Type.Numeric.INT64, l), null, currentPos()));
                    } catch (NumberFormatException nfeL) {
                        throw new SyntaxError("Number out of Range:"+str);
                    }
                }
            }
        }

        private double parseBinFloat(String str){
            long val=0;
            int c1=0,c2=0;
            int d2=0,e=-1;
            for(int i=0;i<str.length();i++){
                switch (str.charAt(i)){
                    case '0':
                    case '1':
                        if(c1<63){
                            val*=2;
                            val+=str.charAt(i)-'0';
                            c1++;
                            c2+=d2;
                        }
                        break;
                    case '.':
                        if(d2!=0){
                            throw new SyntaxError("Duplicate decimal point");
                        }
                        d2=1;
                        break;
                    case 'E':
                    case 'e':
                        e=i+1;
                        break;
                }
            }
            if (e > 0) {
                c2-=Integer.parseInt(str.substring(e),2);
            }
            return val*Math.pow(2,-c2);
        }
        private double parseHexFloat(String str){
            long val=0;
            int c1=0,c2=0;
            int d2=0,e=-1;
            for(int i=0;i<str.length();i++){
                switch (str.charAt(i)){
                    case '0':case '1':case '2':
                    case '3':case '4':case '5':
                    case '6':case '7':case '8':
                    case '9':
                        if(c1<15){
                            val*=16;
                            val+=str.charAt(i)-'0';
                            c1++;
                            c2+=d2;
                        }
                        break;
                    case 'A':case 'B':case 'C':
                    case 'D':case 'E':case 'F':
                        if(c1<15){
                            val*=16;
                            val+=str.charAt(i)-'A'+10;
                            c1++;
                            c2+=d2;
                        }
                        break;
                    case 'a':case 'b':case 'c':
                    case 'd':case 'e':case 'f':
                        if(c1<15){
                            val*=16;
                            val+=str.charAt(i)-'a'+10;
                            c1++;
                            c2+=d2;
                        }
                        break;
                    case '.':
                        if(d2!=0){
                            throw new SyntaxError("Duplicate decimal point");
                        }
                        d2=1;
                        break;
                    case 'P':
                    case 'p':
                        e=i+1;
                        break;
                }
            }
            if (e > 0) {
                c2-=Integer.parseInt(str.substring(e),16);
            }
            return val*Math.pow(2,-c2);
        }
    }


    public static class ParserContext{
        final HashMap<String, Type>      typeNames = new HashMap<>();
        final HashMap<String, Procedure> procNames = new HashMap<>();
        final HashMap<String, Value>     constants = new HashMap<>();

        final HashMap<String, Integer>   varIds    = new HashMap<>();
        final ArrayList<Type> varTypes = new ArrayList<>();

        final HashSet<Type> runtimeTypes =new HashSet<>();
        final HashSet<Type> runtimeBlockTypes =new HashSet<>();
        final HashSet<Type.StructOrUnion> runtimeStructs =new HashSet<>();

        long maxArgSize=0;

        public void declareProcedure(String name,Procedure proc){
            procNames.put(name,proc);
            maxArgSize=Math.max(maxArgSize,((Type.Proc)proc.getType()).argsBlockSize());
            varIds.clear();
            varTypes.clear();
        }

        private boolean hasName(String name) {
            return typeNames.containsKey(name)|| procNames.containsKey(name)||
                    constants.containsKey(name)|| varIds.containsKey(name);
        }

        /**child types of containers that are needed at runtime,
         * this method is a helper of the compiler
         * @param topLevel true if the supplied type is not contained in any other type*/
        public void addRuntimeType(Type t,boolean topLevel){
            if(t instanceof Type.Struct||t instanceof Type.Union){
                runtimeStructs.add((Type.StructOrUnion) t);
            }else if(t instanceof Type.Tuple||t instanceof Type.Proc){
                runtimeBlockTypes.add(t);
            }else if(!topLevel){
                runtimeTypes.add(t);
            }
            //add child types
            for(Type c: t.childTypes()){
                addRuntimeType(c,false);
            }
        }

        public void defType(String name,Type type){
            if(hasName(name)){
                throw new SyntaxError(name+" is already defined");
            }else{
                typeNames.put(name,type);
            }
        }
        public void declareVariable(String name,Type type){
            if(hasName(name)){
                throw new SyntaxError(name+" is already defined");
            }else{
                varIds.put(name,varIds.size());
                varTypes.add(type);
            }
        }

        public Type getType(String name){
            return typeNames.get(name);
        }
        public Procedure getProc(String name){
            return procNames.get(name);
        }

        public int getVarId(String name){
            Integer get=varIds.get(name);
            return get==null?-1:get;
        }

        public Type getVarType(int id) {
            return varTypes.get(id);
        }

        public int varCount(){
            return varIds.size();
        }

        public void addConstant(String constName, Value constValue) {
            if(hasName(constName)){
                throw new SyntaxError(constName+" is already defined");
            }else{
                constants.put(constName,constValue);
            }
        }
        public Value getConst(String constName){
            return constants.get(constName);
        }

        public long maxArgSize() {
            return maxArgSize;
        }
    }

    enum TypeParserState{
        ROOT,BRACKET,STRUCT_TYPE,STRUCT_NAME,TUPLE
    }
    enum ExpressionParserState{
        ROOT,BRACKET, ARRAY,STRUCT_NAME,STRUCT_VALUE,INDEX,RANGE
    }
    enum ActionParserState{
        ROOT,ASSIGN_EXPR,DEF_EXPR, ASSERT, LOG
    }

    ParserContext context;

    /*
    <root_element>=<typedef>|<proc_def>
    <typedef>='typedef' <Name> <Type> ';'
    <proc_def>= <Name>'('<Type>(',' <Type>)*') : ('<Name>(,<Name>)*')=>{'
      (<Action>';')*'}=>['<Name>'('<Name>(,<Name>)*')'(','<Name>'('<Name>(,<Name>)*')')*']'
    <Action> = <Type>': '<Name>' '<Expression>|<proc_def>|<Name>'='<Expression>
    <Expression> = <Value>|'('<Expression>')'|<Expression>'?'<Expression>':'<Expression>|
      <Expression><bOp><Expression>|<lOp><Expression>|<Expression><rOp>

    */
    public Parser(Reader in) throws IOException {
        context=new ParserContext();
        Type.addAtomics(context.typeNames);
        parse(in);
    }
    private void updateBracketStack(ParserToken nextToken, ArrayDeque<ParserTokenType> bracketStack) {
        if(nextToken.tokenType ==ParserTokenType.OPEN_BRACKET){
            bracketStack.addLast(ParserTokenType.OPEN_BRACKET);
        }else if(nextToken.tokenType ==ParserTokenType.OPEN_SQ_BRACKET){
            bracketStack.addLast(ParserTokenType.OPEN_SQ_BRACKET);
        }else if(nextToken.tokenType ==ParserTokenType.OPEN_CR_BRACKET){
            bracketStack.addLast(ParserTokenType.OPEN_CR_BRACKET);
        }else if(nextToken.tokenType ==ParserTokenType.CLOSE_BRACKET){
            if(bracketStack.isEmpty()||
                    bracketStack.removeLast()!=ParserTokenType.OPEN_BRACKET){
                throw new SyntaxError("unexpected closing bracket");
            }
        }else if(nextToken.tokenType ==ParserTokenType.CLOSE_SQ_BRACKET){
            if(bracketStack.isEmpty()||
                    bracketStack.removeLast()!=ParserTokenType.OPEN_SQ_BRACKET){
                throw new SyntaxError("unexpected end of struct");
            }
        }else if(nextToken.tokenType ==ParserTokenType.CLOSE_CR_BRACKET){
            if(bracketStack.isEmpty()||
                    bracketStack.removeLast()!=ParserTokenType.OPEN_CR_BRACKET){
                throw new SyntaxError("unexpected end of struct");
            }
        }
    }

    private Type typeFromTokens(ParserContext context, ArrayList<ParserToken> tokens, String structName) {
        if(structName!=null&&tokens.get(0).tokenType!=ParserTokenType.STRUCT_DEFINITION){
            //addLater allow top-level structs containing optionals/arrays/references to themselves
            throw new RuntimeException("structName should only be non-null if the current call is a struct definition");
        }
        //1. parse primitives to TYPE
        // <Primitive> | <typedef Type>
        Type tmp;
        ParserToken token;
        for(int i=0;i<tokens.size();i++){
            if(tokens.get(i).tokenType ==ParserTokenType.WORD) {
                if (i > 0 && tokens.get(i - 1).tokenType == ParserTokenType.DOLLAR) {
                    token = tokens.remove(i--);
                    tokens.set(i, new TypeToken(new Type.AnyType(((NamedToken) token).value), token.pos));
                } else if (i == 0 ||(tokens.get(i - 1).tokenType != ParserTokenType.SEPARATOR&&
                        tokens.get(i-1).tokenType!=ParserTokenType.STRUCT_DEFINITION)) {
                    //word preceded with : is param name
                    token=tokens.get(i);
                    tmp = context.getType(((NamedToken) token).value);
                    if (tmp == null) {
                        throw new SyntaxError("Not a typeName: \"" +((NamedToken)token).value+"\" at "+token.pos);
                    }
                    tokens.set(i, new TypeToken(tmp, token.pos));
                }
            }
        }
        //2. parse parentheses
        // '('<Type>')'
        // '('<Type>(','<Type>)*')=>?'
        // struct '{'<Type>':'<Name>(','<Type>':'<Name>)*'}'
        // union  '{'<Type>':'<Name>(','<Type>':'<Name>)*'}'
        ArrayDeque<ParserTokenType> bracketStack=new ArrayDeque<>();
        TypeParserState state=TypeParserState.ROOT;
        ArrayList<ParserToken> tokenBuffer=new ArrayList<>();
        ArrayList<Type> typeBuffer=new ArrayList<>();
        ArrayList<String> nameBuffer=new ArrayList<>();
        boolean isUnion=false;
        for(int i=0;i<tokens.size();i++){
            updateBracketStack(tokens.get(i), bracketStack);
            switch (state){
                case ROOT:
                    if(tokens.get(i).tokenType==ParserTokenType.STRUCT_DEFINITION){
                        switch (((NamedToken)tokens.get(i)).value){
                            case "struct":
                            case "union":
                            case "tuple":
                                if(i+1<tokens.size()&&tokens.get(i+1).tokenType==ParserTokenType.OPEN_CR_BRACKET){
                                    updateBracketStack(tokens.remove(i+1),bracketStack);
                                }else{
                                    throw new SyntaxError("unexpected start of anonymous struct:"+tokens.get(i+1)+
                                            " expected: <struct-type>{<content>}");
                                }
                                isUnion=((NamedToken)tokens.get(i)).value.equals("union");
                                state=((NamedToken)tokens.remove(i--)).value.equals("tuple")?TypeParserState.TUPLE:TypeParserState.STRUCT_TYPE;
                                break;
                            default:
                                throw new RuntimeException("Unexpected struct-type:"+((NamedToken)tokens.get(i)).value);
                        }
                    }else if(bracketStack.size()>0){
                        if(bracketStack.peekLast()==ParserTokenType.OPEN_BRACKET){
                            tokens.remove(i--);
                            state=TypeParserState.BRACKET;
                        }else if(bracketStack.peekLast()==ParserTokenType.OPEN_CR_BRACKET){
                            throw new SyntaxError("unexpected { (struct definitions need to be preceded by the struct keyword)");
                        }
                    }
                break;
                case BRACKET:
                    if(bracketStack.isEmpty()||(bracketStack.size()==1
                            &&tokens.get(i).tokenType ==ParserTokenType.COMMA)){
                        if(bracketStack.size()>0||tokenBuffer.size()>0){
                            typeBuffer.add(typeFromTokens(context, tokenBuffer, null));
                        }
                        tokenBuffer.clear();
                        if(bracketStack.isEmpty()){
                            if(i+2<tokens.size()&&tokens.get(i+1).tokenType ==ParserTokenType.MAPS_TO
                                    &&tokens.get(i+2).tokenType ==ParserTokenType.OPTION){
                                tokens.remove(i+2);
                                tokens.remove(i+1);
                                tokens.set(i,new TypeToken(
                                        new Type.Proc(typeBuffer.toArray(new Type[0])),tokens.get(i).pos));
                                typeBuffer.clear();
                                state=TypeParserState.ROOT;
                            }else if(typeBuffer.size()==1){
                               tokens.set(i,new TypeToken(typeBuffer.get(0),tokens.get(i).pos));
                               typeBuffer.clear();
                                state=TypeParserState.ROOT;
                            }else{
                                throw new SyntaxError("invalid syntax for bracket:" +
                                        "expected '('<Type>')' or '('<Type>(','<Type>)*')=>?'");
                            }
                        }else{
                            tokens.remove(i--);
                        }
                    }else {
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
                case STRUCT_TYPE:
                    if(bracketStack.size()==1&&
                            (tokens.get(i).tokenType ==ParserTokenType.SEPARATOR)){
                        typeBuffer.add(typeFromTokens(context, tokenBuffer, null));
                        tokenBuffer.clear();
                        state=TypeParserState.STRUCT_NAME;
                        tokens.remove(i--);
                    }else{
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
                case STRUCT_NAME:
                    if(bracketStack.size()==1&&tokens.get(i).tokenType ==ParserTokenType.WORD){
                        nameBuffer.add(((NamedToken)tokens.remove(i)).value);
                        if(tokens.get(i).tokenType==ParserTokenType.CLOSE_CR_BRACKET){
                            if(bracketStack.removeLast()!=ParserTokenType.OPEN_CR_BRACKET){
                                throw new SyntaxError("mismatched bracket");
                            }
                            if(isUnion){
                                tokens.set(i,new TypeToken(new Type.Union(structName,typeBuffer.toArray(new Type[0]),
                                        nameBuffer.toArray(new String[0])),tokens.get(i).pos));
                            }else{
                                tokens.set(i,new TypeToken(new Type.Struct(structName,typeBuffer.toArray(new Type[0]),
                                        nameBuffer.toArray(new String[0])),tokens.get(i).pos));
                            }
                            nameBuffer.clear();
                            typeBuffer.clear();
                            state=TypeParserState.ROOT;
                        }else if (tokens.remove(i--).tokenType==ParserTokenType.COMMA){
                            state=TypeParserState.STRUCT_TYPE;
                        }else{
                            throw new SyntaxError("invalid syntax for struct entry:" +
                                    "expected <Type>':'<Name>','");
                        }
                    }else{
                        throw new SyntaxError(
                                "unexpected token for struct-entry name:\""+ tokens.get(i)+
                                        "\" expected Word");
                    }
                    break;
                case TUPLE:
                    if(bracketStack.size()==0||(bracketStack.size()==1&&
                            (tokens.get(i).tokenType ==ParserTokenType.COMMA))){
                        typeBuffer.add(typeFromTokens(context, tokenBuffer, null));
                        tokenBuffer.clear();
                        if(bracketStack.size()==0){
                            tokens.set(i,new TypeToken(new Type.Tuple(structName,typeBuffer.toArray(new Type[0])),
                                    tokens.get(i).pos));
                        }else{
                            tokens.remove(i--);
                        }
                    }else{
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
            }

        }
        //3. parse optionals and arrays
        // <Type>'?'
        // <Type>'[]'
        for(int i=0;i<tokens.size()-1;i++){
            if(tokens.get(i).tokenType ==ParserTokenType.TYPE&&
                    tokens.get(i+1).tokenType==ParserTokenType.OPTION){
                tmp=((TypeToken)tokens.get(i)).type;
                tokens.remove(i+1);
                tokens.set(i,new TypeToken(new Type.Optional(tmp),tokens.get(i).pos));
                i--;
            }else if(tokens.get(i).tokenType ==ParserTokenType.TYPE&&
                    tokens.get(i+1).tokenType ==ParserTokenType.OPEN_SQ_BRACKET){
                tmp=((TypeToken)tokens.get(i)).type;
                tokens.remove(i+1);
                if(i+1<tokens.size()&&tokens.get(i+1).tokenType ==ParserTokenType.CLOSE_SQ_BRACKET){//Array
                    tokens.remove(i+1);
                    tokens.set(i,new TypeToken(new Type.Array(tmp),tokens.get(i).pos));
                    i--;
                }else{
                    throw new SyntaxError("Illegal Syntax for Array: expected " +
                            "<Type>'[]'or <Type>'['<Type>']'");
                }
            }
        }
        //4. parse references
        // '@'<Type>
        for(int i=tokens.size()-1;i>0;i--){
            if(tokens.get(i).tokenType ==ParserTokenType.TYPE&&
                    tokens.get(i-1).tokenType ==ParserTokenType.AT){
                TypeToken tmpToken=((TypeToken)tokens.remove(i));
                tokens.set(i-1,new TypeToken(new Type.Reference(tmpToken.type),tmpToken.pos));
            }
        }
        if(tokens.size()!=1||tokens.get(0).tokenType !=ParserTokenType.TYPE){
            throw new SyntaxError("Invalid syntax for Type:"+tokens);
        }else{
            return ((TypeToken)tokens.get(0)).type;
        }
    }
    private void readTypeDef(Tokenizer tokens,ParserContext context) throws IOException {
        ParserToken token= tokens.getNextToken();
        if(token.tokenType !=ParserTokenType.WORD){
            throw new SyntaxError("Illegal token-type for type-name:\""+token.tokenType.toString()+
                    "\" expected \""+ParserTokenType.WORD+"\" or identifier");
        }
        String name=((NamedToken)token).value;
        ArrayList<ParserToken> typeTokens=new ArrayList<>();
        while((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.END){
            typeTokens.add(token);
        }
        context.defType(name,typeFromTokens(context,typeTokens, null));
    }

    private Expression expressionFromTokens(String procName,Type.Proc procType,ParserContext context, ArrayList<ParserToken> tokens){
        //1. read brackets
        // (<Expr>)
        // (<Type>:)
        // {<Expr>,<Expr>,<Expr>}
        // {'.'<Name>'='<Expr>,'.'<Name>'='<Expr>,'.'<Name>'='<Expr>}
        //'['<Expr>']'
        //'['<Expr>':'<Expr>?']'
        //'['':'<Expr>']'
        //addLater? [._] [_.] popFirst,popLast
        ArrayDeque<ParserTokenType> bracketStack=new ArrayDeque<>();
        ArrayList<ParserToken> tokenBuffer=new ArrayList<>();
        ArrayList<Expression> exprBuffer=new ArrayList<>();
        ArrayList<String> nameBuffer=new ArrayList<>();
        ExpressionParserState state=ExpressionParserState.ROOT;
        for(int i=0;i<tokens.size();i++){
            updateBracketStack(tokens.get(i), bracketStack);
            switch (state){
                case ROOT:
                    if(bracketStack.size()>0){
                        if(bracketStack.peekLast()==ParserTokenType.OPEN_BRACKET){
                            tokens.remove(i--);
                            state=ExpressionParserState.BRACKET;
                        }else if(bracketStack.peekLast()==ParserTokenType.OPEN_SQ_BRACKET){
                            tokens.remove(i--);
                            state=ExpressionParserState.INDEX;
                        }else if(bracketStack.peekLast()==ParserTokenType.OPEN_CR_BRACKET){
                            tokens.remove(i--);
                            if(i+1<tokens.size()&&tokens.get(i+1).tokenType==ParserTokenType.DOT){
                                state=ExpressionParserState.STRUCT_NAME;
                            }else{
                                state=ExpressionParserState.ARRAY;
                            }
                        }
                    }
                    break;
                case BRACKET:
                    if(bracketStack.isEmpty()){
                        if(tokenBuffer.size()>0&&
                                tokenBuffer.get(tokenBuffer.size()-1).tokenType==ParserTokenType.SEPARATOR){
                            tokenBuffer.remove(tokenBuffer.size()-1);//(<Type>:) -> type-cast
                            tokens.set(i,new TypeToken(typeFromTokens(context,tokenBuffer, null),tokens.get(i).pos));
                        }else{//(<Expr>) -> <Expr>
                            tokens.set(i,new ExprToken(expressionFromTokens(procName,procType,context,tokenBuffer),
                                    tokens.get(i).pos));
                        }
                        tokenBuffer.clear();
                        state=ExpressionParserState.ROOT;
                    }else{
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
                case ARRAY://'{'<Expr>(','<Expr>)*'}'
                    if(bracketStack.isEmpty()||
                            (bracketStack.size()==1&&tokens.get(i).tokenType==ParserTokenType.COMMA)){
                        if (bracketStack.size()>0||tokenBuffer.size()>0||exprBuffer.size()>0) {
                            exprBuffer.add(expressionFromTokens(procName,procType,context,tokenBuffer));
                            tokenBuffer.clear();
                        }
                        if(bracketStack.isEmpty()){
                            tokens.set(i,new ExprToken(InitStructOrArray.newTuple(exprBuffer),tokens.get(i).pos));
                            state=ExpressionParserState.ROOT;
                        }else{
                            tokens.remove(i--);
                        }
                    }else{
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
                case STRUCT_NAME://'.'<ID>'='
                    if(i+2<tokens.size()&&tokens.remove(i).tokenType==ParserTokenType.DOT&&
                            tokens.get(i).tokenType==ParserTokenType.WORD&&
                            tokens.remove(i+1).tokenType==ParserTokenType.ASSIGN){
                        nameBuffer.add(((NamedToken)tokens.remove(i--)).value);
                        state=ExpressionParserState.STRUCT_VALUE;
                    }else{
                        throw new SyntaxError("Invalid syntax for " +
                                "struct entry name expected: . <ID> = ");
                    }
                    break;
                case STRUCT_VALUE:// <Expr> ',' or <Expr> END_OF_BRACKET
                    if(bracketStack.isEmpty()||
                            (bracketStack.size()==1&&tokens.get(i).tokenType==ParserTokenType.COMMA)){
                        exprBuffer.add(expressionFromTokens(procName,procType,context,tokenBuffer));
                        tokenBuffer.clear();
                        if(bracketStack.isEmpty()){
                            tokens.set(i,new ExprToken(InitStructOrArray.newStruct(exprBuffer,nameBuffer),
                                    tokens.get(i).pos));
                            state=ExpressionParserState.ROOT;
                        }else{
                            tokens.remove(i--);
                            state=ExpressionParserState.STRUCT_NAME;
                        }
                    }else{
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
                case INDEX:
                    if(bracketStack.isEmpty()){
                        Expression index=expressionFromTokens(procName,procType,context,tokenBuffer);
                        tokenBuffer.clear();
                        tokens.set(i,new ExprToken(true,index,tokens.get(i).pos));
                        state=ExpressionParserState.ROOT;
                    }else if(bracketStack.size()==1&&tokens.get(i).tokenType==ParserTokenType.SEPARATOR){
                        tokens.remove(i--);
                        exprBuffer.add(tokenBuffer.isEmpty()?null:
                                expressionFromTokens(procName,procType,context,tokenBuffer));
                        tokenBuffer.clear();
                        state=ExpressionParserState.RANGE;
                    }else{
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
                case RANGE:
                    //addLater change syntax for range operators:
                    // [._],[_.] popFirst/Last
                    // ['>'<Expr>],['<'<Expr>]
                    // [<Expr>:<Expr>]
                    if(bracketStack.isEmpty()){
                        Expression right=tokenBuffer.isEmpty()?null:
                                expressionFromTokens(procName,procType,context,tokenBuffer);
                        tokenBuffer.clear();
                        tokens.set(i,new ExprToken(//placeholder
                                Value.createPrimitive(Type.Numeric.INT32,0), null, tokens.get(i).pos));
                        //TODO create Range access operator from right and exprBuffer
                        state=ExpressionParserState.ROOT;
                    }else if(bracketStack.size()==1&&tokens.get(i).tokenType==ParserTokenType.SEPARATOR){
                        throw new SyntaxError("invalid syntax for range expected " +
                                "[<Expr>:<Expr>]");
                    }else{
                        tokenBuffer.add(tokens.remove(i--));
                    }
                    break;
            }
        }
        //2. read Values/Vars
        int id;
        Expression tmpL,tmpR;
        for(int i=0;i<tokens.size();i++){
            if(tokens.get(i).tokenType == ParserTokenType.WORD&&
                    (i==0||(tokens.get(i-1).tokenType !=ParserTokenType.DOT))){
                String name = ((NamedToken) tokens.get(i)).value;
                id=context.getVarId(name);
                if(id<0){
                    if(name.equals(procName)){
                        tokens.set(i,new ExprToken(new ThisExpr(procType),tokens.get(i).pos));
                    }else {
                        Value v=context.getConst(name);
                        if(v==null){
                            v=context.getProc(name);
                        }//no else
                        if(v!=null){
                            tokens.set(i,new ExprToken(v, name, tokens.get(i).pos));
                        }else{
                            throw new SyntaxError("Unknown Identifier: \""+name+"\" at "+tokens.get(i).pos);
                        }
                    }
                }else {
                    tokens.set(i, new ExprToken(new VarExpression(context.getVarType(id), id),tokens.get(i).pos));
                }
            }
        }
        //3. access operators . [.]
        for(int i=1;i<tokens.size();i++){
            if(tokens.get(i-1).tokenType==ParserTokenType.EXPRESSION){
                if(tokens.get(i).tokenType==ParserTokenType.DOT&&
                tokens.get(i+1).tokenType==ParserTokenType.WORD){
                    String fieldName=((NamedToken)tokens.remove(i+1)).value;
                    tokens.set(i-1,new ExprToken(GetField.create(
                            ((ExprToken)tokens.get(i-1)).expr,
                            fieldName,context),tokens.get(i-1).pos));
                    tokens.remove(i--);
                }else if(tokens.get(i).tokenType==ParserTokenType.INDEX){
                    tokens.set(i-1,new ExprToken(GetIndex.create(
                            ((ExprToken)tokens.get(i-1)).expr,
                            ((ExprToken)tokens.get(i)).expr),tokens.get(i-1).pos));
                    tokens.remove(i--);
                }else if(tokens.get(i).tokenType==ParserTokenType.RANGE){
                    //TODO Range
                    throw new UnsupportedOperationException("unimplemented");
                }
            }else if(tokens.get(i-1).tokenType==ParserTokenType.TYPE&&
                tokens.get(i).tokenType==ParserTokenType.EXPRESSION){//typecast
                tokens.set(i-1,new ExprToken(TypeCast.create(
                        ((TypeToken)tokens.get(i-1)).type,
                        ((ExprToken)tokens.get(i)).expr, context),tokens.get(i-1).pos));
                tokens.remove(i--);
            }
        }
        //3b. replace remaining TypeCasts with TypeValues
        for(int i=0;i<tokens.size();i++){
            if(tokens.get(i).tokenType==ParserTokenType.TYPE){//typecast
                tokens.set(i,new ExprToken(new Value.TypeValue(((TypeToken)tokens.get(i)).type),null,tokens.get(i).pos));
            }
        }
        //4. unary operators: left: + - ~ !
        for(int i=tokens.size()-2;i>=0;i--){
            if(tokens.get(i+1).tokenType==ParserTokenType.EXPRESSION&&
                    (i==0||tokens.get(i-1).tokenType!=ParserTokenType.EXPRESSION)){
                if(tokens.get(i).tokenType==ParserTokenType.OPERATOR){
                    if(((Operator)tokens.get(i)).opType==OperatorType.PLUS){
                        tokens.set(i, tokens.remove(i+1));
                    }else if(((Operator)tokens.get(i)).opType==OperatorType.MINUS||
                            ((Operator)tokens.get(i)).opType==OperatorType.FLIP||
                            ((Operator)tokens.get(i)).opType==OperatorType.NOT
                    ){
                        tmpL=((ExprToken)tokens.remove(i+1)).expr;
                        tokens.set(i,new ExprToken(LeftUnaryOp.create(((Operator)tokens.get(i)).opType,tmpL),tokens.get(i).pos));
                    }
                }
            }
        }
        //5. binary operators
        // **
        for(int i=tokens.size()-2;i>0;i--){
            if(tokens.get(i).tokenType==ParserTokenType.OPERATOR&&
                    ((Operator)tokens.get(i)).opType==OperatorType.POW&&
                    tokens.get(i+1).tokenType==ParserTokenType.EXPRESSION&&
                    tokens.get(i-1).tokenType==ParserTokenType.EXPRESSION){
                tmpL=((ExprToken)tokens.get(i-1)).expr;
                tmpR=((ExprToken)tokens.remove(i+1)).expr;
                tokens.remove(i--);
                tokens.set(i,new ExprToken(BinOp.create(tmpL,OperatorType.POW,tmpR),tokens.get(i).pos));
            }
        }
        for(int level=0;level<=5;level++){
            //0: / % //
            //1: + -
            //2: >> <<
            //3:  & | ^
            //4: && ||
            //5: == <= < != > =>
            for(int i=1;i< tokens.size()-1;i++){
                if(tokens.get(i).tokenType==ParserTokenType.OPERATOR&&
                        tokens.get(i+1).tokenType==ParserTokenType.EXPRESSION&&
                        tokens.get(i-1).tokenType==ParserTokenType.EXPRESSION){
                    if((level==0&&(
                        (((Operator)tokens.get(i)).opType==OperatorType.DIV||
                        ((Operator)tokens.get(i)).opType==OperatorType.MULT||
                        ((Operator)tokens.get(i)).opType==OperatorType.INT_DIV)
                    ))||(level==1&&(
                        ((Operator)tokens.get(i)).opType==OperatorType.PLUS||
                        ((Operator)tokens.get(i)).opType==OperatorType.MINUS
                    ))||(level==2&&(
                        ((Operator)tokens.get(i)).opType==OperatorType.LSHIFT||
                        ((Operator)tokens.get(i)).opType==OperatorType.RSHIFT
                    ))||(level==3&&(
                        ((Operator)tokens.get(i)).opType==OperatorType.AND||
                        ((Operator)tokens.get(i)).opType==OperatorType.OR||
                        ((Operator)tokens.get(i)).opType==OperatorType.XOR
                    ))||(level==4&&(
                        ((Operator)tokens.get(i)).opType==OperatorType.FAST_AND||
                        ((Operator)tokens.get(i)).opType==OperatorType.FAST_OR
                    ))||(level==5&&(
                        ((Operator)tokens.get(i)).opType==OperatorType.EQ||
                        ((Operator)tokens.get(i)).opType==OperatorType.LE||
                        ((Operator)tokens.get(i)).opType==OperatorType.LT||
                        ((Operator)tokens.get(i)).opType==OperatorType.NE||
                        ((Operator)tokens.get(i)).opType==OperatorType.GT||
                        ((Operator)tokens.get(i)).opType==OperatorType.GE
                    ))){
                        tmpL=((ExprToken)tokens.get(i-1)).expr;
                        tmpR=((ExprToken)tokens.remove(i+1)).expr;
                        OperatorType opType=((Operator)tokens.remove(i--)).opType;
                        tokens.set(i,new ExprToken(BinOp.create(tmpL,opType,tmpR),tokens.get(i).pos));
                    }
                }
            }
        }
        //5. trinary operator ?:
        for(int i=tokens.size()-1;i>=4;i--){
            if(tokens.get(i-4).tokenType==ParserTokenType.EXPRESSION&&
                tokens.get(i-3).tokenType==ParserTokenType.OPTION&&
                tokens.get(i-2).tokenType==ParserTokenType.EXPRESSION&&
                tokens.get(i-1).tokenType==ParserTokenType.SEPARATOR&&
                tokens.get(i).tokenType==ParserTokenType.EXPRESSION){
                tokens.set(i-4,new ExprToken(IfExpr.create(((ExprToken)tokens.get(i-4)).expr,
                        ((ExprToken)tokens.get(i-2)).expr,((ExprToken)tokens.get(i)).expr,context),tokens.get(i-4).pos));
                tokens.remove(i--);//i
                tokens.remove(i);//i-1
                tokens.remove(i-1);//i-2
                tokens.remove(i-2);//i-3
                if(i>tokens.size()){
                    i=tokens.size();
                }
            }
        }
        if(tokens.size()!=1||tokens.get(0).tokenType!=ParserTokenType.EXPRESSION){
            throw new SyntaxError("Invalid syntax for Expression:"+tokens);
        }
        return ((ExprToken)tokens.get(0)).expr;
    }

    private void readConstDef(Tokenizer tokens,ParserContext context) throws IOException{
        ArrayList<ParserToken> tokenBuffer=new ArrayList<>();
        Type constType;
        ParserToken token;
        ArrayDeque<ParserTokenType> bracketStack=new ArrayDeque<>();
        while((token=tokens.getNextToken())!=null&&(bracketStack.size()>0||
                token.tokenType!=ParserTokenType.SEPARATOR)){
            updateBracketStack(token,bracketStack);
            tokenBuffer.add(token);
        }
        constType=typeFromTokens(context,tokenBuffer, null);
        tokenBuffer.clear();
        if((token=tokens.getNextToken())==null||token.tokenType!=ParserTokenType.WORD){
            throw new SyntaxError("invalid syntax for const definition, expected const <Type>:<Name>=<Expr>;");
        }
        String constName=((NamedToken)token).value;
        if((token=tokens.getNextToken())==null||token.tokenType!=ParserTokenType.ASSIGN){
            throw new SyntaxError("invalid syntax for const definition, expected const <Type>:<Name>=<Expr>;");
        }
        while((token=tokens.getNextToken())!=null&&token.tokenType!=ParserTokenType.END){
            tokenBuffer.add(token);
        }
        if(token==null){
            throw new SyntaxError("unfinished const expression");
        }
        context.addConstant(constName,expressionFromTokens(null,null,context,tokenBuffer)
                .evaluate(null,new ArrayList<>()).get().castTo(constType));
    }

    //addLater? update types of values depending on maximum range of parameters
    // i.e. don't use type any for values that are always an integer
    //addLater detect variables with predictable value
    private void readProcDef(String name,Tokenizer tokens,ParserContext context) throws IOException {
        ParserToken token;
        ArrayDeque<ParserTokenType> bracketStack=new ArrayDeque<>();
        //1. '('
        if((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.OPEN_BRACKET){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
        }
        bracketStack.addLast(ParserTokenType.OPEN_BRACKET);
        //2. <Type> (',' <Type>)* ')'
        ArrayList<Type> argTypes=new ArrayList<>();
        ArrayList<ParserToken> tokenBuffer=new ArrayList<>();
        while(bracketStack.size()>0&&(token=tokens.getNextToken())!=null){
            updateBracketStack(token,bracketStack);
            if(bracketStack.isEmpty()||(bracketStack.size()==1&&
                    token.tokenType ==ParserTokenType.COMMA)){
                if(bracketStack.size()>0||tokenBuffer.size()>0){//ignore empty buffer only at end
                    argTypes.add(typeFromTokens(context,tokenBuffer, null));
                }
                tokenBuffer.clear();
            }else{
                tokenBuffer.add(token);
            }
        }
        Type.Proc procType=new Type.Proc(argTypes.toArray(new Type[0]));
        if(token==null){
            throw new SyntaxError("Unexpected end of file");
        }
        //3. ':' '('
        if((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.SEPARATOR){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
            }
        if((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.OPEN_BRACKET){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
        }
        //4. <Name> (',' <Name>)* ')'
        ArrayList<String> argNames=new ArrayList<>(argTypes.size());
        boolean wasWord=false;
        while((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.CLOSE_BRACKET){
            if(token.tokenType ==ParserTokenType.COMMA){
                if(wasWord){
                    wasWord=false;
                }else{
                    throw new SyntaxError(" syntax for ArgNames: Expected <Name>(','<Name>)* ");
                }
            }else if(token.tokenType ==ParserTokenType.WORD){
                if(wasWord){
                    throw new SyntaxError(" syntax for ArgNames: Expected <Name>(','<Name>)* ");
               }else{
                    argNames.add(((NamedToken)token).value);
                    if(argNames.size()>argTypes.size()){
                        throw new SyntaxError(" too much arguments for procedure expected: "+argTypes.size());
                    }
                    Type type = argTypes.get(argNames.size() - 1);
                    context.declareVariable(((NamedToken)token).value,type);
                    wasWord=true;
                }
            }else{
                throw new SyntaxError("wrong syntax for ArgNames: Expected <Name>(','<Name>)* ");
            }
        }
        if(argNames.size()!=argTypes.size()){
            throw new SyntaxError("Number of arguments Types ("+argTypes.size()
                    +") does not match number of arguments ("+argNames.size()+")");
        }
        //5. '=>' '{'
        if((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.MAPS_TO){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
        }
        if((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.OPEN_CR_BRACKET){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
        }
        //6. (<Action>';')* '}'
        ArrayList<Action> actions=new ArrayList<>();
        tokenBuffer.clear();
        //<Assign-Expr>'=' <Expr>;      -> Assignment
        //<Type>':' <name> '=' <Expr>;  -> VarDeclaration
        ActionParserState state=ActionParserState.ROOT;
        Type defType=null;
        String defName=null;
        Expression assignTarget=null;
        bracketStack.clear();
        LogType logType=null;
        TokenPosition assertPos=null;
        while((token=tokens.getNextToken())!=null&&
                (bracketStack.size()>0||token.tokenType !=ParserTokenType.CLOSE_CR_BRACKET)){
            updateBracketStack(token,bracketStack);
            switch (state){
                case ROOT:
                    if(bracketStack.isEmpty()&&token.tokenType==ParserTokenType.END){
                        if(tokenBuffer.size()>0){
                            throw new SyntaxError("Unexpected semicolon");
                        }
                    }else if(bracketStack.isEmpty()&&
                            token.tokenType==ParserTokenType.SEPARATOR){
                        defType=typeFromTokens(context,tokenBuffer, null);
                        tokenBuffer.clear();
                        token=tokens.getNextToken();
                        if(token==null||token.tokenType!=ParserTokenType.WORD){
                            throw new SyntaxError("Illegal Name for procedure name:" +
                                    token+" expected: Word");
                        }
                        defName=((NamedToken)token).value;
                        token=tokens.getNextToken();
                        if(token==null||token.tokenType!=ParserTokenType.ASSIGN){
                            throw new SyntaxError("Illegal syntax for procedure definition " +
                                    "expected <Type>:<Name>=<Expr>");
                        }
                        state=ActionParserState.DEF_EXPR;
                    }else if(bracketStack.isEmpty()&&
                            token.tokenType==ParserTokenType.ASSIGN){
                        assignTarget=expressionFromTokens(name,procType,context,tokenBuffer);
                                                tokenBuffer.clear();
                        state=ActionParserState.ASSIGN_EXPR;
                    }else if(tokenBuffer.isEmpty()&&bracketStack.isEmpty()&&
                            token.tokenType==ParserTokenType.WORD&&
                            (((NamedToken)token).value.equals("log")||((NamedToken)token).value.equals("_log"))){
                        boolean append=((NamedToken)token).value.startsWith("_");
                        token= tokens.getNextToken();
                        if(token==null){
                            throw new SyntaxError("unexpected end of file");
                        }
                        if(token.tokenType==ParserTokenType.DOT){
                            token=tokens.getNextToken();
                            if(token==null){
                                throw new SyntaxError("unexpected end of file");
                            }else if(token.tokenType!=ParserTokenType.WORD){
                                throw new SyntaxError("invalid log-type:"+token);
                            }
                            switch (((NamedToken)token).value){
                                case "err"://log.err    ->  (stderr) errors
                                    logType=new LogType(append,LogType.Type.ERR);
                                    break;
                                case "debug"://log.debug  ->  debug statements
                                    logType=new LogType(append,LogType.Type.DEBUG);
                                    break;
                                case "info"://log.info   ->  info statements
                                    logType=new LogType(append,LogType.Type.INFO);
                                    break;
                                default:
                                    throw new SyntaxError("invalid log-type:"+((NamedToken)token).value);
                            }
                        }else{
                            updateBracketStack(token,bracketStack);
                            tokenBuffer.add(token);
                            logType=new LogType(append,LogType.Type.DEFAULT);
                            //mode -> log
                        }
                        state=ActionParserState.LOG;
                    }else if(tokenBuffer.isEmpty()&&bracketStack.isEmpty()&&token.tokenType==ParserTokenType.WORD&&
                            ((NamedToken)token).value.equals("assert")){
                        assertPos=token.pos;
                        token= tokens.getNextToken();
                        if(token==null){
                            throw new SyntaxError("unexpected end of file");
                        }
                        updateBracketStack(token,bracketStack);
                        tokenBuffer.add(token);
                        if(token.tokenType==ParserTokenType.EXPRESSION&&
                                ((ExprToken)token).expr instanceof ValueExpression&&
                                ((ExprToken)token).expr.expectedType() instanceof Type.NoRetString){
                            defName=((ValueExpression) ((ExprToken) token).expr).getValue().stringValue();
                            token=tokens.getNextToken();
                            if(token==null){
                                throw new SyntaxError("unexpected end of file");
                            }else if(token.tokenType!=ParserTokenType.SEPARATOR){
                                defName=null;
                                updateBracketStack(token,bracketStack);
                                tokenBuffer.add(token);
                            }else{
                                tokenBuffer.clear();
                            }
                        }
                        state=ActionParserState.ASSERT;
                    }else{//addLater exit, if-else, match
                        /*
                            if <expr> :
                            <if-body>
                            elif <expr> :
                            <elif-body>
                            else:
                            <else-body>
                            end

                            match <expr>:
                            <const>:
                            <case-body>
                            break;
                            <const>:
                            <case-body>
                            break;
                            default:
                            <case-body>
                            end
                        * */
                        tokenBuffer.add(token);
                    }
                    break;
                case ASSIGN_EXPR:
                    if(bracketStack.isEmpty()&&token.tokenType==ParserTokenType.END){
                        Expression expr=expressionFromTokens(name,procType,context,tokenBuffer);
                        if(!Type.canAssign(assignTarget.expectedType(),expr.expectedType(),null)){
                            throw new TypeError("cannot assign " +expr.expectedType()+ " to "+assignTarget.expectedType());
                        }else if(!assignTarget.canAssignTo()){
                            throw new TypeError("cannot assign values to immutable value"+assignTarget);
                        }
                        actions.add(new Assignment(assignTarget,expr,context));
                        tokenBuffer.clear();
                        state=ActionParserState.ROOT;
                    }else{
                        tokenBuffer.add(token);
                    }
                    break;
                case DEF_EXPR:
                    if(bracketStack.isEmpty()&&token.tokenType==ParserTokenType.END){
                        Expression expr=expressionFromTokens(name,procType,context,tokenBuffer);
                        actions.add(new ValDef(defType,expr,context));
                        context.declareVariable(defName,defType);
                        tokenBuffer.clear();
                        state=ActionParserState.ROOT;
                    }else{
                        tokenBuffer.add(token);
                    }
                    break;
                case LOG:
                    if(bracketStack.isEmpty()&&token.tokenType==ParserTokenType.END){
                        Expression expr=expressionFromTokens(name,procType,context,tokenBuffer);
                        actions.add(new LogAction(logType,expr,context));
                        tokenBuffer.clear();
                        state=ActionParserState.ROOT;
                    }else{
                        tokenBuffer.add(token);
                    }
                    break;
                case ASSERT:
                    if(bracketStack.isEmpty()&&token.tokenType==ParserTokenType.END){
                        Expression expr=TypeCast.create(Type.Primitive.BOOL,
                                expressionFromTokens(name,procType,context,tokenBuffer),context);
                        if(expr instanceof ValueExpression){//compile time assert
                            if(!(Boolean)((Value.Primitive)((ValueExpression) expr).getValue()).getValue()){
                                throw new SyntaxError("assertion failed: "+assertPos);
                            }
                        }else{
                            actions.add(new Assertion(expr,defName));
                            tokenBuffer.clear();
                            state=ActionParserState.ROOT;
                        }
                    }else{
                        tokenBuffer.add(token);
                    }
                    break;
            }
        }
        if(state!=ActionParserState.ROOT){
            throw new SyntaxError("Unfinished Action");
        }
        //7. '=>' '['
        if((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.MAPS_TO){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
        }
        if((token=tokens.getNextToken())!=null&&token.tokenType !=ParserTokenType.OPEN_SQ_BRACKET){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
        }
        //8. '<Name>'('<Name>(,<Name>)*')' (','<Name>'('<Name>(,<Name>)*')')*' ']'
        ArrayList<Procedure.ProcChild> targets=new ArrayList<>();
        ArrayList<Expression[]> args=new ArrayList<>();
        ArrayList<Expression> argBuffer;
        Expression[] argArray;
        Type[] outTypes;
        Procedure proc;
        String procName;
        tokenBuffer.clear();
        do{
            token=tokens.getNextToken();
            if(token==null||token.tokenType !=ParserTokenType.WORD){
                if(token!=null&&token.tokenType==ParserTokenType.CLOSE_SQ_BRACKET){
                    break;//end of proc block
                }
                throw new SyntaxError("illegal syntax for procedure-call: "+token+" " +
                        "expected: <Name>'('<Args>')'");
            }
            procName=((NamedToken)token).value;
            proc= context.getProc(procName);
            if(proc==null){
                int id=context.getVarId(procName);
                if(id>=0&&context.getVarType(id) instanceof Type.Proc){
                   targets.add(new Procedure.DynamicProcChild(id, false));
                   outTypes=((Type.Proc)context.getVarType(id)).getArgTypes();
                }else if(id>=0&&context.getVarType(id) instanceof Type.Optional&&
                        ((Type.Optional) context.getVarType(id)).content instanceof Type.Proc){
                    targets.add(new Procedure.DynamicProcChild(id, true));
                    outTypes=((Type.Proc)((Type.Optional)context.getVarType(id)).content).getArgTypes();
                }else if(procName.equals(name)){
                    targets.add(Procedure.RECURSIVE_CALL);
                    outTypes=argTypes.toArray(new Type[0]);
                }else{//addLater allow names of procedures that are declared later in the file
                    throw new SyntaxError("procedure \"" + ((NamedToken) token).value + "\" is not defined");
                }
            }else{
                outTypes=proc.argTypes();
                targets.add(new Procedure.StaticProcChild(procName, proc));
            }
            token=tokens.getNextToken();
            if(token==null||token.tokenType !=ParserTokenType.OPEN_BRACKET){
                throw new SyntaxError("illegal syntax for procedure-call: "+token+" " +
                        "expected: <Name>'('<Args>')'");
            }
            argBuffer=new ArrayList<>(outTypes.length);
            bracketStack.clear();
            bracketStack.add(ParserTokenType.OPEN_BRACKET);
            while((token=tokens.getNextToken())!=null){
                updateBracketStack(token,bracketStack);
                if(bracketStack.size()==0||(bracketStack.size()==1&&(token.tokenType==ParserTokenType.COMMA))){
                    if(bracketStack.size()>0||tokenBuffer.size()>0) {
                        argBuffer.add(expressionFromTokens(name, procType, context, tokenBuffer));
                    }
                    tokenBuffer.clear();
                    if(bracketStack.size()==0){
                        break;
                    }
                }else{
                    tokenBuffer.add(token);
                }
            }
            if(token==null){
                throw new SyntaxError("missing closing bracket in proc-call");
            }
            if(argBuffer.size()!=outTypes.length){
                throw new SyntaxError("Number of arguments Arguments for \""+procName+
                        "\" ("+argBuffer.size()+") does not match the expected number of " +
                        "arguments ("+outTypes.length+")");
            }
            argArray=new Expression[argBuffer.size()];
            HashMap<String, Type.GenericBound> generics=new HashMap<>();
            for(int i=0;i< argBuffer.size();i++){
                //Type-check parameters
                if(Type.canAssign(outTypes[i],argBuffer.get(i).expectedType(),generics)){
                    //ensure arguments are cast to correct type addLater cast contents of generics to correct type
                    argArray[i]=TypeCast.create(outTypes[i],argBuffer.get(i), context);
                }else{
                    throw new TypeError("Cannot assign "+ argBuffer.get(i).expectedType()+" to "+outTypes[i]+" generics:"+generics);
                }
            }
            args.add(argArray);
        }while((token=tokens.getNextToken())!=null&&token.tokenType ==ParserTokenType.COMMA);
        if(token==null||token.tokenType !=ParserTokenType.CLOSE_SQ_BRACKET){
            throw new SyntaxError("wrong syntax for procedure, expected:" +
                    " <name>(<Types>):(<ArgNames>) => {<Actions>} => [<Procedures>]");
        }
        proc = new Procedure(procType,actions.toArray(new Action[0]),
                targets.toArray(new Procedure.ProcChild[0]),
                args.toArray(new Expression[0][]), context.varCount());
        context.declareProcedure(name, proc);
    }


    public void parse(Reader input) throws IOException {
        Tokenizer tokens=new Tokenizer(input);
        ParserToken token;
        while((token=tokens.getNextToken())!=null){
            if(token.tokenType ==ParserTokenType.WORD){
                if(((NamedToken)token).value.equals("typedef")){
                    readTypeDef(tokens,context);
                }else if(((NamedToken)token).value.equals("const")){
                    readConstDef(tokens,context);
                }else if(!context.hasName(((NamedToken)token).value)){
                    readProcDef(((NamedToken)token).value,tokens,context);
                }else{
                    throw new SyntaxError("\""+token+"\" is already defined");
                }
            }else if(token.tokenType==ParserTokenType.STRUCT_DEFINITION){
                ArrayList<ParserToken> tokenBuffer=new ArrayList<>();
                tokenBuffer.add(token);
                String name;
                if((token=tokens.getNextToken())==null||token.tokenType!=ParserTokenType.WORD){
                    throw new SyntaxError("unexpected start of struct:"+token+" expected: <struct-type> <struct-name>{<content>}");
                }
                name=((NamedToken)token).value;
                if((token=tokens.getNextToken())==null||token.tokenType!=ParserTokenType.OPEN_CR_BRACKET){
                    throw new SyntaxError("unexpected start of struct:"+token+" expected: <struct-type> <struct-name>{<content>}");
                }
                tokenBuffer.add(token);
                int c=1;
                while((token=tokens.getNextToken())!=null&&c>0){
                    tokenBuffer.add(token);
                    if(token.tokenType==ParserTokenType.OPEN_CR_BRACKET){
                        c++;
                    }else if(token.tokenType==ParserTokenType.CLOSE_CR_BRACKET){
                        c--;
                    }
                }
                if(c>0){
                    throw new SyntaxError("unfinished struct definition "+tokenBuffer.get(0));
                }
                context.defType(name, typeFromTokens(context, tokenBuffer, name));
            }else{
              throw new SyntaxError("Illegal root level token:\""+token+
                      "\" expected \"typedef\" or identifier");
            }
        }
    }

    public void compile(BufferedWriter output) throws IOException {
        (new CompileToC(output)).compile(context);
    }

    public Procedure interpret(){
        Procedure start=context.getProc("start");
        if(start==null){
            throw new SyntaxError("No \"start\" procedure found");
        }
        Type[] startTypes= start.argTypes();
        if(startTypes.length>0){
            if (startTypes.length != 1 || !(startTypes[0] instanceof Type.Array) ||
                    ((Type.Array) startTypes[0]).content != Type.NoRetString.STRING8) {
                throw new SyntaxError("wrong signature of start, " +
                        "expected ()=>? or (string[])=>?");
            }
        }
        return start;
    }

}
