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
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.immutable
import scala.util.Random

/**
 * @Author:XuChengLei
 * @Date:2020-08-05
 *
 */
object AllowLateTest2 {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    env.socketTextStream("192.168.2.201",9999,'\n')
        .map(line=>{
          val strings: Array[String] = line.split(" ")
          (strings(0),strings(1).toLong * 1000)
        })
        .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[(String, Long)](Time.seconds(5)) {
          override def extractTimestamp(element: (String, Long)) = element._2
        })
        .keyBy(_._1)
        .timeWindow(Time.seconds(5))
        .allowedLateness(Time.seconds(5))
        .process(new UpdateWindowCountFunction)
        .print()

    env.execute("TriggerTest")
  }

  class UpdateWindowCountFunction extends ProcessWindowFunction[(String, Long),String,String,TimeWindow]{
    // 会在水位线没过窗口关闭时间的时候执行；在在迟到元素到来时执行。
    override def process(key: String,
                         context: Context,
                         elements: Iterable[(String, Long)],
                         out: Collector[String]): Unit = {
      val count: Int = elements.count(_ => true)

      val isUpdate: ValueState[Boolean] = context.windowState.getState(
        new ValueStateDescriptor[Boolean]("isUpdate", Types.of[Boolean])
      )

      if(!isUpdate.value()){
        // isUpdate的状态为空，说明是第一次执行，及水位线没过窗口关闭时间的时候
        out.collect((key,count,context.window.getEnd,"first").toString())
        isUpdate.update(true)
      }else{
        // isUpdate的状态不为空，说明水位线已经没过窗口结束时间，处理的是迟到元素
        out.collect((key,count,context.window.getEnd,"update").toString())
      }
    }
  }
}
