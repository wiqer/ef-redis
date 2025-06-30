import com.wiqer.redis.aof.RingBlockingQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RingBlockingQueue简单线程安全性测试
 */
public class SimpleRingBlockingQueueTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("开始测试RingBlockingQueue线程安全性...");
        
        // 测试基本线程安全
        testBasicThreadSafety();
        
        // 测试并发读写
        testConcurrentOfferPoll();
        
        // 测试阻塞操作
        testBlockingOperations();
        
        System.out.println("所有测试完成！");
    }

    public static void testBasicThreadSafety() throws InterruptedException {
        System.out.println("\n=== 测试基本线程安全性 ===");
        RingBlockingQueue<Integer> queue = new RingBlockingQueue<>(100, 1000);
        
        // 创建多个生产者和消费者
        int producerCount = 4;
        int consumerCount = 4;
        int elementsPerProducer = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(producerCount + consumerCount);
        CountDownLatch producerLatch = new CountDownLatch(producerCount);
        CountDownLatch consumerLatch = new CountDownLatch(consumerCount);
        
        AtomicInteger totalProduced = new AtomicInteger(0);
        AtomicInteger totalConsumed = new AtomicInteger(0);
        List<Integer> consumedElements = new ArrayList<>();
        
        // 启动生产者
        for (int i = 0; i < producerCount; i++) {
            final int producerId = i;
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int j = 0; j < elementsPerProducer; j++) {
                        int element = producerId * elementsPerProducer + j;
                        queue.put(element);
                        totalProduced.incrementAndGet();
                        Thread.sleep(random.nextInt(10)); // 模拟工作负载
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producerLatch.countDown();
                }
            });
        }
        
        // 启动消费者
        for (int i = 0; i < consumerCount; i++) {
            executor.submit(() -> {
                try {
                    while (producerLatch.getCount() > 0 || !queue.isEmpty()) {
                        Integer element = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (element != null) {
                            synchronized (consumedElements) {
                                consumedElements.add(element);
                            }
                            totalConsumed.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumerLatch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        producerLatch.await();
        consumerLatch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        // 验证结果
        System.out.println("Total produced: " + totalProduced.get());
        System.out.println("Total consumed: " + totalConsumed.get());
        System.out.println("Queue size: " + queue.size());
        System.out.println("Consumed elements count: " + consumedElements.size());
        
        // 验证所有元素都被正确处理
        if (totalProduced.get() == totalConsumed.get()) {
            System.out.println("✓ 生产消费数量匹配");
        } else {
            System.out.println("✗ 生产消费数量不匹配");
        }
        
        if (queue.isEmpty()) {
            System.out.println("✓ 队列为空");
        } else {
            System.out.println("✗ 队列不为空");
        }
    }
    
    public static void testConcurrentOfferPoll() throws InterruptedException {
        System.out.println("\n=== 测试并发读写 ===");
        RingBlockingQueue<String> queue = new RingBlockingQueue<>(50, 500);
        
        int threadCount = 10;
        int operationsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicLong totalOffers = new AtomicLong(0);
        AtomicLong totalPolls = new AtomicLong(0);
        AtomicLong successfulOffers = new AtomicLong(0);
        AtomicLong successfulPolls = new AtomicLong(0);
        
        // 启动混合读写线程
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                Random random = new Random();
                for (int j = 0; j < operationsPerThread; j++) {
                    if (random.nextBoolean()) {
                        // 写操作
                        totalOffers.incrementAndGet();
                        if (queue.offer("Thread-" + threadId + "-Element-" + j)) {
                            successfulOffers.incrementAndGet();
                        }
                    } else {
                        // 读操作
                        totalPolls.incrementAndGet();
                        if (queue.poll() != null) {
                            successfulPolls.incrementAndGet();
                        }
                    }
                    
                    // 随机休眠
                    if (random.nextInt(100) < 10) {
                        try {
                            Thread.sleep(random.nextInt(5));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("Total offers: " + totalOffers.get());
        System.out.println("Successful offers: " + successfulOffers.get());
        System.out.println("Total polls: " + totalPolls.get());
        System.out.println("Successful polls: " + successfulPolls.get());
        System.out.println("Final queue size: " + queue.size());
        
        // 验证队列状态的一致性
        if (successfulOffers.get() >= successfulPolls.get()) {
            System.out.println("✓ 消费数量不超过生产数量");
        } else {
            System.out.println("✗ 消费数量超过生产数量");
        }
        
        if (queue.size() == (successfulOffers.get() - successfulPolls.get())) {
            System.out.println("✓ 队列大小计算正确");
        } else {
            System.out.println("✗ 队列大小计算错误");
        }
    }
    
    public static void testBlockingOperations() throws InterruptedException {
        System.out.println("\n=== 测试阻塞操作 ===");
        RingBlockingQueue<Integer> queue = new RingBlockingQueue<>(10, 100);
        
        // 测试阻塞put操作
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // 填充队列
        for (int i = 0; i < 100; i++) {
            queue.put(i);
        }
        
        // 启动一个线程尝试put（应该阻塞）
        Future<?> blockedPut = executor.submit(() -> {
            try {
                queue.put(999);
                return "Put completed";
            } catch (InterruptedException e) {
                return "Put interrupted";
            }
        });
        
        // 等待一小段时间，确保put被阻塞
        Thread.sleep(100);
        
        // 启动一个线程进行poll操作，应该唤醒被阻塞的put
        Future<?> poller = executor.submit(() -> {
            try {
                Thread.sleep(50);
                Integer result = queue.poll();
                return "Polled: " + result;
            } catch (InterruptedException e) {
                return "Poll interrupted";
            }
        });
        
        // 等待操作完成
        try {
            String putResult = (String) blockedPut.get(2, TimeUnit.SECONDS);
            String pollResult = (String) poller.get(2, TimeUnit.SECONDS);
            
            System.out.println("Put result: " + putResult);
            System.out.println("Poll result: " + pollResult);
            System.out.println("Final queue size: " + queue.size());
            
            // 验证阻塞操作正常工作
            if ("Put completed".equals(putResult)) {
                System.out.println("✓ Put操作正常完成");
            } else {
                System.out.println("✗ Put操作异常");
            }
            
            if (pollResult.startsWith("Polled: ")) {
                System.out.println("✓ Poll操作正常完成");
            } else {
                System.out.println("✗ Poll操作异常");
            }
        } catch (Exception e) {
            System.out.println("✗ 阻塞操作测试失败: " + e.getMessage());
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
} 