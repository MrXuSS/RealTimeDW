package com.haiyi.kfk.app

import java.util
import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import com.haiyi.kfk.util.KuDuSink
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011

import scala.collection.mutable


/**
 * @Author:XuChengLei
 * @Date:2020-07-14
 *
 */
object KFKuduApp {
  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "node1:9092")
    properties.setProperty("group.id", "consumer-group12311")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "latest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)
    env.enableCheckpointing(5000)
    //设置模式为：exactly_one，仅一次语义
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(1000)
    //检查点必须在10s之内完成，或者被丢弃【checkpoint超时时间】
    env.getCheckpointConfig.setCheckpointTimeout(10000)
    //同一时间只允许进行一次检查点
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)

    // 1.获取kafka中的json字符串
    val stream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("threadTest", new SimpleStringSchema(), properties))

    // 2.将数据转换为Map
    val streamMap: DataStream[util.HashMap[String, String]] = stream.map {
      jsonStr => {
        val tmpMap = new util.HashMap[String, String]()
        val jSONObject: JSONObject = JSON.parseObject(jsonStr)
        val keys: util.Set[String] = jSONObject.keySet()
        val iter: util.Iterator[String] = keys.iterator()
        while (iter.hasNext) {
          val key: String = iter.next()
          tmpMap.put(key, jSONObject.get(key).toString)
        }
        println(tmpMap.toString)
        tmpMap
      }
    }

    // 3 .将数据写入到KUDU
    var kuduMaster ="192.168.2.201"
    var tableName = "hs_jldxx"

    streamMap.addSink(new KuDuSink(kuduMaster,tableName))

    env.execute("K-F-KuduApp")

  }
}
