package bsoelch.noret;

import bsoelch.noret.lang.Expression;
import bsoelch.noret.lang.Procedure;
import bsoelch.noret.lang.Type;
import bsoelch.noret.lang.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ProgramContext {
    final HashMap<String, Type> typeNames = new HashMap<>();
    final HashMap<String, Procedure> procNames = new HashMap<>();
    final HashMap<String, Value> constants = new HashMap<>();

    final HashMap<String, Integer> varIds = new HashMap<>();
    final ArrayList<Type> varTypes = new ArrayList<>();
    final HashMap<Integer, Value> varValues = new HashMap<>();

    final HashSet<Type> runtimeTypes = new HashSet<>();
    final HashSet<Type> runtimeBlockTypes = new HashSet<>();
    final HashSet<Type.StructOrUnion> runtimeStructs = new HashSet<>();

    long maxArgSize = 0;

    public void declareProcedure(String name, Procedure proc) {
        procNames.put(name, proc);
        maxArgSize = Math.max(maxArgSize, ((Type.Proc) proc.getType()).argsBlockSize());
        varIds.clear();
        varValues.clear();
        varTypes.clear();
    }

    boolean hasName(String name) {
        return typeNames.containsKey(name) || procNames.containsKey(name) ||
                constants.containsKey(name) || varIds.containsKey(name);
    }

    /**
     * child types of containers that are needed at runtime,
     * this method is a helper of the compiler
     *
     * @param topLevel true if the supplied type is not contained in any other type
     */
    public void addRuntimeType(Type t, boolean topLevel) {
        if (t instanceof Type.Struct || t instanceof Type.Union) {
            runtimeStructs.add((Type.StructOrUnion) t);
        } else if (t instanceof Type.Tuple || t instanceof Type.Proc) {
            runtimeBlockTypes.add(t);
        } else if (!topLevel) {
            runtimeTypes.add(t);
        }
        //add child types
        for (Type c : t.childTypes()) {
            addRuntimeType(c, false);
        }
    }

    public void defType(String name, Type type) {
        if (hasName(name)) {
            throw new SyntaxError(name + " is already defined");
        } else {
            typeNames.put(name, type);
        }
    }

    public void declareVariable(String name, Type type, Expression initValue) {
        if (hasName(name)) {
            throw new SyntaxError(name + " is already defined");
        } else {
            if(initValue!=null&&initValue.hasValue(this)){
                varValues.put(varIds.size(),initValue.getValue(this).castTo(type));
            }
            varIds.put(name, varIds.size());
            varTypes.add(type);
        }
    }

    public Type getType(String name) {
        return typeNames.get(name);
    }

    public Procedure getProc(String name) {
        return procNames.get(name);
    }

    public int getVarId(String name) {
        Integer get = varIds.get(name);
        return get == null ? -1 : get;
    }
    public boolean hasVarValue(int varId) {
        return varValues.containsKey(varId);
    }
    public Value getVarValue(int varId) {
        Value v= varValues.get(varId);
        if(v==null){
            throw new RuntimeException("Var"+varId+" cannot be evaluated at compile time");
        }
        return v;
    }

    public Type getVarType(int id) {
        return varTypes.get(id);
    }

    public int varCount() {
        return varIds.size();
    }

    public void addConstant(String constName, Value constValue) {
        if (hasName(constName)) {
            throw new SyntaxError(constName + " is already defined");
        } else {
            constants.put(constName, constValue);
        }
    }

    public Value getConst(String constName) {
        return constants.get(constName);
    }

    public long maxArgSize() {
        return maxArgSize;
    }

}
