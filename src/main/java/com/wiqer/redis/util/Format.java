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
    //保留n位
    public static int uintNBit(long z,int n) {
        // if(z<0) z=-z;
        return (int)(z&(1<<n-1));
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
}
