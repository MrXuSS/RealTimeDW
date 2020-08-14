package com.haiyi.kfh.app

import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import com.haiyi.kfh.entity.Hs_jldxx
import com.haiyi.kfh.util.{HBaseSink, HBaseUtil}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import org.apache.flink.util.Collector


/**
 * @Author:XuChengLei
 * @Date:2020-07-03
 *
 */
object KFHApp {
  def main(args: Array[String]): Unit = {

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "node1:9092")
    properties.setProperty("group.id", "consumer-group")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "latest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)
    env.enableCheckpointing(5000)
    //设置模式为：exactly_one，仅一次语义
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(1000)
    //检查点必须在10s之内完成，或者被丢弃【checkpoint超时时间】
    env.getCheckpointConfig.setCheckpointTimeout(10000)
    //同一时间只允许进行一次检查点
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)

    // 1.获取kafka中的json字符串
    val stream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("topic_canal_mysql", new SimpleStringSchema(), properties))
     // 2.对数据进行分流，json串中存在eventType的数据即为要删除的数据
    val result: DataStream[String] = stream.process(new MyProcessFunction)
    // 3.对主流数据进行转换
    var mm = 0
    val hs_jldxx_stream: DataStream[Hs_jldxx] = result.map {
      kafkaString => {
        mm = mm + 1
        println(mm)
        val hs_jldxx: Hs_jldxx = JSON.parseObject(kafkaString, classOf[Hs_jldxx])
        hs_jldxx
      }
    }
    // 4.将数据存入或更新到HBase中
    hs_jldxx_stream.addSink(new HBaseSink("hs_jldxx","info"))
    //hs_jldxx_stream.print()

    // 5.获取侧输出流
    val deleteStream: DataStream[String] = result.getSideOutput(new OutputTag[String]("deleteStream"))

    // 6.获取要删除数据的rowkey
    val deleteRowKeyStream: DataStream[String] = deleteStream.map {
      jsonStr =>

        val jSONObject: JSONObject = JSON.parseObject(jsonStr)
        val rowKey: AnyRef = jSONObject.get("GZDBH")
        rowKey.toString
    }
    // 7.根据rowkey进行删除
    deleteRowKeyStream.map(HBaseUtil.deleteByRowKey("hs_jldxx",_))

    // 8.执行代码
    env.execute("kafka-hbase-app")
  }

  // 分流
  class MyProcessFunction extends  ProcessFunction[String,String]{
    lazy val deleteOutput = new OutputTag[String]("deleteStream")

    override def processElement(json: String,
                                context: ProcessFunction[String, String]#Context,
                                collector: Collector[String]): Unit = {
      val jSONObject: JSONObject = JSON.parseObject(json)
      if(jSONObject.get("eventType") == "DELETE"){
        context.output(deleteOutput,json)
      }else{
        collector.collect(json)
      }
    }
  }

}
