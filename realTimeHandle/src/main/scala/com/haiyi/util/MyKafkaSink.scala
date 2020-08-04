package com.haiyi.util

import java.io.InputStream
import java.util.Properties

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/**
 * @Author:XuChengLei
 * @Date:2020-07-08
 *
 */
class MyKafkaSink(topic:String) extends RichSinkFunction[String]{

  var producer: KafkaProducer[String, String] = null

  override def open(parameters: Configuration): Unit ={
    val properties = new Properties()
    // 配置资源配置
    properties.put("bootstrap.servers","node1:9092");
    properties.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
    properties.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");

    producer = new KafkaProducer[String,String](properties)
  }

  override def close(): Unit = {
    producer.close()
  }

  override def invoke(json: String, context: SinkFunction.Context[_]): Unit = {
    producer.send(new ProducerRecord[String,String](topic,json))
  }
}
