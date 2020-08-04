package com.haiyi.app

import java.util
import java.util.Properties

import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}

import scala.collection.mutable

/**
 * @Author:XuChengLei
 * @Date:2020-07-23
 *
 */
object QueueThreadTest {

  val queue1 = new mutable.Queue[String]()
  val queue2 = new mutable.Queue[String]()

  def main(args: Array[String]): Unit = {

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "node1:9092")
    properties.setProperty("group.id", "consumer-group122")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "earliest")

    val kafkaConsumer = new KafkaConsumer[String, String](properties)
    kafkaConsumer.subscribe(util.Arrays.asList("threadTest"))


    val myThread1 = new MyThread(queue1)
    myThread1.setName("MyThread1")
    myThread1.start()
    val myThread2 = new MyThread(queue2)
    myThread2.setName("MyThread2")
    myThread2.start()

    while(true){
      val consumerRecords: ConsumerRecords[String, String] = kafkaConsumer.poll(1000)
      val iter: util.Iterator[ConsumerRecord[String, String]] = consumerRecords.iterator()
      while (iter.hasNext){
        val next: ConsumerRecord[String, String] = iter.next()
        if (next.key().hashCode % 2 == 0) {
          queue1.enqueue(next.value())
        }else{
          queue2.enqueue(next.value())
        }
      }
    }
  }

  class MyThread(queue:mutable.Queue[String]) extends Thread{

    override def run(): Unit = {
      while (true){
        if (!queue.isEmpty) {
          val str: String = queue.dequeue()
          println(Thread.currentThread().getName + "---" + str)
        }else{
          println(Thread.currentThread().getName+"暂时没有数据，休息2s")
          Thread.sleep(2000)
        }
      }
    }
  }
}
