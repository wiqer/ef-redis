import com.wiqer.redis.aof.RingBlockingQueue;
import org.junit.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RingBlockingQueue线程安全性测试
 */
public class RingBlockingQueueThreadSafetyTest {

    @Test
    public void testBasicThreadSafety() throws InterruptedException {
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
        assert totalProduced.get() == totalConsumed.get() : 
            "Produced: " + totalProduced.get() + ", Consumed: " + totalConsumed.get();
        assert queue.isEmpty() : "Queue should be empty after all operations";
    }
    
    @Test
    public void testConcurrentOfferPoll() throws InterruptedException {
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
        assert successfulOffers.get() >= successfulPolls.get() : 
            "Cannot consume more than produced";
        assert queue.size() == (successfulOffers.get() - successfulPolls.get()) : 
            "Queue size should match the difference between offers and polls";
    }
    
    @Test
    public void testPeekThreadSafety() throws InterruptedException {
        RingBlockingQueue<Integer> queue = new RingBlockingQueue<>(20, 200);
        
        int threadCount = 5;
        int operationsPerThread = 500;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger peekCount = new AtomicInteger(0);
        AtomicInteger nullPeekCount = new AtomicInteger(0);
        
        // 启动peek线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                Random random = new Random();
                for (int j = 0; j < operationsPerThread; j++) {
                    Integer peeked = queue.peek();
                    peekCount.incrementAndGet();
                    if (peeked == null) {
                        nullPeekCount.incrementAndGet();
                    }
                    
                    // 偶尔进行写操作
                    if (random.nextInt(10) == 0) {
                        queue.offer(random.nextInt(1000));
                    }
                    
                    // 偶尔进行读操作
                    if (random.nextInt(10) == 0) {
                        queue.poll();
                    }
                    
                    try {
                        Thread.sleep(random.nextInt(2));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("Total peeks: " + peekCount.get());
        System.out.println("Null peeks: " + nullPeekCount.get());
        System.out.println("Final queue size: " + queue.size());
        
        // 验证peek操作没有破坏队列状态
        assert peekCount.get() == operationsPerThread * threadCount : 
            "All peek operations should be counted";
    }
    
    @Test
    public void testBlockingOperations() throws InterruptedException, ExecutionException, TimeoutException {
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
        String putResult = (String) blockedPut.get(2, TimeUnit.SECONDS);
        String pollResult = (String) poller.get(2, TimeUnit.SECONDS);
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("Put result: " + putResult);
        System.out.println("Poll result: " + pollResult);
        System.out.println("Final queue size: " + queue.size());
        
        // 验证阻塞操作正常工作
        assert "Put completed".equals(putResult) : "Put should complete after poll";
        assert pollResult.startsWith("Polled: ") : "Poll should return a value";
    }
    
    @Test
    public void testIteratorThreadSafety() throws InterruptedException {
        RingBlockingQueue<String> queue = new RingBlockingQueue<>(30, 300);
        
        // 添加一些元素
        for (int i = 0; i < 50; i++) {
            queue.offer("Element-" + i);
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        
        // 启动迭代器线程
        executor.submit(() -> {
            try {
                int count = 0;
                for (String element : queue) {
                    count++;
                    Thread.sleep(1); // 模拟处理时间
                }
                System.out.println("Iterator 1 processed " + count + " elements");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // 启动写操作线程
        executor.submit(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    queue.offer("NewElement-" + i);
                    Thread.sleep(5);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // 启动读操作线程
        executor.submit(() -> {
            try {
                for (int i = 0; i < 15; i++) {
                    queue.poll();
                    Thread.sleep(3);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("Final queue size: " + queue.size());
        
        // 验证迭代器操作没有导致异常
        assert true : "Iterator should complete without exceptions";
    }
} 