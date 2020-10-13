package com.haiyi.kftoes.app

import java.util
import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink
import org.apache.http.HttpHost
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
    properties.setProperty("group.id", "consumer-groupA1")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "earliest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    val inputStream = env.addSource(new FlinkKafkaConsumer011[String]("testEs", new SimpleStringSchema(), properties))

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
    inputStream.print()
    inputStream.addSink(esSinkBulider.build())

    env.execute("KfToEsApp")
  }
}

/**
 * {
 * "_index" : "my-index",
 * "_type" : "_doc",
 * "_id" : "0",
 * "_version" : 9,
 * "_seq_no" : 44019,
 * "_primary_term" : 3,
 * "found" : true,
 * "_source" : {
 * "JLDYTDM" : "0",
 * "SJLX" : "0",
 * "YGFTDL" : "0",
 * "YGZDBH" : "0",
 * "tableName" : "hs_jldxx",
 * "YHWYJQSR" : "0",
 * "CZSJ" : "0",
 * "JFXL" : "0",
 * "WGBSDL" : "0",
 * "WGXSDL" : "0",
 * "XSFTXYZ" : "0",
 * "JTDLBL" : "0",
 * "DDDF" : "0",
 * "TCDM" : "0",
 * "YGXSDL" : "0",
 * "JSLXDM" : "0",
 * "JBFDJ" : "0",
 * "TZXS" : "0",
 * "ZBJLDBH" : "0",
 * "LTDF" : "0",
 * "YGFBKJDL" : "0",
 * "WGFBKJDL" : "0",
 * "JSHH" : "0",
 * "eventType" : "DELETE",
 * "BSJFBZ" : "0",
 * "SJLL" : "0",
 * "TBGZDBH" : "0",
 * "YSDF" : "0",
 * "GTSYDL" : "0",
 * "DJBBBH" : "0",
 * "CZCS" : "0",
 * "YGXSJSZ" : "0",
 * "GDLL" : "0",
 * "JLDBH" : "0",
 * "DWBM" : "0",
 * "YGBSDL" : "0",
 * "ZJRL" : "0",
 * "JSBZ" : "0",
 * "DLJSFSDM" : "0",
 * "TCLJDF" : "0",
 * "YWLBDM" : "0",
 * "JFRL" : "0",
 * "HSZTDM" : "0",
 * "TBDF" : "0",
 * "BZ" : "0",
 * "HCRQ" : "0",
 * "ZDF" : "0",
 * "JBDFFTZ" : "0",
 * "SCCBRQ" : "0",
 * "CBQDBH" : "0",
 * "TCJSSJ" : "0",
 * "CDZXLDF" : "0",
 * "JSRQ" : "0",
 * "YGTBDL" : "0",
 * "SCGZDBH" : "0",
 * "JTLX" : "0",
 * "WGZDL" : "0",
 * "FJFHJ" : "0",
 * "JFYXJ" : "0",
 * "GLYSBZDM" : "0",
 * "GDDWBM" : "0",
 * "WGCJDL" : "0",
 * "JLDXH" : "0",
 * "HYFLDM" : "0",
 * "TCKSSJ" : "0",
 * "JBDFJSFSDM" : "0",
 * "DJ" : "0",
 * "JLDCBSXH" : "0",
 * "LTDFJSFS" : "0",
 * "PTBBDM" : "0",
 * "XLHDZ" : "0",
 * "BSFTFSDM" : "0",
 * "YGZDL" : "0",
 * "CZRBS" : "0",
 * "WYJJSFSDM" : "0",
 * "WGBSXYZ" : "0",
 * "JBDF" : "0",
 * "FGZS" : "0",
 * "DLFTGS" : "0",
 * "CYTZDF" : "0",
 * "CZNY" : "0",
 * "YXBZ" : "0",
 * "XSJSFSDM" : "0",
 * "JTNF" : "0",
 * "JLDZTDM" : "0",
 * "CXDM" : "0",
 * "JBDFFTFS" : "0",
 * "DYZH" : "0",
 * "ZJSL" : "0",
 * "GZDBH" : "0",
 * "TQBS" : "0",
 * "CTBBDM" : "0",
 * "YDRL" : "0",
 * "WGHBDL" : "0",
 * "DJDM" : "0",
 * "FXRQ" : "0",
 * "XLXDBS" : "0",
 * "PJLXDM" : "0",
 * "DQBM" : "0",
 * "NLJDL" : "0",
 * "DYBH" : "0",
 * "DBKJBZ" : "0",
 * "YHBH" : "0",
 * "YGBSXYZ" : "0",
 * "CBRQ" : "0",
 * "GLYSKHFSDM" : "0",
 * "WGTBDL" : "0",
 * "MFDL" : "0",
 * "FSJFBZ" : "0",
 * "YDDZ" : "0",
 * "CBZQ" : "0",
 * "GLBH" : "0",
 * "YDLBDM" : "0",
 * "YHMC" : "0",
 * "JLFSDM" : "0",
 * "DJCLBH" : "0",
 * "XSJFBZ" : "0",
 * "DFNY" : "0",
 * "YHZTDM" : "0",
 * "WGFTDL" : "0",
 * "YGCJDL" : "0",
 * "CJSJ" : "0",
 * "WGXSJSZ" : "0",
 * "YDTS" : "0",
 * "JLDYDJDM" : "0",
 * "KJXBDL" : "0",
 * "JFDL" : "0",
 * "BQCBCS" : "0",
 * "YCBCS" : "0",
 * "DLDBZ" : "0",
 * "YHLBDM" : "0",
 * "CJSBZ" : "0",
 * "YGDDL" : "0",
 * "KFRQ" : "0",
 * "YGHBDL" : "0",
 * "CBJHBH" : "0",
 * "XSFTBZ" : "0",
 * "SCDL" : "0"
 * }
 * }
 */
