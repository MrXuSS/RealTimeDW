package com.haiyi.app

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * @Author:XuChengLei
 * @Date:2020-07-28
 *
 */
object FlinkKeyByTest2 {
    def main(args: Array[String]): Unit = {
      val env = StreamExecutionEnvironment.getExecutionEnvironment
      env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
      env.setParallelism(3)

      env.addSource(new SensorReadingSource)
          .assignAscendingTimestamps(_.timestamp)
          .keyBy(_.id)
          .timeWindow(Time.seconds(5),Time.seconds(2))
          .aggregate(new MyAggerate)
          .keyBy(_._2)
          .print()


      env.execute("FlinkKeyByTest2")
    }
  }

  class MyAggerate extends AggregateFunction[SensorReading,(String, Double),(String,Double)]{
    override def createAccumulator(): (String, Double) = ("", 0)

    override def add(value: SensorReading,
                     accumulator: (String, Double)): (String, Double) = {
      (value.id, value.temperature + accumulator._2)
    }

    override def getResult(accumulator: (String, Double)): (String, Double) = {
      accumulator
    }

    override def merge(a: (String, Double), b: (String, Double)): (String, Double) = {
      (a._1, a._2 + b._2)
    }
}
