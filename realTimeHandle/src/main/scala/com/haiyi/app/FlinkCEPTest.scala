package com.haiyi.app

import java.util
import java.util.Calendar

import org.apache.flink.cep.{PatternSelectFunction, PatternTimeoutFunction}
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.api.scala.map
import org.apache.flink.util.Collector

import scala.collection.{Map, immutable}
import scala.util.Random
/**
 * @Author:XuChengLei
 * @Date:2020-07-29
 *
 */
object FlinkCEPTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val stream: DataStream[SensorReading] = env.addSource(new SensorReadingSource).assignAscendingTimestamps(_.timestamp)

    val pattern: Pattern[SensorReading, SensorReading] = Pattern
      .begin[SensorReading]("x")
      .where(_.temperature > 10)
      .next("y")
      .where(_.temperature > 10)
    val resultStream: PatternStream[SensorReading] = CEP.pattern(stream, pattern)

    val value: DataStream[(SensorReading, SensorReading)] = resultStream.select((map: Map[String, Iterable[SensorReading]]) => {
      val sensorReading1: SensorReading = map.getOrElse("x", null).iterator.next()
      val sensorReading2: SensorReading = map.getOrElse("y", null).iterator.next()
      (sensorReading1, sensorReading2)
    })

    value.print()


//    val logEventStream: DataStream[LogEvent] = env.fromCollection(List(
//      LogEvent("1", "192.168.1.1", "fail", "17863523153"),
//      LogEvent("1", "192.168.1.2", "success", "17863523153"),
//      LogEvent("1", "192.168.1.3", "fail", "17863523153"),
//      LogEvent("2", "192.168.1.4", "fail", "17863523153"),
//      LogEvent("2", "192.168.1.5", "fail", "17863523153")
//    )).assignAscendingTimestamps(_.eventTime.toLong * 1000)
//
//    //val pattern: Pattern[LogEvent, LogEvent] = Pattern.begin[LogEvent]("x").where(_.userId == "1").where(_.ip == "192.168.1.1")
//    val pattern: Pattern[LogEvent, LogEvent] = Pattern
//        .begin[LogEvent]("x")
//       .where{obj =>obj.userId == "1"}
//
//    val value: PatternStream[LogEvent] = CEP.pattern(logEventStream.keyBy(_.userId), pattern)
//
//    value.select((pattern: Map[String, Iterable[LogEvent]]) => {
//      val event: LogEvent = pattern.getOrElse("x", null).iterator.next()
//      event
//      }).print()


//    val pattern: Pattern[LogEvent, LogEvent] = Pattern
//      .begin[LogEvent]("x").where(_.eventStatus == "fail")
//      .next("y").where(_.eventStatus == "fail")
//      .within(Time.seconds(10))
//
//    val patternStream: PatternStream[LogEvent] = CEP.pattern(logEventStream.keyBy(_.userId), pattern)



    // Map一定要用 import scala.collection.Map
//    patternStream.select((map:Map[String,Iterable[LogEvent]])=>{
//      val x: LogEvent = map.getOrElse("x", null).iterator.next()
//      val y: LogEvent = map.getOrElse("y", null).iterator.next()
//
//      (x.ip,y.ip)
//    }).print()

//    val timeOut = new OutputTag[String]("timeout")
//
//    val timeFunc = (map:Map[String,Iterable[LogEvent]],ts:Long,out:Collector[String]) =>{
//      val head: LogEvent = map("x").head
//      out.collect(head.toString+"迟到了")
//    }
//
//    val selectFunc = (map:Map[String,Iterable[LogEvent]],out:Collector[String])=>{
//      out.collect(map.toString())
//    }
//
//    val outputStream: DataStream[String] = patternStream.flatSelect(timeOut)(timeFunc)(selectFunc)
//
//    outputStream.print()
//    outputStream.getSideOutput(timeOut).print()

    env.execute("FlinkCEPTest")

  }

  //case class LogEvent(userId:String,ip:String,eventStatus:String,eventTime:String)

  case class SensorReading(id:String,
                           timestamp:Long,
                           temperature:Double)

  class SensorReadingSource extends RichParallelSourceFunction[SensorReading]{
    var running:Boolean = true

    override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit =  {
      val random = new Random()

      val taskIdx: Int = this.getRuntimeContext.getIndexOfThisSubtask

      val curFtemp: immutable.IndexedSeq[(String, Double)] = (1 to 10).map {
        i => ("sensor_" + (taskIdx * 10 + i), 65 + (random.nextGaussian() * 20))
      }

      while (running){
        curFtemp.map(t=>(t._1,t._2+(random.nextGaussian()),t._2))
        val ts: Long = Calendar.getInstance.getTimeInMillis
        curFtemp.foreach(t => sourceContext.collect(SensorReading(t._1,ts,t._2)))
        Thread.sleep(100)
      }
    }

    override def cancel(): Unit = {
      running = false
    }
  }

}
