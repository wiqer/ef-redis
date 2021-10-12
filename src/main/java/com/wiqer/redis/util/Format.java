package com.wiqer.redis.util;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Format {
    static final  char[] EFNETBytes =new char[]{'0','1','2','3','4','5','6','7','8','9',
            'a','b','c','d','e','f','g','h','i','j','k','l',
            'm','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V',
            'W','X','Y','Z','$','~'};
    static final byte[] digits = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    static final byte[] DigitTens = new byte[]{'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'};
    static final byte[] DigitOnes = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public static int uint32(long z) {
       // if(z<0) z=-z;
        return (int)(z&0x7fffffff);
    }
    /**
     *  保留后n位
     */
    public static int uintNBit(long z,int n) {
        return (int)(z&((1<<n)-1));
    }
    public static String digits(int val, int digits) {
        char[] buf = new char[digits];
        do {
            buf[--digits] = EFNETBytes[val&63];
            val >>>= 6;
        }while (digits>0);
        return new String(buf);

    }
    public static String digits(Long val, int digits) {
        char[] buf = new char[digits];
        do {
            buf[--digits] = EFNETBytes[(int)(val&63)];
            val >>>= 6;
        }while (digits>0);
        return new String(buf);

    }

    public static long parseLong(byte[] content, int radix)
            throws NumberFormatException
    {
        /*
         * WARNING: This method may be invoked early during VM initialization
         * before IntegerCache is initialized. Care must be taken to not use
         * the valueOf method.
         */

        if (content == null) {
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
        int i = 0, len = content.length;
        int limit = -Integer.MAX_VALUE;
        int multmin;
        int digit;

        if (len > 0) {
            byte firstChar = content[0];
            if (firstChar < '0') {
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+') {
                    throw new NumberFormatException();
                }

                if (len == 1) // Cannot have lone "+" or "-"
                {
                    throw new NumberFormatException();
                }
                i++;
            }
            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(content[i++],radix);
                if (digit < 0) {
                    throw new NumberFormatException();
                }
                if (result < multmin) {
                    throw new NumberFormatException();
                }
                result *= radix;
                if (result < limit + digit) {
                    throw new NumberFormatException();
                }
                result -= digit;
            }
        } else {
            throw new NumberFormatException();
        }
        return negative ? result : -result;
    }
    public static byte[] toByteArray(long value) {
        if (value == -9223372036854775808L) {
            return "-9223372036854775808".getBytes(UTF_8);
        } else {
            int size = value < 0L ? stringSize(-value) + 1 : stringSize(value);
            byte[] arr = new byte[size];
            getChars(value, size, arr);
            return  arr;
        }
    }
    static int stringSize(long value) {
        long var2 = 10L;

        for(int var4 = 1; var4 < 19; ++var4) {
            if (value < var2) {
                return var4;
            }

            var2 = 10L * var2;
        }

        return 19;
    }
    static void getChars(long value, int var2, byte[] var3) {
        int index = var2;
        byte var8 = 0;
        if (value < 0L) {
            var8 = 45;
            value = -value;
        }

        int digitOnesIndex;
        while(value > 2147483647L) {
            long var4 = value / 100L;
            digitOnesIndex = (int)(value - ((var4 << 6) + (var4 << 5) + (var4 << 2)));
            value = var4;
            --index;
            var3[index] = DigitOnes[digitOnesIndex];
            --index;
            var3[index] = DigitTens[digitOnesIndex];
        }

        int var9;
        int var10;
        for(var10 = (int)value; var10 >= 65536; var3[index] = DigitTens[digitOnesIndex]) {
            var9 = var10 / 100;
            digitOnesIndex = var10 - ((var9 << 6) + (var9 << 5) + (var9 << 2));
            var10 = var9;
            --index;
            var3[index] = DigitOnes[digitOnesIndex];
            --index;
        }

        do {
            var9 = var10 * '쳍' >>> 19;
            digitOnesIndex = var10 - ((var9 << 3) + (var9 << 1));
            --index;
            var3[index] = digits[digitOnesIndex];
            var10 = var9;
        } while(var9 != 0);

        if (var8 != 0) {
            --index;
            var3[index] = var8;
        }

    }
}
