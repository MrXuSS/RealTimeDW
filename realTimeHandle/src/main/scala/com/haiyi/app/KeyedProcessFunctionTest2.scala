package com.haiyi.app

import java.util.Calendar

import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.immutable
import scala.util.Random

/**
 * @Author:XuChengLei
 * @Date:2020-08-04
 *
 */
object KeyedProcessFunctionTest2 {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val stream: DataStream[SensorReading] = env.addSource(new SensorReadingSource)
      .keyBy(_.id)
      .process(new alert)

    stream.getSideOutput(new OutputTag[String]("freezing")).print()

    env.execute("KeyedProcessFunctionTest")
  }

  class alert extends KeyedProcessFunction[String,SensorReading,SensorReading]{

    lazy val freezingAlarmOutput = new OutputTag[String]("freezing")

    override def processElement(value: SensorReading,
                                ctx: KeyedProcessFunction[String, SensorReading, SensorReading]#Context,
                                out: Collector[SensorReading]): Unit = {
      if(value.temperature < 32){
        ctx.output(freezingAlarmOutput,value.id+"温度小于32摄氏度")
      }
        out.collect(value)
    }
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
