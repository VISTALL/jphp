package php.runtime.memory;

import php.runtime.common.Messages;
import php.runtime.common.Modifier;
import php.runtime.exceptions.CriticalException;
import php.runtime.exceptions.support.ErrorType;
import php.runtime.env.Environment;
import php.runtime.env.TraceInfo;
import php.runtime.lang.Closure;
import php.runtime.lang.ForeachIterator;
import php.runtime.lang.IObject;
import php.runtime.lang.Resource;
import php.runtime.lang.spl.ArrayAccess;
import php.runtime.lang.spl.iterator.Iterator;
import php.runtime.lang.spl.iterator.IteratorAggregate;
import php.runtime.lang.support.IComparableObject;
import php.runtime.Memory;
import php.runtime.memory.support.MemoryStringUtils;
import php.runtime.reflection.ClassEntity;
import php.runtime.reflection.PropertyEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class ObjectMemory extends Memory {

    public IObject value;

    public ObjectMemory() {
        super(Type.OBJECT);
    }

    public ObjectMemory(IObject object){
        this();
        this.value = object;
    }

    public static Memory valueOf(IObject object){
        if (object == null)
            return NULL;
        return new ObjectMemory(object);
    }

    public ClassEntity getReflection(){
        return value.getReflection();
    }

    public ArrayMemory getProperties(){
        return value.getProperties();
    }

    @Override
    public int getPointer(boolean absolute) {
        return value.getPointer();
    }

    @Override
    public int getPointer() {
        return value.getPointer();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean isResource() {
        return value instanceof Resource;
    }

    @Override
    public long toLong() {
        return 0;
    }

    @Override
    public double toDouble() {
        return 0;
    }

    @Override
    public boolean toBoolean() {
        return true;
    }

    @Override
    public Memory toNumeric() {
        return CONST_INT_0;
    }

    @Override
    public String toString() {
        ClassEntity entity = value.getReflection();
        if (entity.methodMagicToString != null){
            Environment env = value.getEnvironment();
            if (env == null)
                return "Object";

            // We can't get real trace info from toString method :(
            env.pushCall(
                    entity.methodMagicToString.getTrace(),
                    value, null, entity.methodMagicToString.getName(), entity.getName(), null
            );
            try {
                Memory result = entity.methodMagicToString.invokeDynamic(value, env, null);
                if (!result.isString()){
                    env.error(
                            ErrorType.E_RECOVERABLE_ERROR, "Method %s must return a string value",
                            entity.methodMagicToString.getSignatureString(false)
                    );
                    return "";
                }
                return result.toString();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e){
                throw new RuntimeException(e);
            } finally {
                env.popCall();
            }
        } else
            return "Object";
    }

    @Override
    public Memory inc() {
        return CONST_INT_1;
    }

    @Override
    public Memory dec() {
        return CONST_INT_M1;
    }

    @Override
    public Memory negative() {
        return CONST_INT_0;
    }

    @Override
    public Memory plus(Memory memory) {
        return toNumeric().plus(memory);
    }

    @Override
    public Memory minus(Memory memory) {
        return toNumeric().minus(memory);
    }

    @Override
    public Memory mul(Memory memory) {
        return toNumeric().mul(memory);
    }

    @Override
    public Memory div(Memory memory) {
        return toNumeric().div(memory);
    }

    private static interface Comparator {
        boolean compare(IObject o1, IObject o2);
    }

    private boolean compare(Memory other, Comparator comparator){
        switch (other.type){
            case OBJECT:
                ClassEntity otherReflection = ((ObjectMemory)other).getReflection();
                if (otherReflection.getId() != getReflection().getId())
                    return false;
                IObject otherObject = ((ObjectMemory)other).value;
                return comparator.compare(value, otherObject);
            case REFERENCE: return equal(other.toValue());
        }
        return false;
    }

    public int compare(IObject other, boolean strict, Set<Integer> used){
        ClassEntity otherReflection = other.getReflection();
        if (otherReflection.getId() != getReflection().getId())
            return -2;

        if (used == null)
            used = new HashSet<Integer>();


        try {
            if (used.add(other.getPointer()))
                return value.getProperties().compare(other.getProperties(), strict, used);

            return 0;
        } finally {
            used.remove(other.getPointer());
        }
    }

    @Override
    public boolean equal(Memory memory) {
        return compare(memory, new Comparator() {
            @Override
            public boolean compare(IObject o1, IObject o2) {
                if (o1 instanceof IComparableObject) return ((IComparableObject) o1).__equal(o2);

                return o1.getProperties().equal(o2.getProperties());
            }
        });
    }

    @Override
    public boolean notEqual(Memory memory) {
        return !equal(memory);
    }

    @Override
    public boolean smaller(Memory memory) {
        return compare(memory, new Comparator() {
            @Override
            public boolean compare(IObject o1, IObject o2) {
                if (o1 instanceof IComparableObject) return ((IComparableObject) o1).__smaller(o2);

                return o1.getProperties().smaller(o2.getProperties());
            }
        });
    }

    @Override
    public boolean smallerEq(Memory memory) {
        return compare(memory, new Comparator() {
            @Override
            public boolean compare(IObject o1, IObject o2) {
                if (o1 instanceof IComparableObject) return ((IComparableObject) o1).__smallerEq(o2);

                return o1.getProperties().smallerEq(o2.getProperties());
            }
        });
    }

    @Override
    public boolean greater(Memory memory) {
        return compare(memory, new Comparator() {
            @Override
            public boolean compare(IObject o1, IObject o2) {
                if (o1 instanceof IComparableObject) return ((IComparableObject) o1).__greater(o2);

                return o1.getProperties().greater(o2.getProperties());
            }
        });
    }

    @Override
    public boolean greaterEq(Memory memory) {
        return compare(memory, new Comparator() {
            @Override
            public boolean compare(IObject o1, IObject o2) {
                if (o1 instanceof IComparableObject) return ((IComparableObject) o1).__greaterEq(o2);

                return o1.getProperties().greaterEq(o2.getProperties());
            }
        });
    }

    @Override
    public byte[] getBinaryBytes() {
        return MemoryStringUtils.getBinaryBytes(this);
    }

    @Override
    public boolean identical(Memory memory) {
        return compare(memory, new Comparator() {
            @Override
            public boolean compare(IObject o1, IObject o2) {
                if (o1 instanceof IComparableObject) return ((IComparableObject) o1).__identical(o2);

                return o1.getPointer() == o2.getPointer();
            }
        });
    }

    @Override
    public boolean identical(long value) {
        return false;
    }

    @Override
    public boolean identical(double value) {
        return false;
    }

    @Override
    public boolean identical(boolean value) {
        return false;
    }

    @Override
    public boolean identical(CharSequence value) {
        return false;
    }

    @Override
    public ForeachIterator getNewIterator(final Environment env, boolean getReferences, boolean getKeyReferences) {
        if (value instanceof IteratorAggregate){
            return ((IteratorAggregate)value).getIterator(env).getNewIterator(env, getReferences, getKeyReferences);
        } else if (value instanceof Iterator){
            final Iterator iterator = (Iterator)value;
            final String className = value.getReflection().getName();

            return new ForeachIterator(getReferences, getKeyReferences, false) {
                private boolean keyInit = false;
                private boolean needNext = false;
                private boolean rewind = false;

                @Override
                protected boolean init() {
                    return rewind();
                }

                protected boolean rewind() {
                    if (!rewind){
                        env.pushCall(null, ObjectMemory.this.value, null, "rewind", className, null);
                        try {
                            return iterator.rewind(env).toValue() != FALSE;
                        } finally {
                            rewind = true;
                            env.popCall();
                        }
                    }
                    return true;
                }

                @Override
                protected boolean prevValue() {
                    return false;
                }

                @Override
                protected boolean nextValue() {
                    return true;
                }

                @Override
                public boolean next() {
                    if (!rewind())
                        return false;

                    boolean valid = false;
                    keyInit = false;
                    if (needNext){
                        env.pushCall(null, ObjectMemory.this.value, null, "next", className, null);
                        try {
                            iterator.next(env);
                        } finally {
                            env.popCall();
                        }
                    }

                    needNext = true;
                    env.pushCall(null, ObjectMemory.this.value, null, "valid", className, null);
                    try {
                        valid = iterator.valid(env).toBoolean();
                        if (valid) {
                            env.pushCall(null, ObjectMemory.this.value, null, "current", className, null);
                            try {
                                currentValue = iterator.current(env);
                                if (!getReferences)
                                    currentValue = currentValue.toImmutable();
                            } finally {
                                env.popCall();
                            }
                        } else {
                            rewind = false;
                        }
                    } finally {
                        env.popCall();
                    }

                    return valid;
                }

                @Override
                public Object getKey() {
                    return getMemoryKey();
                }

                @Override
                public Memory getMemoryKey() {
                    if (keyInit)
                        return (Memory)currentKey;

                    env.pushCall(null, ObjectMemory.this.value, null, "key", className, null);
                    try {
                        currentKey = iterator.key(env).toImmutable();
                        keyInit = true;
                        return (Memory)currentKey;
                    } finally {
                        env.popCall();
                    }
                }
            };
        } else {
            return new ForeachIterator(getReferences, getKeyReferences, false) {
                private ForeachIterator child;
                private ClassEntity reflection;
                private ClassEntity context;

                @Override
                protected boolean init() {
                    context = env.getLastClassOnStack();
                    reflection = value.getReflection();
                    child = value.getProperties().foreachIterator(getReferences, getKeyReferences);
                    return true;
                }

                @Override
                protected boolean nextValue() {
                    while (true){
                        if (!child.next())
                            return false;

                        Object key = child.getKey();
                        if (key instanceof String){
                            String keyS = (String)key;
                            int pos = keyS.lastIndexOf('\0');
                            if (pos > -1) keyS = keyS.substring(pos + 1);

                            PropertyEntity entity = reflection.isInstanceOf(context)
                                    ? context.properties.get(keyS) : reflection.properties.get(keyS);

                            int accessFlag = entity == null ? 0 : entity.canAccess(env);
                            if (accessFlag == 0){
                                currentKey = entity == null ? keyS : entity.getName();
                                break;
                            } else
                                continue;
                        }
                        break;
                    }

                    //currentKey = child.getKey();
                    currentValue = child.getValue();
                    return true;
                }

                @Override
                protected boolean prevValue() {
                    return false;
                }
            };
        }
    }

    @Override
    public boolean instanceOf(String className, String lowerClassName) {
        ClassEntity origin = value.getReflection();
        return origin.isInstanceOfLower(lowerClassName);
    }

    @Override
    public boolean instanceOf(String name) {
        return instanceOf(name, name.toLowerCase());
    }

    @Override
    public Memory toObject(Environment env) {
        return this;
    }

    @Override
    public Memory clone(Environment env, TraceInfo trace) throws Throwable {
        return new ObjectMemory(value.getReflection().cloneObject(value, env, trace));
    }

    @Override
    public Memory toArray() {
        ArrayMemory result = new ArrayMemory();
        ArrayMemory props = value.getProperties();
        ForeachIterator iterator = props.foreachIterator(false, false);
        ClassEntity reflection = value.getReflection();

        while (iterator.next()){
            Object key = iterator.getKey();
            Memory value = iterator.getValue().toImmutable();

            if (key instanceof String){
                String keyS = (String)key;
                PropertyEntity prop = reflection.properties.get(keyS);

                if (prop == null || prop.getModifier() == Modifier.PUBLIC){
                    result.refOfIndex(keyS).assign(iterator.getValue().toImmutable());
                } else {
                    if (prop.getModifier() == Modifier.PROTECTED){
                        result.refOfIndex("\0*\0" + keyS).assign(value);
                    } else {
                        result.refOfIndex("\0" + prop.getClazz().getName() + "\0" + keyS).assign(value);
                    }
                }
            } else {
                result.refOfIndex(null, iterator.getMemoryKey()).assign(value);
            }
        }

        return result.toConstant();
    }

    @Override
    public void manualUnset(Environment env) {
        ClassEntity entity = value.getReflection();
        if (entity.methodDestruct != null) {
            if (!value.isFinalized()) {
                value.doFinalize();
                env.pushCall(value, entity.methodDestruct.getName());
                try {
                    entity.methodDestruct.invokeDynamic(value, env);
                } catch (InvocationTargetException e){
                    env.__throwException(e);
                } catch (RuntimeException e){
                    throw e;
                } catch (Throwable throwable) {
                    throw new CriticalException(throwable);
                } finally {
                    env.popCall();
                }
            }
        }
    }

    private void invalidUseAsArray(TraceInfo trace){
        Environment env = value.getEnvironment();
        if (env != null){
            env.error(trace == null ? getReflection().getTrace() : trace, ErrorType.E_ERROR,
                    Messages.ERR_CANNOT_USE_OBJECT_AS_ARRAY, getReflection().getName()
            );
        }
    }

    @Override
    public Memory refOfIndex(final TraceInfo trace, final Memory index) {
        if (value instanceof ArrayAccess){
            return new ReferenceMemory(){
                @Override
                public Memory assign(Memory memory) {
                    Environment env = ObjectMemory.this.value.getEnvironment();
                    if (env != null && trace != null){
                        ArrayAccess array = (ArrayAccess)ObjectMemory.this.value;
                        Memory[] args = new Memory[]{ index, memory };
                        env.pushCall(ObjectMemory.this.value, "offsetSet", args);
                        try {
                            array.offsetSet(env, args);
                        } finally {
                            env.popCall();
                        }
                    } else
                        invalidUseAsArray(trace);
                    return memory;
                }

                @Override
                public Memory assignRef(Memory reference) {
                    return assign(reference);
                }

                @Override
                public Memory assign(long memory) {
                    return assign(LongMemory.valueOf(memory));
                }

                @Override
                public Memory assign(CharSequence memory) {
                    return assign(StringMemory.valueOf(memory));
                }

                @Override
                public Memory assign(boolean memory) {
                    return assign(TrueMemory.valueOf(memory));
                }

                @Override
                public Memory assign(double memory) {
                    return assign(DoubleMemory.valueOf(memory));
                }

                @Override
                public Memory assignConcat(Memory memory) {
                    return assign(toValue().concat(memory));
                }

                @Override
                public Memory assignConcat(long memory) {
                    return assign(toValue().concat(memory));
                }

                @Override
                public Memory assignConcat(double memory) {
                    return assign(toValue().concat(memory));
                }

                @Override
                public Memory assignConcat(boolean memory) {
                    return assign(toValue().concat(memory));
                }

                @Override
                public Memory assignConcat(CharSequence memory) {
                    return assign(toValue().concat(memory));
                }

                @Override
                public Memory assignPlus(Memory memory) {
                    value = toValue();
                    return super.assignPlus(memory);
                }

                @Override
                public Memory assignPlus(long memory) {
                    value = toValue();
                    return super.assignPlus(memory);
                }

                @Override
                public Memory assignPlus(double memory) {
                    value = toValue();
                    return super.assignPlus(memory);
                }

                @Override
                public Memory assignPlus(boolean memory) {
                    value = toValue();
                    return super.assignPlus(memory);
                }

                @Override
                public Memory assignPlus(CharSequence memory) {
                    value = toValue();
                    return super.assignPlus(memory);
                }

                @Override
                public Memory assignPlusRight(Memory memory) {
                    value = toValue();
                    return super.assignPlusRight(memory);
                }

                @Override
                public Memory assignMinus(Memory memory) {
                    value = toValue();
                    return super.assignMinus(memory);
                }

                @Override
                public Memory assignMinus(long memory) {
                    value = toValue();
                    return super.assignMinus(memory);
                }

                @Override
                public Memory assignMinus(double memory) {
                    value = toValue();
                    return super.assignMinus(memory);
                }

                @Override
                public Memory assignMinus(boolean memory) {
                    value = toValue();
                    return super.assignMinus(memory);
                }

                @Override
                public Memory assignMinus(CharSequence memory) {
                    value = toValue();
                    return super.assignMinus(memory);
                }

                @Override
                public Memory assignMinusRight(Memory memory) {
                    value = toValue();
                    return super.assignMinusRight(memory);
                }

                @Override
                public Memory assignMul(Memory memory) {
                    value = toValue();
                    return super.assignMul(memory);
                }

                @Override
                public Memory assignMul(long memory) {
                    value = toValue();
                    return super.assignMul(memory);
                }

                @Override
                public Memory assignMul(double memory) {
                    value = toValue();
                    return super.assignMul(memory);
                }

                @Override
                public Memory assignMul(boolean memory) {
                    value = toValue();
                    return super.assignMul(memory);
                }

                @Override
                public Memory assignMul(CharSequence memory) {
                    value = toValue();
                    return super.assignMul(memory);
                }

                @Override
                public Memory assignMulRight(Memory memory) {
                    value = toValue();
                    return super.assignMulRight(memory);
                }

                @Override
                public Memory assignDiv(Memory memory) {
                    value = toValue();
                    return super.assignDiv(memory);
                }

                @Override
                public Memory assignDiv(long memory) {
                    value = toValue();
                    return super.assignDiv(memory);
                }

                @Override
                public Memory assignDiv(double memory) {
                    value = toValue();
                    return super.assignDiv(memory);
                }

                @Override
                public Memory assignDiv(boolean memory) {
                    value = toValue();
                    return super.assignDiv(memory);
                }

                @Override
                public Memory assignDiv(CharSequence memory) {
                    value = toValue();
                    return super.assignDiv(memory);
                }

                @Override
                public Memory assignDivRight(Memory memory) {
                    value = toValue();
                    return super.assignDivRight(memory);
                }

                @Override
                public Memory assignMod(Memory memory) {
                    value = toValue();
                    return super.assignMod(memory);
                }

                @Override
                public Memory assignMod(long memory) {
                    value = toValue();
                    return super.assignMod(memory);
                }

                @Override
                public Memory assignMod(double memory) {
                    value = toValue();
                    return super.assignMod(memory);
                }

                @Override
                public Memory assignMod(boolean memory) {
                    value = toValue();
                    return super.assignMod(memory);
                }

                @Override
                public Memory assignMod(CharSequence memory) {
                    value = toValue();
                    return super.assignMod(memory);
                }

                @Override
                public Memory assignModRight(Memory memory) {
                    value = toValue();
                    return super.assignModRight(memory);
                }

                @Override
                public Memory assignBitShr(Memory memory) {
                    value = toValue();
                    return super.assignBitShr(memory);
                }

                @Override
                public Memory assignBitShr(long memory) {
                    value = toValue();
                    return super.assignBitShr(memory);
                }

                @Override
                public Memory assignBitShr(double memory) {
                    value = toValue();
                    return super.assignBitShr(memory);
                }

                @Override
                public Memory assignBitShr(boolean memory) {
                    value = toValue();
                    return super.assignBitShr(memory);
                }

                @Override
                public Memory assignBitShr(CharSequence memory) {
                    value = toValue();
                    return super.assignBitShr(memory);
                }

                @Override
                public Memory assignBitShrRight(Memory memory) {
                    value = toValue();
                    return super.assignBitShrRight(memory);
                }

                @Override
                public Memory assignBitShl(Memory memory) {
                    value = toValue();
                    return super.assignBitShl(memory);
                }

                @Override
                public Memory assignBitShl(long memory) {
                    value = toValue();
                    return super.assignBitShl(memory);
                }

                @Override
                public Memory assignBitShl(double memory) {
                    value = toValue();
                    return super.assignBitShl(memory);
                }

                @Override
                public Memory assignBitShl(boolean memory) {
                    value = toValue();
                    return super.assignBitShl(memory);
                }

                @Override
                public Memory assignBitShl(CharSequence memory) {
                    value = toValue();
                    return super.assignBitShl(memory);
                }

                @Override
                public Memory assignBitShlRight(Memory memory) {
                    value = toValue();
                    return super.assignBitShlRight(memory);
                }

                @Override
                public Memory assignBitAnd(Memory memory) {
                    value = toValue();
                    return super.assignBitAnd(memory);
                }

                @Override
                public Memory assignBitAnd(long memory) {
                    value = toValue();
                    return super.assignBitAnd(memory);
                }

                @Override
                public Memory assignBitAnd(double memory) {
                    value = toValue();
                    return super.assignBitAnd(memory);
                }

                @Override
                public Memory assignBitAnd(boolean memory) {
                    value = toValue();
                    return super.assignBitAnd(memory);
                }

                @Override
                public Memory assignBitAnd(CharSequence memory) {
                    value = toValue();
                    return super.assignBitAnd(memory);
                }

                @Override
                public Memory assignBitAndRight(Memory memory) {
                    value = toValue();
                    return super.assignBitAndRight(memory);
                }

                @Override
                public Memory assignBitOr(Memory memory) {
                    value = toValue();
                    return super.assignBitOr(memory);
                }

                @Override
                public Memory assignBitOr(long memory) {
                    value = toValue();
                    return super.assignBitOr(memory);
                }

                @Override
                public Memory assignBitOr(double memory) {
                    value = toValue();
                    return super.assignBitOr(memory);
                }

                @Override
                public Memory assignBitOr(boolean memory) {
                    value = toValue();
                    return super.assignBitOr(memory);
                }

                @Override
                public Memory assignBitOr(CharSequence memory) {
                    value = toValue();
                    return super.assignBitOr(memory);
                }

                @Override
                public Memory assignBitOrRight(Memory memory) {
                    value = toValue();
                    return super.assignBitOrRight(memory);
                }

                @Override
                public Memory assignBitXor(Memory memory) {
                    value = toValue();
                    return super.assignBitXor(memory);
                }

                @Override
                public Memory assignBitXor(long memory) {
                    value = toValue();
                    return super.assignBitXor(memory);
                }

                @Override
                public Memory assignBitXor(double memory) {
                    value = toValue();
                    return super.assignBitXor(memory);
                }

                @Override
                public Memory assignBitXor(boolean memory) {
                    value = toValue();
                    return super.assignBitXor(memory);
                }

                @Override
                public Memory assignBitXor(String memory) {
                    value = toValue();
                    return super.assignBitXor(memory);
                }

                @Override
                public Memory assignBitXorRight(Memory memory) {
                    value = toValue();
                    return super.assignBitXorRight(memory);
                }

                @Override
                public Memory toValue() {
                    return ObjectMemory.this.valueOfIndex(trace, index);
                }

                @Override
                public ReferenceMemory getReference() {
                    Memory ret = toValue();
                    if (ret instanceof ReferenceMemory)
                        return (ReferenceMemory)ret;
                    else
                        return new ReferenceMemory();
                }

                @Override
                public Memory toImmutable() {
                    return toValue().toImmutable();
                }

                @Override
                public Memory inc() {
                    return toValue().inc();
                }

                @Override
                public Memory dec() {
                    return toValue().dec();
                }

                @Override
                public Memory valueOfIndex(TraceInfo trace, Memory index) {
                    return toValue().valueOfIndex(trace, index);
                }

                @Override
                public Memory valueOfIndex(TraceInfo trace, long index) {
                    return toValue().valueOfIndex(trace, index);
                }

                @Override
                public Memory valueOfIndex(TraceInfo trace, double index) {
                    return toValue().valueOfIndex(trace, index);
                }

                @Override
                public Memory valueOfIndex(TraceInfo trace, String index) {
                    return toValue().valueOfIndex(trace, index);
                }

                @Override
                public Memory valueOfIndex(TraceInfo trace, boolean index) {
                    return toValue().valueOfIndex(trace, index);
                }

                @Override
                public Memory refOfPush(TraceInfo trace) {
                    return toValue().refOfPush(trace);
                }

                @Override
                public Memory refOfIndexAsShortcut(TraceInfo trace, Memory index) {
                    return toValue().refOfIndexAsShortcut(trace, index);
                }

                @Override
                public Memory refOfIndex(TraceInfo trace, Memory index) {
                    return toValue().refOfIndex(trace, index);
                }

                @Override
                public Memory refOfIndex(TraceInfo trace, long index) {
                    return toValue().refOfIndex(trace, index);
                }

                @Override
                public Memory refOfIndex(TraceInfo trace, double index) {
                    return toValue().refOfIndex(trace, index);
                }

                @Override
                public Memory refOfIndex(TraceInfo trace, String index) {
                    return toValue().refOfIndex(trace, index);
                }

                @Override
                public Memory refOfIndex(TraceInfo trace, boolean index) {
                    return toValue().refOfIndex(trace, index);
                }

                @Override
                public void unsetOfIndex(TraceInfo trace, Memory index) {
                    toValue().unsetOfIndex(trace, index);
                }

                @Override
                public Memory issetOfIndex(TraceInfo trace, Memory index) {
                    return toValue().issetOfIndex(trace, index);
                }

                @Override
                public Memory emptyOfIndex(TraceInfo trace, Memory index) {
                    return toValue().emptyOfIndex(trace, index);
                }
            };
        } else {
            invalidUseAsArray(trace);
            return new ReferenceMemory();
        }
    }

    @Override
    public Memory refOfIndexAsShortcut(TraceInfo trace, Memory index) {
        return refOfIndex(trace, index);
    }

    @Override
    public Memory refOfIndex(TraceInfo trace, long index) {
        return refOfIndex(trace, LongMemory.valueOf(index));
    }

    @Override
    public Memory refOfIndex(TraceInfo trace, double index) {
        return refOfIndex(trace, DoubleMemory.valueOf(index));
    }

    @Override
    public Memory refOfIndex(TraceInfo trace, String index) {
        return refOfIndex(trace,StringMemory.valueOf(index));
    }

    @Override
    public Memory refOfIndex(TraceInfo trace, boolean index) {
        return refOfIndex(trace, index ? TRUE : FALSE);
    }

    @Override
    public Memory refOfPush(TraceInfo trace) {
        if (value instanceof ArrayAccess){
            return refOfIndex(trace, NULL);
        } else {
            invalidUseAsArray(trace);
            return new ReferenceMemory();
        }
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, Memory index) {
        if (value instanceof ArrayAccess){
            Environment env = value.getEnvironment();
            if (env != null && trace != null){
                Memory[] args = new Memory[] { index };
                env.pushCall(value, "offsetGet", args);
                try {
                    return ((ArrayAccess) value).offsetGet(env, args);
                } finally {
                    env.popCall();
                }
            } else {
                invalidUseAsArray(trace);
                return NULL;
            }
        } else {
            invalidUseAsArray(trace);
            return NULL;
        }
    }


    @Override
    public Memory valueOfIndex(TraceInfo trace, boolean index) {
        return valueOfIndex(trace, TrueMemory.valueOf(index));
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, long index) {
        return valueOfIndex(trace, LongMemory.valueOf(index));
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, double index) {
        return valueOfIndex(trace, DoubleMemory.valueOf(index));
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, String index) {
        return valueOfIndex(trace, StringMemory.valueOf(index));
    }

    @Override
    public void unsetOfIndex(TraceInfo trace, Memory index) {
        if (value instanceof ArrayAccess){
            Environment env = value.getEnvironment();
            if (env != null && trace != null){
                Memory[] args = new Memory[]{index};
                env.pushCall(value, "offsetUnset", args);
                try {
                    ((ArrayAccess) value).offsetUnset(env, args);
                } finally {
                    env.popCall();
                }
            } else
                invalidUseAsArray(trace);
        } else
            invalidUseAsArray(trace);
    }

    @Override
    public Memory emptyOfIndex(TraceInfo trace, Memory index) {
        return issetOfIndex(trace, index, true);
    }

    @Override
    public Memory issetOfIndex(TraceInfo trace, Memory index) {
        return issetOfIndex(trace, index, false);
    }

    private Memory issetOfIndex(TraceInfo trace, Memory index, boolean asEmpty) {
        if (value instanceof ArrayAccess){
            Environment env = value.getEnvironment();
            if (env != null && trace != null){
                Memory[] args = new Memory[]{index};
                env.pushCall(value, "offsetExists", args);
                try {
                    if (((ArrayAccess) value).offsetExists(env, args).toBoolean())
                        return asEmpty ? valueOfIndex(trace, index) : TRUE;
                    else
                        return NULL;
                } finally {
                    env.popCall();
                }
            } else {
                invalidUseAsArray(trace);
                return NULL;
            }
        } else {
            invalidUseAsArray(trace);
            return NULL;
        }
    }

    @Override
    public boolean isClosure() {
        return value instanceof Closure;
    }
}
