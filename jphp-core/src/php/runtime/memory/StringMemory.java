package php.runtime.memory;

import php.runtime.Memory;
import php.runtime.OperatorUtils;
import php.runtime.env.TraceInfo;
import php.runtime.util.CharSequenceByChar;
import php.runtime.util.CharSequenceUtil;
import php.runtime.util.EmptyCharSequence;

public class StringMemory extends Memory {
    CharSequence value = EmptyCharSequence.INSTANCE;

    protected final static StringMemory[] CACHED_CHARS;

    public StringMemory(CharSequence value) {
        super(Type.STRING);
        this.value = value;
    }

    public StringMemory(char ch){
        this(new CharSequenceByChar(ch));
    }

    public static Memory valueOf(CharSequence value){
        if (CharSequenceUtil.isEmpty(value))
            return Memory.CONST_EMPTY_STRING;

        if (value.length() == 1) {
            return getChar(value.charAt(0));
        }

        return new StringMemory(value);
    }

    @Override
    public long toLong() {
        return toLongNumeric().toLong();
    }

    @Override
    public double toDouble() {
        return toNumeric().toDouble();
    }

    @Override
    public boolean toBoolean() {
        return !CharSequenceUtil.isOneCharEquals(value, '0');
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public CharSequence toCharSequence() {
        return value;
    }

    public static Memory toLong(CharSequence value){
        int len = value.length();
        if (len == 0)
            return null;

        int i = 0;
        int start = i;
        boolean neg = false;
        for(; i < len; i++){
            char ch = value.charAt(i);
            if (!('9' >= ch && ch >= '0')){
                if (ch == '-'){
                    if (i == start) {
                        neg = true;
                        continue;
                    }
                }
                return null;
            }
            neg = false;
        }
        if (neg)
            return null;

        return LongMemory.valueOf(CharSequenceUtil.parseLong(value));
    }

    public static Memory toNumeric(CharSequence value){
        return toNumeric(value, false, CONST_INT_0);
    }

    public Memory toLongNumeric(){
        return toNumeric(value, true, CONST_INT_0);
    }

    public static Memory toNumeric(CharSequence value, boolean onlyInt, Memory def){
        int len = value.length();
        boolean real = false;
        int i = 0;
        for(; i < len; i++){
            char ch = value.charAt(i);
            if (ch > 32)
                break;
        }

        int start = i;
        boolean canSign = true;
        boolean e_char = false;

        for(; i < len; i++){
            char ch = value.charAt(i);
            if (!('9' >= ch && ch >= '0')){
                if (canSign && (ch == '-' || ch == '+')){
                    canSign = false;
                    // fix bug for OpenJDK 1.6 because cannot convert +1234 numbers
                    if (start == i && ch == '+')
                        start += 1;
                    continue;
                }

                if (!e_char && i != start && ((ch == 'e' || ch == 'E') && value.charAt(i - 1) != '.')){
                    if (onlyInt)
                        break;

                    e_char = true;
                    real = true;
                    canSign = true;
                    continue;
                }

                canSign = false;
                if (ch == '.'){
                    if (real || onlyInt)
                        break;
                    real = true;
                    continue;
                }
                if (i == start || (value.charAt(i - 1) == '-' || value.charAt(i - 1) == '+'))
                    return def;
                else
                    break;
            }
            canSign = false;
        }
        if (real) {
            if (len == i && start == 0)
                return new DoubleMemory(Double.parseDouble(value.toString()));
            else
                return new DoubleMemory(Double.parseDouble(value.subSequence(start, i).toString()));
        } else {
            if (len == 0)
                return def;

            if (len == i && start == 0){
                return LongMemory.valueOf(CharSequenceUtil.parseLong(value));
            } else {
                return LongMemory.valueOf(CharSequenceUtil.parseLong(value.subSequence(start, i)));
            }
        }
    }

    @Override
    public Memory toNumeric(){
        return toNumeric(value);
    }

    @Override
    public Memory inc() {
        CharSequence str = toCharSequence();
        if (CharSequenceUtil.isEmpty(str))
            return CONST_INT_1;

        char ch = str.charAt(str.length() - 1);
        Memory ret = toNumeric(str, false, null);

        if (ret == null || !Character.isDigit(ch)){

            StringBuilder sb = new StringBuilder();

            int length = str.length(), i = length - 1;
            int type = -1;

            while (i >= -1){
                if (i >= 0)
                    ch = str.charAt(i);
                else {
                    switch (type){
                        case 0: sb.append('0'); break;
                        case 1: sb.append('a'); break;
                        case 2: sb.append('A'); break;
                    }
                    break;
                }

                if (ch >= '0' && ch <= '9') type = 0;
                else if (ch >= 'a' && ch <= 'z') type = 1;
                else if (ch >= 'A' && ch <= 'Z') type = 2;
                else {
                    i += 1;
                    break;
                }

                ch++;
                boolean next = false;
                switch (type){
                    case 0: if (ch > '9') {
                        ch = '0';
                        next = true;
                    } break;
                    case 1: if (ch > 'z') {
                        ch = 'a';
                        next = true;
                    } break;
                    case 2: if (ch > 'Z') {
                        ch = 'A';
                        next = true;
                    }
                }
                if (type > -1)
                    sb.insert(0, ch);

                if (!next)
                    break;

                i--;
            }

            if (i > 0)
                sb.insert(0, str.subSequence(0, i));

            return new StringMemory(sb);
        }

        return ret.inc();
    }

    @Override
    public Memory dec() {
        CharSequence str = toCharSequence();
        Memory ret = toNumeric(str, false, null);
        // hack for php
        if (ret == null || CharSequenceUtil.isEmpty(str) || !Character.isDigit(str.charAt(str.length() - 1)))
            return this;
        return ret.dec();
    }

    @Override
    public Memory negative() {
        return toNumeric().negative();
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

    @Override
    public Memory div(long value) {
        return toNumeric().div(value);
    }

    @Override
    public Memory div(boolean value) {
        return div(value ? 1 : 0);
    }

    @Override
    public Memory div(double value) {
        return toNumeric().div(value);
    }

    @Override
    public Memory div(CharSequence value) {
        return toNumeric().div(value);
    }

    @Override
    public boolean identical(Memory memory) {
        return memory.getRealType() == Type.STRING && toCharSequence().equals(memory.toCharSequence());
    }

    @Override
    public boolean equal(Memory memory) {
        switch (memory.type){
            case NULL: return CharSequenceUtil.isEmpty(value);
            case DOUBLE:
            case INT: return toNumeric().equal(memory);
            case STRING: return toCharSequence().equals(memory.toCharSequence());
            case OBJECT:
            case ARRAY: return false;
            default: return equal(memory.toValue());
        }
    }

    @Override
    public boolean equal(long value) {
        return toNumeric().equal(value);
    }

    @Override
    public boolean equal(double value) {
        return toNumeric().equal(value);
    }

    @Override
    public boolean equal(CharSequence value) {
        return CharSequenceUtil.equals(this.value, value);
    }

    @Override
    public boolean notEqual(Memory memory) {
        return !equal(memory);
    }

    @Override
    public Memory minus(long value) {
        return toNumeric().minus(value);
    }

    @Override
    public CharSequence concat(Memory memory) {
        switch (memory.type){
            case STRING: return CharSequenceUtil.concat(this.value, memory.toCharSequence());
            case REFERENCE: return concat(memory.toImmutable());
            default:
                return CharSequenceUtil.concat(this.value, memory.toCharSequence());
        }
    }

    @Override
    public boolean smaller(Memory memory) {
        switch (memory.type){
            case STRING: return CharSequenceUtil.compareTo(value, memory.toCharSequence()) < 0;
            case REFERENCE: return smaller(memory.toImmutable());
        }
        return toNumeric().smaller(memory);
    }

    @Override
    public boolean smaller(CharSequence value) {
        return CharSequenceUtil.compareTo(this.value, value) < 0;
    }

    @Override
    public boolean smallerEq(Memory memory) {
        switch (memory.type){
            case STRING: return CharSequenceUtil.compareTo(value, memory.toCharSequence()) <= 0;
            case REFERENCE: return smaller(memory.toImmutable());
        }
        return toNumeric().smallerEq(memory);
    }

    @Override
    public boolean smallerEq(CharSequence value) {
        return CharSequenceUtil.compareTo(this.value, value) <= 0;
    }

    @Override
    public boolean greater(Memory memory) {
        switch (memory.type){
            case STRING: return CharSequenceUtil.compareTo(value, memory.toCharSequence()) > 0;
            case REFERENCE: return smaller(memory.toImmutable());
        }
        return toNumeric().greater(memory);
    }

    @Override
    public boolean greater(CharSequence value) {
        return CharSequenceUtil.compareTo(this.value, value) > 0;
    }

    @Override
    public boolean greaterEq(Memory memory) {
        switch (memory.type){
            case STRING: return CharSequenceUtil.compareTo(value, memory.toCharSequence()) >= 0;
            case REFERENCE: return smaller(memory.toImmutable());
        }
        return toNumeric().greaterEq(memory);
    }

    @Override
    public boolean greaterEq(CharSequence value) {
        return CharSequenceUtil.compareTo(this.value, value) >= 0;
    }

    @Override
    public CharSequence concat(CharSequence value) {
        return CharSequenceUtil.concat(this.value, value);
    }

    @Override
    public Memory bitAnd(Memory memory) {
        if(memory.isString())
            return OperatorUtils.binaryAnd(this, memory);
        else
            return super.bitAnd(memory);
    }

    @Override
    public Memory bitAnd(CharSequence memory) {
        return OperatorUtils.binaryAnd(this, new StringMemory(memory));
    }

    @Override
    public Memory bitOr(Memory memory) {
        if(memory.isString())
            return OperatorUtils.binaryOr(this, memory);
        else
            return super.bitOr(memory);
    }

    @Override
    public Memory bitOr(CharSequence memory) {
        return OperatorUtils.binaryOr(this, new StringMemory(memory));
    }

    @Override
    public Memory bitXor(Memory memory) {
        if(memory.isString())
            return OperatorUtils.binaryXor(this, memory);
        else
            return super.bitXor(memory);
    }

    @Override
    public Memory bitXor(CharSequence memory) {
        return OperatorUtils.binaryXor(this, new StringMemory(memory));
    }

    @Override
    public Memory bitNot() {
        /*Memory value = toNumeric(toString(), null);
        if (value != null)
            return value.bitNot();*/

        return OperatorUtils.binaryNot(this);
    }

    @Override
    public Memory bitShr(Memory memory) {
        /*if(memory.isString())
            return OperatorUtils.binaryShr(this, memory);
        else*/
            return toLongNumeric().bitShr(memory);
    }

    @Override
    public Memory bitShr(CharSequence memory) {
        return toLongNumeric().bitShr(memory);
        //return OperatorUtils.binaryShr(this, new StringMemory(memory));
    }

    @Override
    public Memory bitShl(Memory memory) {
        /*if(memory.isString())
            return OperatorUtils.binaryShl(this, memory);
        else */
            return toLongNumeric().bitShl(memory);
    }

    @Override
    public Memory bitShl(CharSequence memory) {
        return toLongNumeric().bitShl(memory);
        //return OperatorUtils.binaryShl(this, new StringMemory(memory));
    }

    @Override
    public Memory bitShrRight(String value) {
        return toNumeric(value, true, CONST_INT_0).bitShr(this);
        //return OperatorUtils.binaryShr(new StringMemory(value), this);
    }

    @Override
    public Memory bitShlRight(String value) {
        return toNumeric(value, true, CONST_INT_0).bitShl(this);
        //return OperatorUtils.binaryShl(new StringMemory(value), this);
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, Memory index) {
        int _index = -1;

        switch (index.type){
            case STRING:
                Memory tmp = StringMemory.toLong(index.toCharSequence());
                if (tmp != null)
                    _index = tmp.toInteger();
                break;
            case REFERENCE: return valueOfIndex(index.toValue());
            default:
                _index = index.toInteger();
        }

        if (_index < toCharSequence().length() && _index >= 0)
            return getChar(value.charAt(_index));
        else
            return CONST_EMPTY_STRING;
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, long index) {
        int _index = (int)index;
        CharSequence string = toCharSequence();
        if (_index >= 0 && _index < string.length())
            return getChar(string.charAt(_index));
        else
            return CONST_EMPTY_STRING;
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, double index) {
        int _index = (int)index;
        CharSequence string = toCharSequence();
        if (_index >= 0 && _index < string.length())
            return getChar(string.charAt(_index));
        else
            return CONST_EMPTY_STRING;
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, boolean index) {
        int _index = index ? 1 : 0;
        CharSequence string = toCharSequence();
        if (_index >= 0 && _index < string.length())
            return getChar(string.charAt(_index));
        else
            return CONST_EMPTY_STRING;
    }

    @Override
    public Memory valueOfIndex(TraceInfo trace, String index) {
        int _index = -1;

        Memory tmp = StringMemory.toLong(index);
        if (tmp != null)
            _index = tmp.toInteger();

        CharSequence string = toCharSequence();
        if (_index >= 0 && _index < string.length())
            return getChar(string.charAt(_index));
        else
            return CONST_EMPTY_STRING;
    }

    @Override
    public byte[] getBinaryBytes() {
        return toString().getBytes();
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

    protected static StringMemory getChar(char ch) {
        int i = (int)ch;
        if (i >= CACHED_CHARS.length)
            return new StringMemory(ch);
        else
            return CACHED_CHARS[i];
    }

    static {
        CACHED_CHARS = new StringMemory[256];
        for(int i = 0; i < CACHED_CHARS.length; i++) {
            CACHED_CHARS[i] = new StringMemory((char)i);
        }
    }
}
