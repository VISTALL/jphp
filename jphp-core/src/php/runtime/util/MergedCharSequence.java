package php.runtime.util;

/**
 * @author VISTALL
 * @since 07.04.14
 */
public class MergedCharSequence implements CharSequence {
    private CharSequence first;
    private CharSequence second;

    public MergedCharSequence(CharSequence first, CharSequence second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int length() {
        return first.length() + second.length();
    }

    @Override
    public char charAt(int index) {
        if(index >= first.length()) {
            return second.charAt(index);
        }
        return first.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        StringBuilder builder = new StringBuilder(length());
        builder.append(first);
        builder.append(second);
        return builder.subSequence(start, end);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(length());
        builder.append(first);
        builder.append(second);
        return builder.toString();
    }
}
