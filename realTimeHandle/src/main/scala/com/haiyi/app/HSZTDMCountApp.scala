package com.haiyi.app

import java.util
import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import com.haiyi.entity.Hs_jldxx
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011

import scala.collection.mutable

/**
 * @Author:XuChengLei
 * @Date:2020-07-09
 * 实时的统计核算状态代码数量，统计各个核算状态的累计数
 */

object HSZTDMCountApp {
  def main(args: Array[String]): Unit = {

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "node1:9092")
    properties.setProperty("group.id", "consumer-group53")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "earliest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)
    env.enableCheckpointing(5000)

    // 1.获取kafka中的json字符串
    val stream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("topic_canal_mysql", new SimpleStringSchema(), properties))

    val resultMap = new util.HashMap[String, Int]()

    // 2.清洗数据：因为之前往kafka中写数据测试时，有脏数据的写入；保留可以转换为Hs_jldxx的数据
    val filterStream: DataStream[String] = stream.filter {
      jsonStr =>
        val jSONObject: JSONObject = JSON.parseObject(jsonStr)
        val keys: util.Set[String] = jSONObject.keySet()
        if (keys.size() < 135 || jSONObject.getString("HSZTDM").isEmpty) {
          false
        }else{
          true
        }
    }

    // 3.实时的统计核算状态代码数量，统计各个核算状态的累计数
    val resultJson: DataStream[String] = filterStream.map {
      jsonStr => {
        val jSONObject: JSONObject = JSON.parseObject(jsonStr)
        var key = jSONObject.getString("HSZTDM")
        if (resultMap.containsKey(key)) {
          var count = resultMap.get(key)
          resultMap.put(key, count + 1)
        } else {
          resultMap.put(key, 1)
        }
        val resultJsonObject = new JSONObject()
        resultJsonObject.put("result", resultMap)
        resultJsonObject.toString
      }
    }

    resultJson.print()

    env.execute("HSZTDMCountApp")
  }
}
