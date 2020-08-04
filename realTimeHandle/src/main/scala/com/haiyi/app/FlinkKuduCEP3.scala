package com.haiyi.app

import java.util

import com.alibaba.fastjson.{JSON, JSONObject}
import com.haiyi.entity.{Table_HS_JLDXX, Table_HS_LJFMX}
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.configuration.Configuration
import org.apache.flink.contrib.streaming.state.{RocksDBKeyedStateBackend, RocksDBStateBackend}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.kudu.Schema
import org.apache.kudu.client.{KuduClient, KuduPredicate, KuduScanner, KuduTable, RowResult, RowResultIterator}

import scala.collection.Map

/**
 * @Author:XuChengLei
 * @Date:2020-07-30
 *  采用 RocksDBStateBackend
 */

object FlinkKuduCEP3 {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    env.enableCheckpointing(5000)

    env.setStateBackend(new RocksDBStateBackend("file:///D:/Program/WorkSpace/IDEA_WorkSpace/checkpoint"))

    val stream: DataStream[Table_HS_JLDXX] = env
      .addSource(new MyKuduSource)
      .map(JSON.parseObject(_, classOf[Table_HS_JLDXX]))

    val resultStr: DataStream[(Table_HS_JLDXX,String)] = stream
      .keyBy("jldbh","cjsj","gzdbh","ywlbdm")
      .map(new MyMapJoin)

    val cepPattern: Pattern[(Table_HS_JLDXX, String), (Table_HS_JLDXX, String)] = Pattern
      .begin[(Table_HS_JLDXX, String)]("begin")
      .where {
        t => {
          t._1.dqbm == "030600" && t._1.jfdl.toDouble > 1000 && t._1.jfdl.toDouble > t._1.jfrl.toDouble * 100
        }
      }

    val patternStream: PatternStream[(Table_HS_JLDXX, String)] = CEP.pattern(resultStr, cepPattern)

    patternStream.select((map:Map[String,Iterable[(Table_HS_JLDXX,String)]])=>{
      val tuple: (Table_HS_JLDXX, String) = map.getOrElse("begin", null).iterator.next()
      tuple
    })

    resultStr.print()

    env.execute("FlinkKuduCEP3")
  }

  class MyMapJoin extends RichMapFunction[Table_HS_JLDXX,(Table_HS_JLDXX,String)]{

    var dataOfTable_LC_CBXX:MapState[String,String] = _
    private val masterAddr:String = "172.19.138.6,172.19.138.7,172.19.138.8"
    private val tableName:String = "LC_CBXX"
    private var client: KuduClient = _
    private var kuduTable:KuduTable = _
    private var kuduScanner:KuduScanner = _

    override def open(parameters: Configuration): Unit = {
      val descriptor = new MapStateDescriptor[String, String]("dataOfLcCbxx",
        TypeInformation.of(classOf[String]),
        TypeInformation.of(classOf[String]))

      dataOfTable_LC_CBXX = getRuntimeContext.getMapState(descriptor)
      client = new KuduClient.KuduClientBuilder(masterAddr).build()
      kuduTable = client.openTable(tableName)
    }

    override def map(value: Table_HS_JLDXX): (Table_HS_JLDXX, String) = {
      val key:String = value.jldbh+value.dfny+value.gzdbh+value.dqbm
      if(dataOfTable_LC_CBXX.contains(key)){
        val maxStr: String = dataOfTable_LC_CBXX.get(key)
        (value,maxStr)
      }else{
        val resultStr: String = readKudu(value.jldbh, value.dfny, value.gzdbh, value.dqbm)
        dataOfTable_LC_CBXX.put(key,resultStr)
        (value,resultStr)
      }
    }

    override def close(): Unit = {
      kuduScanner.close()
      client.close()
    }

    def readKudu(jldbh:String,dfny:String,gzdbh:String,dqbm:String):String= {
      val schema: Schema = kuduTable.getSchema
      val jldbhPredicate: KuduPredicate = KuduPredicate.newComparisonPredicate(schema.getColumn("jldbh"), KuduPredicate.ComparisonOp.EQUAL, jldbh)
      val dfnyPredicate: KuduPredicate = KuduPredicate.newComparisonPredicate(schema.getColumn("dfny"), KuduPredicate.ComparisonOp.EQUAL, dfny.toInt)
      val gzdbhPredicate: KuduPredicate = KuduPredicate.newComparisonPredicate(schema.getColumn("gzdbh"), KuduPredicate.ComparisonOp.EQUAL, gzdbh)
      val dqbmPredicate: KuduPredicate = KuduPredicate.newComparisonPredicate(schema.getColumn("dqbm"), KuduPredicate.ComparisonOp.EQUAL, dqbm)

      // 获取指定列的值
      val list = new util.ArrayList[String]()
      list.add("zcbh")

      val scanner: KuduScanner = client.newScannerBuilder(kuduTable)
        .setProjectedColumnNames(list)
        .addPredicate(jldbhPredicate)
        .addPredicate(dfnyPredicate)
        .addPredicate(gzdbhPredicate)
        .addPredicate(dqbmPredicate)
        .build()

      var maxStr = ""

      while (scanner.hasMoreRows){
        val iterator: RowResultIterator = scanner.nextRows()
        while (iterator.hasNext){
          val result: RowResult = iterator.next()
          val zcbhStr: String = result.rowToString().split("=")(1)
          if(zcbhStr > maxStr){
            maxStr = zcbhStr
          }
        }
      }
      maxStr
    }
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
