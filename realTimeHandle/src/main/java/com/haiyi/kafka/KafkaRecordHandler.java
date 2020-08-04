package com.haiyi.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * @Author:XuChengLei
 * @Date:2020-07-23
 */
public class KafkaRecordHandler implements Runnable{

    private ConsumerRecord record;

    public KafkaRecordHandler(ConsumerRecord record){
        this.record = record;
    }

    @Override
    public void run() {
        System.out.printf(record.timestamp()+" Received Message topic =%s, partition =%s, offset = %d, key = %s, value = %s\n", record.topic(), record.partition(), record.offset(), record.key(), record.value());
        System.out.println("Thread id = "+ Thread.currentThread().getId());
    }
}
