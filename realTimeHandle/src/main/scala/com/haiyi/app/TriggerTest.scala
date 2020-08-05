package com.haiyi.app

import java.util.Calendar

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.triggers.{Trigger, TriggerResult}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.immutable
import scala.util.Random

/**
 * @Author:XuChengLei
 * @Date:2020-08-05
 *
 */
object TriggerTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    env.addSource(new SensorReadingSource)
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(0)) {
        override def extractTimestamp(element: SensorReading) = element.timestamp
      })
        .keyBy(_.id)
        .timeWindow(Time.seconds(5000))
        .trigger(new MyTrigger)
        .process(new MyProcess)
        .print()

    env.execute("TriggerTest")

  }

  class MyProcess extends ProcessWindowFunction[SensorReading,String,String,TimeWindow]{
    override def process(key: String, context: Context, elements: Iterable[SensorReading], out: Collector[String]): Unit = {
      val temp: Iterable[Double] = elements.map(_.temperature)
      out.collect((key,temp.min,temp.max,context.window.getEnd).toString())
    }
  }

  class MyTrigger extends Trigger[SensorReading,TimeWindow]{

    override def onElement(element: SensorReading,
                           timestamp: Long,
                           window: TimeWindow,
                           ctx: Trigger.TriggerContext): TriggerResult = {
      val firstSeen:ValueState[Boolean] = ctx.getPartitionedState(
        new ValueStateDescriptor[Boolean]("firstSeen",Types.of[Boolean])
      )

      if(!firstSeen.value()){
        val t: Long = ctx.getCurrentWatermark + (1000 - (ctx.getCurrentWatermark % 1000))
        ctx.registerEventTimeTimer(t)
        ctx.registerEventTimeTimer(window.getEnd)
        firstSeen.update(true)
      }

      TriggerResult.CONTINUE
    }

    override def onProcessingTime(time: Long,
                                  window: TimeWindow,
                                  ctx: Trigger.TriggerContext): TriggerResult = {
      TriggerResult.CONTINUE
    }

    override def onEventTime(time: Long,
                             window: TimeWindow,
                             ctx: Trigger.TriggerContext): TriggerResult = {
      if(time == window.getEnd){
        TriggerResult.FIRE_AND_PURGE
      }else{
        val t: Long = ctx.getCurrentWatermark + (1000 - ctx.getCurrentWatermark % 1000)
        if(t < window.getEnd){
          ctx.registerEventTimeTimer(t)
        }
        TriggerResult.FIRE
      }
    }

    override def clear(window: TimeWindow,
                       ctx: Trigger.TriggerContext): Unit = {
      val firstSeen: ValueState[Boolean] = ctx.getPartitionedState(
        new ValueStateDescriptor[Boolean]("firstSeen", Types.of[Boolean])
      )
      firstSeen.clear()
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
