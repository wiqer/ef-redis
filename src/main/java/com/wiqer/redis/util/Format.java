package com.wiqer.redis.util;

public class Format {
    static final  char[] EFNETBytes =new char[]{'0','1','2','3','4','5','6','7','8','9',
            'a','b','c','d','e','f','g','h','i','j','k','l',
            'm','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V',
            'W','X','Y','Z','$','~'};
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

    public  long parseLong(byte[] content, int radix)
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
}
