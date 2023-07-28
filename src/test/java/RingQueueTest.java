import com.wiqer.redis.aof.RingBlockingQueue;
import com.wiqer.redis.memory.SimpleRingQueue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RingQueueTest {
    @Test
    public  void RingBlockingQueueTestMain()  {
        SimpleRingQueue<String> que=  new SimpleRingQueue<String>(4000,10000000);
        Long cur=System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            que.add("ss"+i);
        }
        System.out.println("SimpleRingQueue,耗时"+(System.currentTimeMillis()-cur)+"ms");
        LinkedList<String> que2=  new LinkedList<String>();
        cur=System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            que2.add("ss"+i);
        }
        System.out.println("LinkedList,耗时"+(System.currentTimeMillis()-cur)+"ms");

    }
    @Test
    public  void RingBlockingQueueTest2()  {
        SimpleRingQueue<String> que=  new SimpleRingQueue<String>(4000,100000);
        Long cur=System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            que.add("ss"+i);
        }
        String s="";
        while ((s=que.poll() )!= null) {
            System.out.println(s);
        }
    }

    /**
     * 内存
     */
    @Test
    public  void RingBlockingQueueTest()  {
        Long cur=System.currentTimeMillis();
        for (int l = 0; l < 100; l++) {
            SimpleRingQueue<String> que=  new SimpleRingQueue<String>(400,100000);
            for (int p = 0; p < 100; p++){
                for (int i = 0; i < 10000; i++) {
                    que.add("ss"+i);
                }
                for (int i = 0; i < 10000; i++) {
                    que.poll();
                }
            }

        }

        System.out.println("SimpleRingQueue,耗时"+(System.currentTimeMillis()-cur)+"ms");

        cur=System.currentTimeMillis();
        for (int l = 0; l < 100; l++) {
            LinkedList<String> que2=  new LinkedList<>();
            for (int p = 0; p < 100; p++){
                for (int i = 0; i < 100000; i++) {
                    que2.add("ss"+i);
                }
                for (int i = 0; i < 100000; i++) {
                    que2.poll();
                }
            }

        }

        System.out.println("LinkedList,耗时"+(System.currentTimeMillis()-cur)+"ms");

        cur=System.currentTimeMillis();
        for (int l = 0; l < 100; l++) {
            ArrayList<String> que3=  new ArrayList<>(100000);
            for (int p = 0; p < 100; p++){
                for (int i = 0; i < 100000; i++) {
                    que3.add("ss"+i);
                }


                for(int var1 = 0; var1 < que3.size(); ++var1) {
                    que3.get(var1);
                }
                que3.clear();
            }

        }

        System.out.println("ArrayList,耗时"+(System.currentTimeMillis()-cur)+"ms");
    }
}
