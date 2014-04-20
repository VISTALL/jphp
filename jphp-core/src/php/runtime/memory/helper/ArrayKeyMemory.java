package php.runtime.memory.helper;

import php.runtime.memory.ArrayMemory;
import php.runtime.memory.LongMemory;
import php.runtime.memory.ReferenceMemory;
import php.runtime.memory.StringMemory;
import php.runtime.Memory;

public class ArrayKeyMemory extends ReferenceMemory {
    private ArrayMemory array;

    public ArrayKeyMemory(ArrayMemory array, Memory key) {
        super(key);
        this.array = array;
    }

    @Override
    public Memory assign(Memory memory) {
        array.renameKey(value, memory);
        return super.assign(memory);
    }

    @Override
    public Memory assign(long memory) {
        array.renameKey(value, LongMemory.valueOf(memory));
        return super.assign(memory);
    }

    @Override
    public Memory assign(CharSequence memory) {
        array.renameKey(value, new StringMemory(memory));
        return super.assign(memory);
    }

    @Override
    public Memory assign(boolean memory) {
        return assign(memory ? 1 : 0);
    }

    @Override
    public Memory assign(double memory) {
        return assign((long)memory);
    }
}
