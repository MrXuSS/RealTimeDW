package com.haiyi.app

import java.util

import com.alibaba.fastjson.{JSON, JSONObject}
import com.haiyi.entity.Table_HS_JLDXX
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.kudu.client.{KuduClient, KuduScanner, KuduTable, RowResult}

import scala.collection.Map

/**
 * @Author:XuChengLei
 * @Date:2020-07-30
 *
 */
object FlinkKuduCEP {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    val stream: DataStream[String] = env.addSource(new MyKuduSource)
    val keyedStream: DataStream[Table_HS_JLDXX] = stream
      .map(JSON.parseObject(_, classOf[Table_HS_JLDXX]))
      .assignAscendingTimestamps(_.dfny.toLong+System.currentTimeMillis())
      .keyBy("jldbh", "cjsj", "gzdbh", "ywlbdm")

    val cepPattern: Pattern[Table_HS_JLDXX, Table_HS_JLDXX] = Pattern.begin[Table_HS_JLDXX]("begin")
      .where {
        obj => {
          obj.dqbm == "030600" && obj.jfdl.toDouble > 1000 && obj.jfdl.toDouble > obj.jfrl.toDouble * 400
        }
      }

    val patternStream: PatternStream[Table_HS_JLDXX] = CEP.pattern(keyedStream, cepPattern)

    val resultStream = patternStream.select((map: Map[String, Iterable[Table_HS_JLDXX]]) => {
      val begin: Table_HS_JLDXX = map.getOrElse("begin", null).iterator.next()
      begin
    })

    resultStream.print()

    env.execute("FlinkKuduCEP")

  }

  class MyKuduSource extends RichParallelSourceFunction[String]{

    private val masterAddr:String = "172.19.138.6,172.19.138.7,172.19.138.8"
    private val tableName:String = "HS_JLDXX"
    private var client: KuduClient = _
    private var kuduTable:KuduTable = _
    private var kuduScanner:KuduScanner = _

    override def open(parameters: Configuration): Unit = {
      client = new KuduClient.KuduClientBuilder(masterAddr).build()
      kuduTable = client.openTable(tableName)
    }

    override def close(): Unit = {
      kuduScanner.close()
      client.close()
    }

    override def run(ctx: SourceFunction.SourceContext[String]): Unit = {
      kuduScanner = client.newScannerBuilder(kuduTable).build()
      while (kuduScanner.hasMoreRows){
        val rowResultIter: util.Iterator[RowResult] = kuduScanner.nextRows().iterator()
        while (rowResultIter.hasNext){
          val result: RowResult = rowResultIter.next()
          val resultStr: String = result.rowToString()
          //STRING jldbh=0622000035000631, UNIXTIME_MICROS cjsj=1970-01-19T08:45:40.014000Z, STRING gzdbh=3000000032130684, STRING ywlbdm=0000, DOUBLE jfxl=0.0, STRING dlftgs=, STRING jbdfftfs=, STRING gddwbm=03061217, INT64 czcs=0, DOUBLE xsftxyz=0.0, STRING tcdm=, DOUBLE wgzdl=0.0, DOUBLE wgcjdl=0.0, STRING sjlx=1, STRING dljsfsdm=1, STRING jldytdm=110, UNIXTIME_MICROS czsj=1970-01-19T10:22:14.029000Z, DOUBLE sjll=0.0, STRING ydlbdm=500, DOUBLE ysdf=0.0, STRING ltdfjsfs=2, STRING pjlxdm=1, DOUBLE dddf=323.53, DOUBLE gdll=0.0, STRING hyfldm=IY0100, DOUBLE cytzdf=0.0, STRING dwbm=0306, DOUBLE jbdfftz=0.0, STRING bsftfsdm=0, DOUBLE jbfdj=0.0, DOUBLE tzxs=0.0, STRING jshh=062200003500063, DOUBLE ltdf=0.0, UNIXTIME_MICROS tckssj=NULL, STRING ctbbdm=, DOUBLE wghbdl=0.0, DOUBLE wgxsjsz=0.0, DOUBLE wgfbkjdl=0.0, STRING xsjfbz=0, DOUBLE yghbdl=0.0, DOUBLE tbdf=0.0, DOUBLE xlhdz=0.0, DOUBLE jfrl=0.0, DOUBLE wgftdl=0.0, UNIXTIME_MICROS hcrq=NULL, STRING dbkjbz=0, DOUBLE ygxsjsz=0.0, STRING jsbz=1, DOUBLE zjrl=0.0, STRING bz=, STRING jlfsdm=3, STRING tbgzdbh=, STRING djclbh=, STRING wyjjsfsdm=2, INT64 jldcbsxh=1790, STRING ptbbdm=, STRING cbjhbh=1111111115100520, DOUBLE wgxsdl=0.0, STRING jtlx=Y, STRING yhlbdm=20, DOUBLE ygcjdl=487.0, DOUBLE ygbsxyz=0.0, INT64 jfyxj=0, STRING xsftbz=0, DOUBLE fjfhj=4.21, DOUBLE ygzdl=487.0, INT64 jtdlbl=1, STRING fsjfbz=0, DOUBLE kjxbdl=0.0, INT64 yhwyjqsr=26, UNIXTIME_MICROS jsrq=1970-01-19T10:22:14.029000Z, DOUBLE zdf=327.74, INT64 bqcbcs=1, DOUBLE gtsydl=0.0, DOUBLE wgbsdl=0.0, DOUBLE dj=0.59886875, DOUBLE dldbz=0.0, DOUBLE jbdf=0.0, DOUBLE ygxsdl=0.0, STRING yxbz=1, STRING cxdm=2, DOUBLE fgzs=0.0, STRING glysbzdm=0, STRING djdm=35001001, STRING ygzdbh=, UNIXTIME_MICROS fxrq=NULL, STRING djbbbh=030600201907, STRING bsjfbz=0, STRING jbdfjsfsdm=0, INT64 czny=0, STRING dyzh=062200003500063, STRING yhbh=062200003500063, INT64 zjsl=1, DOUBLE ygftdl=0.0, DOUBLE cdzxldf=0.0, INT64 jtnf=0, STRING dybh=0622000035000631, STRING glyskhfsdm=0, DOUBLE wgtbdl=0.0, DOUBLE nljdl=0.0, STRING jslxdm=1, STRING dqbm=030600, STRING tqbs=0620004032, DOUBLE wgbsxyz=0.0, STRING jldydjdm=02, DOUBLE ydrl=4.0, STRING yhztdm=1, STRING czrbs=admin, DOUBLE ygfbkjdl=0.0, DOUBLE ygddl=0.0, STRING scgzdbh=0616908782015031, STRING hsztdm=50, DOUBLE mfdl=0.0, DOUBLE ygtbdl=0.0, UNIXTIME_MICROS tcjssj=NULL, STRING yddz=**墟郡园西路北十二巷5号[001**, INT64 jldxh=1, STRING glbh=062200003500063, STRING yhmc=邹*欢, STRING xlxdbs=030600000001520, INT64 ycbcs=1, STRING cjsbz=0, INT64 dfny=202003, UNIXTIME_MICROS kfrq=1970-01-19T08:45:40.014000Z, DOUBLE scdl=47.0, UNIXTIME_MICROS sccbrq=1970-01-19T07:43:12.000000Z, UNIXTIME_MICROS cbrq=1970-01-19T08:29:16.800000Z, STRING cbqdbh=20000010, INT64 ydts=32, DOUBLE ygbsdl=0.0, DOUBLE jfdl=487.0, STRING cbzq=1, DOUBLE tcljdf=0.0, STRING xsjsfsdm=0, STRING zbjldbh=, STRING jldztdm=1
          val strings: Array[String] = resultStr.split(", ")
          val jSONObject = new JSONObject()
          for (elem <- strings) {
            val words: Array[String] = elem.split(" ")
            val resultArray: Array[String] = words(1).split("=")
            if(resultArray.size == 2){
              jSONObject.put(resultArray(0),resultArray(1))
            }else{
              jSONObject.put(resultArray(0),"")
            }
          }
          ctx.collect(jSONObject.toString)
        }
      }
    }

    override def cancel(): Unit = {

    }
  }
}
