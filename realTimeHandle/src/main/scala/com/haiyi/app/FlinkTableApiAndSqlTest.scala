//package com.haiyi.app
//
//
//import java.sql.Timestamp
//import java.util.{Calendar, Properties}
//
//import com.alibaba.fastjson.JSON
//import com.haiyi.entity.Hs_jldxx
//import org.apache.flink.api.common.serialization.SimpleStringSchema
//import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
//import org.apache.flink.api.java.io.jdbc.JDBCInputFormat
//import org.apache.flink.api.java.typeutils.RowTypeInfo
//import org.apache.flink.streaming.api.TimeCharacteristic
//import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
//import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
//import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
//import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
//import org.apache.flink.streaming.api.scala._
//import org.apache.flink.streaming.api.windowing.time.Time
//import org.apache.flink.table.api.scala._
//import org.apache.flink.api.scala._
//import org.apache.flink.table.api.{Table, TableEnvironment}
//import org.apache.flink.table.api.scala.StreamTableEnvironment
//import org.apache.flink.types.Row
//
//import scala.collection.immutable
//import scala.util.Random
//
///**
// * @Author:XuChengLei
// * @Date:2020-07-28
// *
// */
//object FlinkTableApiAndSqlTest {
//  def main(args: Array[String]): Unit = {
////    val properties = new Properties()
////    properties.setProperty("bootstrap.servers", "node1:9092")
////    properties.setProperty("group.id", "consumer-group12311")
////    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
////    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
////    properties.setProperty("auto.offset.reset", "earliest")
//
//    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
//    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
//    env.setParallelism(1)
//
//    val tableEnv: StreamTableEnvironment = TableEnvironment.getTableEnvironment(env)
//
//    // 1.获取kafka中的json字符串
//    //val stream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("threadTest", new SimpleStringSchema(), properties))
//
////
////    val hs_jldxxStream: DataStream[Hs_jldxx] = stream.map {
////      jsonStr => {
////        val hs_jldxx: Hs_jldxx = JSON.parseObject(jsonStr, classOf[Hs_jldxx])
////        hs_jldxx
////      }
////    }
////
////    val assignTimeAndWaterMarksStream: DataStream[Hs_jldxx] = hs_jldxxStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[Hs_jldxx](Time.seconds(5)) {
////      override def extractTimestamp(element: Hs_jldxx) = {
////        element.JLDBH.toLong
////      }
////    })
////
////    val table: Table = tableEnv.fromDataStream(assignTimeAndWaterMarksStream)
//////    val filterTable: Table = table.select("GZDBH,JLDBH").filter("GZDBH != '0'")
//////    val appendStream: DataStream[(String, String)] = filterTable.toAppendStream[(String, String)]
//////    appendStream.print()
////
////
////    val windowTable: Table = table.window(Tumble over 5000.millis on 'JLDBH as 'ts).groupBy('GZDBH,'ts).select('GZDBH,'GZDBH.count)
////    val resultStream: DataStream[(Boolean, (String, String))] = windowTable.toRetractStream[(String, String)]
////    resultStream.filter(_._1).print()
//
//
//    val stream: DataStream[SensorReading] = env.addSource(new SensorReadingSource)
//
//    val timeAndWaterStream: DataStream[SensorReading] = stream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(5)) {
//      override def extractTimestamp(element: SensorReading) = {
//        element.timestamp
//      }
//    })
//
//    val table: Table = tableEnv.fromDataStream(timeAndWaterStream,'id,'ts.rowtime,'temperature)
//
//
//
//
////    val groupByTable: Table = table.window(Tumble over 5000.millis on 'timestamp as 'ts).groupBy('id, 'ts).select('id, 'temperature.avg)
////
////    val resultStream: DataStream[(Boolean, (String, Double))] = groupByTable.toRetractStream[(String, Double)]
////
////    resultStream.filter(_._1).print()
//
////    val resultTable: Table = tableEnv.sqlQuery("select id,avg(temperature),min(ts) from "+ table +" group by id ,Tumble(ts,interval '5' SECOND)")
////    resultTable.toRetractStream[(String,Double,Timestamp)].filter(_._1).print()
//
//
////    sensor_1	100	10
////    sensor_2	101	10
////    sensor_3	102	10
////    sensor_4	103	10
////    sensor_5	104	10
//
//    val typeInfo = new RowTypeInfo(TypeInformation.of(classOf[String]),
//                                    TypeInformation.of(classOf[Long]),
//                                    TypeInformation.of(classOf[Double]))
//
//    val jdbcInputFormat: JDBCInputFormat = JDBCInputFormat.buildJDBCInputFormat()
//      .setDrivername("com.mysql.jdbc.Driver")
//      .setDBUrl("jdbc:mysql://192.168.2.201:3306/NWPMSKF")
//      .setUsername("root")
//      .setPassword("123456")
//      .setQuery("select * from SensorReading")
//      .setRowTypeInfo(typeInfo)
//      .finish()
//
//    val jdbcStream: DataStream[Row] = env.createInput(jdbcInputFormat)
//
//    val tableMysql: Table = tableEnv.fromDataStream(jdbcStream)
//
//    val queryTableMysql: Table = tableEnv.sqlQuery("select * from SensorReading")
//
//
//
//
//    env.execute("FlinkTableApiAndSqlTestApp")
//  }
//
//  case class SensorReading(id:String,
//                           timestamp:Long,
//                           temperature:Double)
//
//  class SensorReadingSource extends RichParallelSourceFunction[SensorReading]{
//    var running:Boolean = true
//
//    override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit =  {
//      val random = new Random()
//
//      val taskIdx: Int = this.getRuntimeContext.getIndexOfThisSubtask
//
//      val curFtemp: immutable.IndexedSeq[(String, Double)] = (1 to 10).map {
//        i => ("sensor_" + (taskIdx * 10 + i), 65 + (random.nextGaussian() * 20))
//      }
//
//      while (running){
//        curFtemp.map(t=>(t._1,t._2+(random.nextGaussian()),t._2))
//        val ts: Long = Calendar.getInstance.getTimeInMillis
//        curFtemp.foreach(t => sourceContext.collect(SensorReading(t._1,ts,t._2)))
//        Thread.sleep(100)
//      }
//    }
//
//    override def cancel(): Unit = {
//      running = false
//    }
//  }
//}
