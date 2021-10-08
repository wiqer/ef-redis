import com.wiqer.redis.util.StringUtil;
import org.junit.Test;

import java.util.regex.Pattern;

public class StringTest {
    @Test
    public static void matched(String[] args) {

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
}
