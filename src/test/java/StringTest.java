import com.wiqer.redis.util.Format;
import com.wiqer.redis.util.StringUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringTest {
    @Test
    public  void matched() {

        System.out.println( StringUtil.EscapeString("*"));
        Boolean matched= Pattern.matches( StringUtil.EscapeString("*"), "content");
        Boolean matched2= Pattern.matches( ".*o*", "'content'");
        System.out.println(matched);
        System.out.println(matched2);
        System.out.println(" \".*o*\":" + Pattern.matches( ".*o*", "'content'"));
        System.out.println(" \".*\":" + Pattern.matches( ".*", "'content'"));

        String content = "I am noob " +
                "from runoob.com.";

        String pattern2 = ".*runoob.*";

        boolean isMatch = Pattern.matches(pattern2, content);
        System.out.println("字符串中是否包含了 'runoob' 子字符串? " + isMatch);
    }
    @Test
    public  void toInternal() {

        String i="666";
        Integer.valueOf(i);
    }
    @Test
    public  void toInternal2() {

        String i="666";
        System.out.println(this.parseInt(i.getBytes(UTF_8),10));
    }
    @Test
    public  void toInternal3() {

        String i="666";

        long value=Format.parseLong(i.getBytes(UTF_8),10);
        System.out.println(
                value
        );
        System.out.println(
               new String(Format.toByteArray( value),UTF_8)
        );
    }
    public  long parseInt(byte[] content, int radix)
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
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+')
                    throw new NumberFormatException();

                if (len == 1) // Cannot have lone "+" or "-"
                    throw new NumberFormatException();
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
