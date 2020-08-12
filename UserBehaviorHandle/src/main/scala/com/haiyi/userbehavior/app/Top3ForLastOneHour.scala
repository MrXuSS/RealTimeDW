package com.haiyi.userbehavior.app

import java.util

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer
/**
 * @author Mr.Xu
 * @create 2020-08-11
 *    每5min统计之前一小时的top3商品
 *    基本功能实现，但是输出结果一样，明天来Debug,(解决，由于时间需要*1000变成毫秒级别的， 不然数据都会在同一个窗口内，所有输出结果一样)
 */
object Top3ForLastOneHour {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    env.readTextFile("UserBehaviorHandle/src/main/resources/UserBehavior.csv")
        .map(line => {
          val strs = line.split(",")
          UserBehavior(strs(0).toLong,strs(1).toLong,strs(2).toInt,strs(3),strs(4).toLong * 1000)
        })
        .assignTimestampsAndWatermarks(
          new BoundedOutOfOrdernessTimestampExtractor[UserBehavior](Time.seconds(2)) {
          override def extractTimestamp(element: UserBehavior): Long = element.timestamp
        })
        .filter(_.behavior == "pv")
        .keyBy(_.itemId)  // 如果使用 keyby("itemId"), 会将Key封装在Tuple（Flink的Tuple）类型中，
                          // 可以将其转为Tuple1， 使用tuple1.f0获取其中的数据
        .timeWindow(Time.hours(1),Time.minutes(5))
        .aggregate(new MyAggerate, new MyWindow)
        .keyBy(_.windowEnd)
        .process(new MyKeyProcess)
        .print()


    env.execute("Top3ForLastOneHour")
  }

  // 使用ListState多数据进行存储， 在定时器中对数据进行排序
  class MyKeyProcess extends KeyedProcessFunction[Long,ItemViewCount,String]{

    lazy val items: ListState[ItemViewCount] = getRuntimeContext.getListState(
      new ListStateDescriptor[ItemViewCount]("items", Types.of[ItemViewCount])
    )

    override def processElement(value: ItemViewCount,
                                ctx: KeyedProcessFunction[Long, ItemViewCount, String]#Context,
                                out: Collector[String]): Unit = {
      items.add(value)

      ctx.timerService().registerEventTimeTimer(value.windowEnd + 1)
    }

    override def onTimer(timestamp: Long,
                         ctx: KeyedProcessFunction[Long, ItemViewCount, String]#OnTimerContext,
                         out: Collector[String]): Unit = {

      val allItems:ListBuffer[ItemViewCount] = ListBuffer()

      import scala.collection.JavaConversions._

      val iter: util.Iterator[ItemViewCount] = items.get().iterator()
      while (iter.hasNext){
        allItems.add(iter.next())
      }

      items.clear()

      val resultViewList: ListBuffer[ItemViewCount] = allItems.sortBy(_.count)(Ordering.Long.reverse).take(3)
      val stringBuilder = new StringBuilder
      stringBuilder.append("当前时间：" + (timestamp - 1) + "\n")
          .append("=================================\n")
      for (elem <- resultViewList) {
        stringBuilder.append(elem.toString+"\n")
      }
      stringBuilder.append("=================================\n")
      out.collect(stringBuilder.toString())
    }
  }

  // 窗口内部提前聚合，减少window的压力。 （Long,Int）:(itemId，count)
  class MyAggerate extends AggregateFunction[UserBehavior,Long,Long]{
    override def createAccumulator(): Long = 0

    override def add(value: UserBehavior, accumulator: Long): Long = accumulator + 1

    override def getResult(accumulator: Long): Long = accumulator

    override def merge(a: Long, b: Long): Long = a + b
  }

  // 输出 (Long,Long.Int):(itemId, endTime, count)
  class MyWindow extends ProcessWindowFunction[Long,ItemViewCount,Long,TimeWindow]{
    override def process(key: Long,
                         context: Context,
                         elements: Iterable[Long],
                         out: Collector[ItemViewCount]): Unit = {
      out.collect(ItemViewCount(key, context.window.getEnd, elements.iterator.next()))
    }
  }

  case class ItemViewCount(
                          itemId:Long,
                          windowEnd:Long,
                          count:Long
                          )

  case class UserBehavior(
                           userId:Long,
                           itemId:Long,
                           categoryId:Int,
                           behavior:String,
                           timestamp:Long
                         )

}
