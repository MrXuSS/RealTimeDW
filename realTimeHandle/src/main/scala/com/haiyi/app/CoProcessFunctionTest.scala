package com.haiyi.app

import java.util.Calendar

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
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
object CoProcessFunctionTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val stream: DataStream[SensorReading] = env.addSource(new SensorReadingSource)

    val stream2: DataStream[(String, Long)] = env.fromElements(
      ("sensor_2", 1000 * 2L),
      ("sensor_7", 1000 * 5L)
    )

    stream.keyBy(_.id).connect(stream2.keyBy(_._1)).process(new ReadingFilter).print()

    env.execute("KeyedProcessFunctionTest")
  }

  class ReadingFilter extends CoProcessFunction[SensorReading,(String,Long),SensorReading]{

    lazy val switch = getRuntimeContext.getState[Boolean](
      new ValueStateDescriptor[Boolean]("switch",Types.of[Boolean])
    )

    lazy val timer = getRuntimeContext.getState[Long](
      new ValueStateDescriptor[Long]("timer",Types.of[Long])
    )

    override def processElement1(value: SensorReading,
                                 ctx: CoProcessFunction[SensorReading, (String, Long), SensorReading]#Context,
                                 out: Collector[SensorReading]): Unit = {
      if(switch.value()){
        out.collect(value)
      }
    }

    override def processElement2(value: (String, Long),
                                 ctx: CoProcessFunction[SensorReading, (String, Long), SensorReading]#Context,
                                 out: Collector[SensorReading]): Unit = {
      switch.update(true)
      val timeTs: Long = ctx.timerService().currentProcessingTime() + value._2
      val curTimerTimestamp: Long = timer.value()
      if(timeTs > curTimerTimestamp){
        ctx.timerService().deleteProcessingTimeTimer(curTimerTimestamp)
        ctx.timerService().registerProcessingTimeTimer(timeTs)
        timer.update(timeTs)
      }
    }

    override def onTimer(timestamp: Long,
                         ctx: CoProcessFunction[SensorReading, (String, Long), SensorReading]#OnTimerContext,
                         out: Collector[SensorReading]): Unit = {
      timer.clear()
      switch.clear()
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
