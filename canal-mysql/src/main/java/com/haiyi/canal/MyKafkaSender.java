package com.haiyi.canal;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * @Author:XuChengLei
 * @Date:2020-07-03
 */
public class MyKafkaSender {
    public static KafkaProducer<String,String> kafkaProducer = null;

    public static KafkaProducer<String,String> createKafkaProducer(){
        // 配置资源配置
        Properties properties = new Properties();
        properties.put("bootstrap.servers","node1:9092");
        properties.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        properties.put("partitioner.class","com.haiyi.canal.MyPartitioner");

        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(properties);
        return producer;
    }

    public static void send(String topic,String key,String msg){
        if(kafkaProducer == null){
            kafkaProducer =createKafkaProducer();
        }
        kafkaProducer.send(new ProducerRecord<String, String>(topic,key,msg));
    }
}