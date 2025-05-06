import com.wiqer.redis.aof.RingBlockingQueue;


import io.netty.util.internal.shaded.org.jctools.queues.SpscLinkedQueue;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RingBlockingQueueTest {
    @Test
    public  void RingBlockingQueueTestMain()  {
        BlockingQueue<String> que=  new RingBlockingQueue<String>(4000,10000000);
        Long cur=System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            que.add("ss"+i);
        }
        System.out.println("RingBlockingQueue,耗时"+(System.currentTimeMillis()-cur)+"ms");
        BlockingQueue<String> que2=  new LinkedBlockingQueue<>(10000000);
        cur=System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            que2.add("ss"+i);
        }
        System.out.println("LinkedBlockingQueue,耗时"+(System.currentTimeMillis()-cur)+"ms");

    }
    @Test
    public  void RingBlockingQueueTest2()  {
        BlockingQueue<String> que=  new RingBlockingQueue<String>(4000,100000);
        Long cur=System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            que.add("ss"+i);
        }
        String s="";
        while ((s=que.poll() )!= null) {
            System.out.println(s);
        }
    }
}
