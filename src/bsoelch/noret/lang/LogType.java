package bsoelch.noret.lang;

public class LogType {

    public enum Type{
        DEFAULT, ERR, DEBUG, INFO
    }
    final Type type;
    final boolean append;
    public LogType(boolean append,Type type) {
        this.type = type;
        this.append = append;
    }

    @Override
    public String toString() {
        return (append?"_":"")+type;
    }
}
