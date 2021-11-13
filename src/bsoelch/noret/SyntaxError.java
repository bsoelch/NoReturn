package bsoelch.noret;

public class SyntaxError extends Error{
    public SyntaxError(String message){
        super(message);
    }

    public SyntaxError(Throwable t) {
        super(t);
    }
}
