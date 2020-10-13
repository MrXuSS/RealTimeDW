package com.haiyi.kftoes.app

import java.util
import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink
import org.apache.flink.util.Collector
import org.apache.http.HttpHost
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.delete.{DeleteRequest, DeleteResponse}
import org.elasticsearch.client.{RequestOptions, Requests, RestClient, RestHighLevelClient}

/**
 * @author Mr.Xu
 * @create 2020-10-12 14:20
 *  mysql - canal - kafka - flink - es
 */
object KfToEsApp {
  def main(args: Array[String]): Unit = {

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "192.168.2.201:9092")
    properties.setProperty("group.id", "consumer-group798415313")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "earliest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    val inputStream = env.addSource(new FlinkKafkaConsumer011[String]("testEs", new SimpleStringSchema(), properties))
    val result = inputStream.process(new MyProcessFunction)

    val httpHosts = new util.ArrayList[HttpHost]()
    httpHosts.add(new HttpHost("192.168.2.201", 9200, "http"))

    val esSinkBulider = new ElasticsearchSink.Builder[String](
      httpHosts,
      new ElasticsearchSinkFunction[String] {
        override def process(line: String,
                             runtimeContext: RuntimeContext,
                             requestIndexer: RequestIndexer): Unit = {
          val json = new util.HashMap[String, Object]()
          val jSONObject = JSON.parseObject(line)
          val iter = jSONObject.keySet().iterator()
          while (iter.hasNext){
            val key = iter.next()
            val value = jSONObject.get(key)
            json.put(key, value)
          }
          val indexRequest = Requests.indexRequest()
            .index("my-index")
            .id(JSON.parseObject(line).get("GZDBH").toString)
            .source(json)

          requestIndexer.add(indexRequest)
        }
      }
    )

    esSinkBulider.setBulkFlushMaxActions(1)
    result.print()
    result.addSink(esSinkBulider.build())

    env.execute("KfToEsApp")
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
