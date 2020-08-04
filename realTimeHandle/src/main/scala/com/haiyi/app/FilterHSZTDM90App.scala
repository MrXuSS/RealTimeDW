package com.haiyi.app

import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import com.haiyi.util.MyKafkaSink
import org.apache.flink.api.common.functions.{AggregateFunction, RichMapFunction}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import org.apache.flink.streaming.api.scala._

/**
 * @Author:XuChengLei
 * @Date:2020-07-08
 * 比如筛选核算状态代码是90的数据，
 */
object FilterHSZTDM90App {
  def main(args: Array[String]): Unit = {

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "node1:9092")
    properties.setProperty("group.id", "consumer-group12311")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "earliest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    // 1.获取kafka中的json字符串
    val stream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("topic_canal_mysql", new SimpleStringSchema(), properties))
    // 2.刷选出HSZTDM 是 90的数据
    val filter90Stream: DataStream[String] = stream.filter(elem => {
      val jSONObject: JSONObject = JSON.parseObject(elem)
      if(!jSONObject.containsKey("eventType")){
        false
      }else if(jSONObject.get("eventType").toString != "INSERT"){
        false
      }else if(jSONObject.get("HSZTDM") != null && jSONObject.get("HSZTDM") == "90" ){
        true
      }else{
        false
      }
    })

    val resultStream: DataStream[Int] = filter90Stream.keyBy(JSON.parseObject(_).get("HSZTDM").toString).map(new MySumFunction)
    resultStream.print()

//    //3.将数据发送到Kafka中
//    val topic = "topic_HSZTDM_90"
//    filter90Stream.addSink(new MyKafkaSink(topic))
//
//    filter90Stream.print()


    env.execute("FilterHSZTM90App")

  }

  class MySumFunction extends RichMapFunction[String,Int]{
    private var lastTempState:ValueState[Int] = _

    override def open(parameters: Configuration): Unit = {
      val lastTempStateDescriptor = new ValueStateDescriptor[Int]("lastTemp", classOf[Int])
      lastTempState = getRuntimeContext.getState[Int](lastTempStateDescriptor)
    }
    
    override def map(in: String): Int = {
      var lastValue: Int = lastTempState.value()
      lastValue = lastValue + 1
      this.lastTempState.update(lastValue)
      lastValue
    }
  }


//  class MyAggregate extends AggregateFunction[String,Int,Int]{
//    override def createAccumulator(): Int = 0
//
//    override def add(in: String, acc: Int): Int = {
//      acc + 1
//    }
//
//    override def getResult(acc: Int): Int = {
//      acc
//    }
//
//    override def merge(acc: Int, acc1: Int): Int = {
//      acc + acc1
//    }
//  }


}
