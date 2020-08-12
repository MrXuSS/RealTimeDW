package com.haiyi.userbehavior.app

import java.text.SimpleDateFormat
import java.util

import org.apache.flink.api.common.functions.{AggregateFunction, RichAggregateFunction}
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
 * @create 2020-08-12
 *  每1min查看过去1hour的数据的top3， 与之前做的商品热度一样，这个是练习巩固
 */
object HotItemTop3 {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    env.readTextFile("UserBehaviorHandle/src/main/resources/apachetest.log")
        .map(line=>{
          val words = line.split(" ")
          val simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy:HH:mm:ss")
          val ts = simpleDateFormat.parse(words(3)).getTime
          HotItem(words(0),ts.toString,words(6))
        })
        .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[HotItem](Time.seconds(2)) {
          override def extractTimestamp(element: HotItem): Long = element.ts.toLong
        })
        .keyBy(_.url)
        .timeWindow(Time.minutes(60),Time.seconds(60))
        .aggregate(new MyAgg, new MyWin)
        .keyBy(_.windowEnd)
        .process(new MyKeyProcess)
        .print()


    env.execute("HotItemTop3")
  }

  class MyKeyProcess extends KeyedProcessFunction[Long,UrlCountView,String]{

    lazy val hotItems: ListState[UrlCountView] = getRuntimeContext.getListState(
        new ListStateDescriptor[UrlCountView]("hotItems", Types.of[UrlCountView])
    )

    override def processElement(value: UrlCountView,
                                ctx: KeyedProcessFunction[Long, UrlCountView, String]#Context,
                                out: Collector[String]): Unit = {

      hotItems.add(value)

      ctx.timerService().registerEventTimeTimer(value.windowEnd + 1)
    }

    override def onTimer(timestamp: Long,
                         ctx: KeyedProcessFunction[Long, UrlCountView, String]#OnTimerContext,
                         out: Collector[String]): Unit = {


      val buffer: ListBuffer[UrlCountView] = new ListBuffer()

      // 静态导入,不然buffer无法使用add()
      import scala.collection.JavaConversions._

      val iter: util.Iterator[UrlCountView] = hotItems.get().iterator()
      while (iter.hasNext){
        buffer.add(iter.next())
      }

      hotItems.clear()

      val countViews = buffer.sortBy(_.count)(Ordering.Long.reverse).take(3)

      val stringBuilder = new StringBuilder
      stringBuilder.append("当前时间："+timestamp+"\n")
      stringBuilder.append("=========================\n")
      for (elem <- countViews) {
        stringBuilder.append(elem.toString+"\n")
      }
      stringBuilder.append("===========================\n")

      out.collect(stringBuilder.toString())

    }


  }

  class MyWin extends ProcessWindowFunction[Long,UrlCountView,String,TimeWindow]{
    override def process(key: String,
                         context: Context,
                         elements: Iterable[Long],
                         out: Collector[UrlCountView]): Unit = {
      out.collect(UrlCountView(key, elements.iterator.next(), context.window.getEnd))
    }
  }

  class MyAgg extends AggregateFunction[HotItem,Long,Long]{
    override def createAccumulator(): Long = 0

    override def add(value: HotItem, accumulator: Long): Long = accumulator + 1

    override def getResult(accumulator: Long): Long = accumulator

    override def merge(a: Long, b: Long): Long = a + b
  }

  case class UrlCountView(
                         url:String,
                         count:Long,
                         windowEnd:Long
                         )

  case class HotItem(
                    ip:String,
                    ts:String,
                    url:String
                    )

}
