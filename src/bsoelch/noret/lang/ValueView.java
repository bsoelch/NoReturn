package bsoelch.noret.lang;

import bsoelch.noret.SyntaxError;

public interface ValueView {
    Value get();
    void set(Value newValue);

    static ValueView wrap(Value v){
        return new ConstValue(v);
    }
    class ConstValue implements ValueView{
        final Value value;

        public ConstValue(Value value) {
            this.value = value;
        }

        @Override
        public Value get() {
            return value;
        }

        @Override
        public void set(Value newValue) {
            throw new SyntaxError("Unable to modify const Value");
        }
    }
}
