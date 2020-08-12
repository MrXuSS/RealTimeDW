package com.haiyi.app


import java.io.File
import java.util.Calendar

import com.haiyi.app.FlinkCEPTest.SensorReading
import groovy.lang.{GroovyClassLoader, GroovyObject}
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._

import scala.collection.{Map, immutable}
import scala.util.Random

/**
 * @Author:XuChengLei
 * @Date:2020-08-07
 *
 */
object FlinkCEP_Groovy_Test {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val objectMapper = new ObjectMapper()

    val loader = new GroovyClassLoader
    val file = new File("realTimeHandle/src/main/scala/com/haiyi/app/Rule.groovy")
    val aClass: Class[_] = loader.parseClass(file)
    val groovyObject: GroovyObject = aClass.newInstance().asInstanceOf[GroovyObject]
    val pattern = groovyObject.invokeMethod("run", null).asInstanceOf[Pattern[SensorReading,SensorReading]]

   val stream: DataStream[SensorReading] = env.addSource(new SensorReadingSource).assignAscendingTimestamps(_.timestamp)

    val patternStream: PatternStream[SensorReading] = CEP.pattern(stream, pattern)

    val resultStream: DataStream[SensorReading] = patternStream.select(
      (map: Map[String, Iterable[SensorReading]]) => {
      val sensorReading: SensorReading = map.getOrElse("begin", null).iterator.next()
        println(sensorReading)
      sensorReading
    })

    resultStream.print()

    env.execute("FlinkCEP_Groovy_Test")
  }

  case class SensorReading(id:String,
                           timestamp:Long,
                           temperature:Double)

  class SensorReadingSource extends RichParallelSourceFunction[SensorReading]{
    var running:Boolean = true

    override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit =  {
      val random = new Random()

      val taskIdx: Int = this.getRuntimeContext.getIndexOfThisSubtask

      var curFtemp: immutable.IndexedSeq[(String, Double)] = (1 to 10).map {
        i => ("sensor_" + (taskIdx * 10 + i), 65 + (random.nextGaussian() * 20))
      }

      while (running){
        curFtemp = curFtemp.map(t=>(t._1,t._2+(random.nextGaussian()*0.5)))
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
