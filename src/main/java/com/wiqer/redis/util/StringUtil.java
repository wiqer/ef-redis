package com.wiqer.redis.util;

/**
 * @author lilan
 */
public class StringUtil {
    static final char[] EscapeCharacter = "([{/^-$¦}])?*+.".toCharArray();

    public static String EscapeString(String content) {
        if (content == null) {
            return content;
        } else if (content.length() == 0) {
            return content;
        } else {
            content = content.trim();
            StringBuilder sb = new StringBuilder();
            char[] temp = content.toCharArray();
            for (char c :
                    temp) {
                for (char e : EscapeCharacter) {
                    if (e == c) {
                        sb.append('\\');
                    }
                }
                sb.append(c);
            }
            return sb.toString();
        }

    }

}
