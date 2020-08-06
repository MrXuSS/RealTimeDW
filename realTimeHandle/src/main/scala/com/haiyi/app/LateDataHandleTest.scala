package com.haiyi.app

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.functions.aggfunctions.CountAggFunction
import org.apache.flink.util.Collector

/**
 * @Author:XuChengLei
 * @Date:2020-08-05
 *
 */
object LateDataHandleTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val stream: DataStream[(String, Long)] = env.socketTextStream("localhost", 9999, '\n')
      .map(line => {
        val strings: Array[String] = line.split(" ")
        (strings(0), strings(1).toLong * 1000)
      })
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[(String, Long)](Time.seconds(2)) {
        override def extractTimestamp(element: (String, Long)) = element._2
      })

//    stream
//        .keyBy(_._1)
//        .timeWindow(Time.seconds(2))
//        .sideOutputLateData(new OutputTag[(String, Long)]("later"))
//        .process(???)

//    val lateStream: DataStream[(String, Long)] = stream.getSideOutput(new OutputTag[(String, Long)]("later"))


    env.execute("LateDataHandleTest")
  }

  class LateStream extends ProcessFunction[(String,Long),(String,Long)]{

    private val lateOutput = new OutputTag[(String, Long)]("late")

    override def processElement(value: (String, Long),
                                ctx: ProcessFunction[(String, Long), (String, Long)]#Context,
                                out: Collector[(String, Long)]): Unit = {

      if(value._2 < ctx.timerService().currentWatermark()){
        ctx.output(lateOutput,value)
      }else{
        out.collect(value)
      }
    }
  }

}
