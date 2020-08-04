package com.haiyi.app

import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable

/**
 * @Author:XuChengLei
 * @Date:2020-07-22
 *
 */
object ThreadTestApp {

  var queueArray:Array[mutable.Queue[String]] = _
  var queueNum:Int = 3
  var threadNum:Int = 3


  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "node1:9092")
    properties.setProperty("group.id", "consumer-group12311")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "earliest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    createQueueArray(queueNum)
    startThread(threadNum,queueArray)

    env.addSource(new FlinkKafkaConsumer011[String]("threadTest",new SimpleStringSchema(),properties))
      .map{
        json =>{
          val jSONObject: JSONObject = JSON.parseObject(json)
          val queueNum: Int = jSONObject.getString("GZDBH").hashCode % threadNum
          queueArray(queueNum).enqueue(json)
        }
      }


    env.execute("ThreadTestApp")
  }

  def createQueueArray(num:Int): Unit ={
    queueArray = new Array[mutable.Queue[String]](num)
    for (i <- 0 to queueArray.size-1){
      queueArray(i) = new mutable.Queue[String]()
    }
  }

  def startThread(num:Int,array:Array[mutable.Queue[String]]): Unit ={
    val numThread: Int = num
    for (i <- 0 to numThread-1){
      val thread = new MyThread(array(i))
      thread.setName("thread"+"--"+i)
      thread.start()
    }
  }

  class MyThread(queue: mutable.Queue[String]) extends Thread{
    override def run(): Unit = {
      while(true){
        if (!queue.isEmpty) {
          val str: String = queue.dequeue()
          if(str != null){
            println(this.getName + "---" + str)
          }
        }else {
          println(this.getName+"暂时没有数据,休息2s")
          Thread.sleep(2000)
        }
      }
    }
  }
}

