package com.haiyi.userbehavior.app

import org.apache.flink.api.common.functions.{AggregateFunction, RichAggregateFunction}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
/**
 * @author Mr.Xu
 * @create 2020-08-13
 * 分省份统计广告
 */
object AdStatisticsByGeo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    env.fromElements("1,1,ShanDong,YanTai,1","2,2,JiangSu,SuZhou,2")
        .map(line=>{
          val words = line.split(",")
          AdClickLog(words(0).toLong, words(1).toLong, words(2), words(3), words(4).toLong * 1000)
        })
        .assignAscendingTimestamps(_.timestamp)
        .keyBy(_.province)
        .timeWindow(Time.seconds(5))
        .aggregate(new MyAggera, new MyWin)  // 使用预聚合，减少窗口的压力
        .print()

    env.execute("AdStatisticsByGeo")
  }

  class MyWin extends ProcessWindowFunction[Long, CountByProvince, String, TimeWindow]{
    override def process(key: String,
                         context: Context,
                         elements: Iterable[Long],
                         out: Collector[CountByProvince]): Unit = {
      out.collect(CountByProvince(context.window.getEnd.toString, key, elements.iterator.next()))
    }
  }

  class MyAggera extends  AggregateFunction[AdClickLog, Long, Long]{
    override def createAccumulator(): Long = 0

    override def add(value: AdClickLog, accumulator: Long): Long = accumulator + 1

    override def getResult(accumulator: Long): Long = accumulator

    override def merge(a: Long, b: Long): Long = a + b
  }

  case class AdClickLog(userId: Long,
                        adId: Long,
                        province: String,
                        city: String,
                        timestamp: Long)

  case class CountByProvince(windowEnd: String,
                             province: String,
                             count: Long)
}
