package bsoelch.noret.lang;

import java.util.ArrayList;

public class LogAction implements Action {
    private static LogType prevType =null;

    final LogType type;
    final Expression expr;
    public LogAction(LogType logType, Expression expr) {
        assert  logType!=null;
        this.type=logType;
        this.expr=expr;
    }

    private static void log(LogType type,String value){
        if(prevType !=null&&(prevType.type!=type.type||!type.append)){
            switch (prevType.type){//finish previous log
                case DEFAULT:
                case DEBUG:
                case INFO:
                    System.out.println();
                    break;
                case ERR:
                    System.err.println();
                    break;
            }
        }
        switch (type.type){
            case DEFAULT:
                System.out.print(value);
                break;
            case DEBUG:
                System.out.print((type.append?"":"Debug:")+value);
                break;
            case INFO:
                System.out.print((type.append?"":"Info:")+value);
                break;
            case ERR:
                System.err.print(value);
                break;
        }
        prevType = type;
    }

    @Override
    public void execute(Procedure parent, ArrayList<Value> context) {
        Value value = expr.evaluate(parent, context).get();
        log(type, value.getType() instanceof Type.NoRetString?value.stringValue():value.stringRepresentation());
    }

    @Override
    public String toString() {
        return "Log["+type+"]{" +expr +'}';
    }
}
