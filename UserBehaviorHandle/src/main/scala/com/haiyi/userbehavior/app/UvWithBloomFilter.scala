package com.haiyi.userbehavior.app

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.triggers.{Trigger, TriggerResult}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import redis.clients.jedis.Jedis
/**
 * @author Mr.Xu
 * @create 2020-08-13
 *  利用布隆过滤器 实现超级大的数据的过滤
 *  利用redis实现一个布隆过滤器（Bloom）
 *  利用触发器，每来一条数据就进行计算和清除
 *  在MyProcess123中对数据进行储存
 *  使用redis的 bitMap来实现布隆过滤器
 */
object UvWithBloomFilter {
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
        .map(r =>("dummkey",r.userId))  // 确保数据都在一个key上
        .keyBy(_._1)
        .timeWindow(Time.minutes(10))
        .trigger(new MyTrigger)
        .process(new MyProcess123)
        .print()

    env.execute("UvWithBloomFilter")
  }

  class MyProcess123 extends ProcessWindowFunction[(String,Long),(Long,Long),String,TimeWindow]{

    lazy val jedis = new Jedis("node1", 6379)
    lazy val bloom = new Bloom(1 << 29)

    override def process(key: String,
                         context: Context,
                         elements: Iterable[(String, Long)],
                         out: Collector[(Long, Long)]): Unit = {
      val storeKey = context.window.getEnd.toString
      var count = 0L
      val countResult = jedis.hget("UvCountHashTable", storeKey)
      if(countResult != null){
        count = countResult.toLong
      }

      val userId = elements.last._2
      val offset = bloom.hash(userId.toString, 61)

      val isExist = jedis.getbit(storeKey, offset)
      if(!isExist){
        jedis.setbit(storeKey,offset,true)
        jedis.hset("UvCountHashTable",storeKey,(count + 1).toString)
      }
      out.collect(storeKey.toLong, count + 1)
    }
  }

  class MyTrigger extends Trigger[(String,Long),TimeWindow]{
    override def onElement(element: (String, Long),
                           timestamp: Long,
                           window: TimeWindow,
                           ctx: Trigger.TriggerContext): TriggerResult = {
      // 来一条数据就进行处理和销毁
      TriggerResult.FIRE_AND_PURGE
    }

    override def onProcessingTime(time: Long,
                                  window: TimeWindow,
                                  ctx: Trigger.TriggerContext): TriggerResult = {
      TriggerResult.CONTINUE
    }

    override def onEventTime(time: Long,
                             window: TimeWindow,
                             ctx: Trigger.TriggerContext): TriggerResult = {

      TriggerResult.CONTINUE
    }

    override def clear(window: TimeWindow,
                       ctx: Trigger.TriggerContext): Unit = {
    }
  }

  class Bloom(size:Long) extends Serializable{
    private val cap = size

    def hash(value:String, seed:Int): Long ={
      var result = 0
      for (i <- 0 until value.length){
        result = result * seed + value.charAt(i)
      }
      (cap - 1) & result
    }
  }

  case class UserBehavior(
                           userId:Long,
                           itemId:Long,
                           categoryId:Int,
                           behavior:String,
                           timestamp:Long
                         )

}
