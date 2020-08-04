package com.haiyi.kafka;

/**
 * @Author:XuChengLei
 * @Date:2020-07-23
 */
public class ConsumerMain {
    public static void main(String[] args) {
        KafkaProcessor processor = new KafkaProcessor();
        try {
            processor.init(5);
        }catch (Exception e){
            processor.shutdown();
        }

    }
}
