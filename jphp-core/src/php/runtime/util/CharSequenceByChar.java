package php.runtime.util;

/**
 * @author VISTALL
 * @since 07.04.14
 */
public class CharSequenceByChar implements CharSequence{
    private final char myChar;

    public CharSequenceByChar(char myChar) {
        this.myChar = myChar;
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public char charAt(int index) {
        if(index != 0) {
            throw new IllegalArgumentException();
        }
        return myChar;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return Character.toString(myChar);
    }
}
