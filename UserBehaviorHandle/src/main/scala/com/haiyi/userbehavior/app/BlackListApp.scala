package com.haiyi.userbehavior.app

import com.haiyi.userbehavior.app.AdStatisticsByGeo.AdClickLog
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
 * @author Mr.Xu
 * @create 2020-08-13
 *  一天内连续点击一百次，将数据传入侧输出，并不在主流中继续输出（未完成，明天继续）
 */
object BlackListApp {
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
      .keyBy(r => (r.userId, r.adId))
        .process()


    env.execute("BlackListApp")
  }

  class MyBlackListProcess extends KeyedProcessFunction[(Long, Long), AdClickLog, AdClickLog]{
    //  保存当天的个数
    lazy val countState: ValueState[Long] = getRuntimeContext.getState(new ValueStateDescriptor[Long]("count-state", Types.of[Long]))
    // 是不是第一次发给黑名单
    lazy val firstState: ValueState[Boolean] = getRuntimeContext.getState(new ValueStateDescriptor[Boolean]("first-state", Types.of[Boolean]))

    // 到这儿



    override def processElement(value: AdClickLog,
                                ctx: KeyedProcessFunction[(Long, Long), AdClickLog, AdClickLog]#Context,
                                out: Collector[AdClickLog]): Unit = {
      val clickNum = countState.value()
      if(clickNum > 100){

      }
    }
  }

  case class AdClickLog(userId: Long,
                        adId: Long,
                        province: String,
                        city: String,
                        timestamp: Long)

}
