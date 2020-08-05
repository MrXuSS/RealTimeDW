package com.haiyi.app

import java.util.Calendar

import org.apache.flink.api.common.state.StateTtlConfig.TimeCharacteristic
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}

import scala.collection.immutable
import scala.util.Random
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
 * @Author:XuChengLei
 * @Date:2020-08-04
 *
 */
object KeyedProcessFunctionTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    env.addSource(new SensorReadingSource)
        .keyBy(_.id)
        .process(new alert)
        .print()

    env.execute("KeyedProcessFunctionTest")
  }

  class alert extends KeyedProcessFunction[String,SensorReading,String]{
    // 保存上一个传感器温度
    lazy val lastTemp:ValueState[Double] = getRuntimeContext
      .getState(new ValueStateDescriptor[Double]("lastTemp",Types.of[Double]))
    //  保存注册的定时器的时间戳
    lazy val currentTimer:ValueState[Long] = getRuntimeContext.getState(
      new ValueStateDescriptor[Long]("timer",Types.of[Long])
    )

    override def processElement(value: SensorReading,
                                ctx: KeyedProcessFunction[String, SensorReading, String]#Context,
                                out: Collector[String]): Unit = {

      val preTemp: Double = lastTemp.value()

      lastTemp.update(value.temperature)

      val curTimerTimestamp: Long = currentTimer.value()
      if(preTemp == 0.0 || value.temperature < preTemp){ //
        ctx.timerService().deleteProcessingTimeTimer(curTimerTimestamp)
        currentTimer.clear()
      }else if(value.temperature > preTemp && curTimerTimestamp == 0){
        val timeTs: Long = ctx.timerService().currentProcessingTime() + 1000
        ctx.timerService().registerProcessingTimeTimer(timeTs)
        currentTimer.update(timeTs)
      }
    }

    override def onTimer(timestamp: Long,
                         ctx: KeyedProcessFunction[String, SensorReading, String]#OnTimerContext,
                         out: Collector[String]): Unit = {
      out.collect(ctx.getCurrentKey+"温度上升")
      currentTimer.clear()
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
