package com.haiyi.userbehavior.app

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.{ProcessAllWindowFunction, ProcessWindowFunction}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.util.Random

/**
 * @author Mr.Xu
 * @create 2020-08-13
 *  App不分渠道统计， 每2s统计之前10sApp的事件（点击，浏览）数量
 */
object AppMarketingCount {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    env.addSource(new SimulatedEventSource)
        .assignAscendingTimestamps(_.timestamp)
        .filter(_.behavior != "UNINSTALL")
        .map(userBehavior => {
          userBehavior.behavior
        })
        .timeWindowAll(Time.seconds(10),Time.seconds(2))
        .process(new MyWinProcess)
        .print()

    env.execute("AppMarketingCount")
  }

  class MyWinProcess extends ProcessAllWindowFunction[String,(String, Long, Long),TimeWindow]{
    override def process(context: Context,
                         elements: Iterable[String],
                         out: Collector[(String, Long, Long)]): Unit = {

      val behavior = elements.iterator.next()
      out.collect((behavior, elements.size, context.window.getEnd))
    }
  }

  case class MarketingUserBehavior(userId: String,
                                   behavior: String,
                                   channel: String,
                                   timestamp: Long)
  // 自定义数据源
  class SimulatedEventSource extends RichParallelSourceFunction[MarketingUserBehavior]
  {
    var running = true
    val channelSet: Seq[String] = Seq("AppStore", "XiaomiStore", "HuaweiStore", "weibo",
      "wechat", "tieba")
    val behaviorTypes: Seq[String] = Seq("BROWSE", "CLICK", "PURCHASE", "UNINSTALL")
    val rand: Random = Random
    override def run(ctx: SourceContext[MarketingUserBehavior]): Unit = {
      val maxElements = Long.MaxValue
      var count = 0L
      while (running && count < maxElements) {
        val id = UUID.randomUUID().toString
        val behaviorType = behaviorTypes(rand.nextInt(behaviorTypes.size))
        val channel = channelSet(rand.nextInt(channelSet.size))
        val ts = System.currentTimeMillis()
        ctx.collectWithTimestamp(MarketingUserBehavior(id, behaviorType, channel, ts),
          ts)
        count += 1
        TimeUnit.MILLISECONDS.sleep(5L)
      }
    }
    override def cancel(): Unit = running = false
  }

}
