package php.runtime.util;

/**
 * @author VISTALL
 * @since 07.04.14
 */
public class CharSequenceUtil {
    public static boolean isEmpty(CharSequence sequence) {
        return sequence == null || sequence.length() == 0;
    }

    public static boolean isOneCharEquals(CharSequence sequence, char c) {
        return !isEmpty(sequence) && sequence.length() == 1 && sequence.charAt(0) == c;
    }

    public static boolean equals(CharSequence thisString, CharSequence anotherString) {
        if (thisString.length() != anotherString.length()) {
            return false;
        }
        for(int i = 0; i < thisString.length(); i++) {
            if(thisString.charAt(i) != anotherString.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static CharSequence concat(CharSequence thisString, CharSequence anotherString) {
        if(isEmpty(thisString) && isEmpty(anotherString)) {
            return EmptyCharSequence.INSTANCE;
        }

        if(isEmpty(thisString)) {
            return anotherString;
        }
        else if(isEmpty(anotherString)) {
            return thisString;
        }
        return new MergedCharSequence(thisString, anotherString);
    }

    public static int compareTo(CharSequence thisString, CharSequence anotherString) {
        int len1 = thisString.length();
        int len2 = anotherString.length();
        int n = Math.min(len1, len2);

        int k = 0;
        while (k < n) {
            char c1 = thisString.charAt(k);
            char c2 = anotherString.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }

        return len1 - len2;
    }

    // copy from Long.parseLong
    public static long parseLong(CharSequence s) {
        return parseLong(s, 10);
    }

    public static long parseLong(CharSequence s, int radix)
            throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null");
        }

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix +
                    " less than Character.MIN_RADIX");
        }
        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix +
                    " greater than Character.MAX_RADIX");
        }

        long result = 0;
        boolean negative = false;
        int i = 0, len = s.length();
        long limit = -Long.MAX_VALUE;
        long multmin;
        int digit;

        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+')
                    throw new NumberFormatException("For input string: " + s);

                if (len == 1) // Cannot have lone "+" or "-"
                    throw new NumberFormatException("For input string: " + s);
                i++;
            }
            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0) {
                    throw new NumberFormatException("For input string: " + s);
                }
                if (result < multmin) {
                    throw new NumberFormatException("For input string: " + s);
                }
                result *= radix;
                if (result < limit + digit) {
                    throw new NumberFormatException("For input string: " + s);
                }
                result -= digit;
            }
        } else {
            throw new NumberFormatException("For input string: " + s);
        }
        return negative ? result : -result;
    }
}
