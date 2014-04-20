package php.runtime.util;

/**
 * @author VISTALL
 * @since 07.04.14
 */
public class EmptyCharSequence implements CharSequence{
    public static final EmptyCharSequence INSTANCE = new EmptyCharSequence();

    @Override
    public int length() {
        return 0;
    }

    @Override
    public char charAt(int index) {
        return 0;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return "";
    }
}
