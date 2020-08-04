package com.haiyi.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @Author:XuChengLei
 * @Date:2020-07-23
 */
public class KafkaProcessor {
    private final KafkaConsumer threadConsumer;
    private ExecutorService executor;
    private static final Properties CONSUMER_PROPERTIES =new Properties();
    static {
        CONSUMER_PROPERTIES.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "node1:9092");
        // specify the protocol for SSL Encryption
        CONSUMER_PROPERTIES.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
        CONSUMER_PROPERTIES.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        CONSUMER_PROPERTIES.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,true);
        CONSUMER_PROPERTIES.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        CONSUMER_PROPERTIES.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    }
    public KafkaProcessor() {
        this.threadConsumer =new KafkaConsumer<>(CONSUMER_PROPERTIES);//初始化配置Consumer对象
        this.threadConsumer.subscribe(Arrays.asList("topic_canal_mysql")); //订阅topic
    }
    public void init(int numberOfThreads) {
//创建一个线程池
        /**
         * public ThreadPoolExecutor(int corePoolSize,
         int maximumPoolSize,
         long keepAliveTime,
         TimeUnit unit,
         BlockingQueue<Runnable> workQueue,
         ThreadFactory threadFactory,
         RejectedExecutionHandler handler)
         *corePoolSize : 核心线程数，一旦创建将不会再释放。如果创建的线程数还没有达到指定的核心线    程数量，将会继续创建新的核心线程，直到达到最大核心线程数后，核心线程数将不在增加；如果没有空闲的核心线程，同时又未达到最大线程数，则将继续创建非核心线程；如果核心线程数等于最大线程数，则当核心线程都处于激活状态时，任务将被挂起，等待空闲线程来执行。
         *maximumPoolSize : 最大线程数，允许创建的最大线程数量。如果最大线程数等于核心线程数，则无法创建非核心线程；如果非核心线程处于空闲时，超过设置的空闲时间，则将被回收，释放占用的资源。
         *keepAliveTime : 也就是当线程空闲时，所允许保存的最大时间，超过这个时间，线程将被释放销毁，但只针对于非核心线程。
         *unit : 时间单位，TimeUnit.SECONDS等。
         *workQueue : 任务队列，存储暂时无法执行的任务，等待空闲线程来执行任务。
         *threadFactory :  线程工程，用于创建线程。
         *handler : 当线程边界和队列容量已经达到最大时，用于处理阻塞时的程序
         */
        executor =new ThreadPoolExecutor(numberOfThreads, numberOfThreads,0L, TimeUnit.MILLISECONDS,new ArrayBlockingQueue(1000), new ThreadPoolExecutor.CallerRunsPolicy());
        while (true) {
            ConsumerRecords records = threadConsumer.poll(100);
            for (final Object record : records) {
                executor.submit(new KafkaRecordHandler((ConsumerRecord) record));
            }
        }
    }
    //别忘记线程池的关闭！
    public void shutdown() {
        if (threadConsumer !=null) {
            threadConsumer.close();
        }
        if (executor !=null) {
            executor.shutdown();
        }
        try {
            if (executor !=null && !executor.awaitTermination(60, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        }catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }


}
