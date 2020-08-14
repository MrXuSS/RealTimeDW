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
 *  一天内点击一百次，将数据传入黑名单侧输出，并不在主流中继续输出（未完成，明天继续）
 *  测试：一天内点击三次，将数据传入到黑名单,测试成功
 */
object BlackListApp {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    val stream = env.fromElements("1,1,ShanDong,YanTai,1",
                                          "2,2,JiangSu,SuZhou,2",
                                          "1,1,ShanDong,YanTai,3",
                                          "1,1,ShanDong,YanTai,4",
                                          "1,1,ShanDong,YanTai,5")
      .map(line => {
        val words = line.split(",")
        AdClickLog(words(0).toLong, words(1).toLong, words(2), words(3), words(4).toLong * 1000)
      })
      .assignAscendingTimestamps(_.timestamp)
      .keyBy(r => (r.userId, r.adId))
      .process(new MyBlackListProcess)

    stream.print()
    stream.getSideOutput(new OutputTag[String]("blackList")).print()

    env.execute("BlackListApp")
  }

  class MyBlackListProcess extends KeyedProcessFunction[(Long, Long), AdClickLog, AdClickLog]{
    //  保存当天的个数
    lazy val countState: ValueState[Long] = getRuntimeContext.getState(new ValueStateDescriptor[Long]("count-state", Types.of[Long]))
    // 是不是第一次发给黑名单,侧输出对于同一数据只只输出一次
    lazy val firstState: ValueState[Boolean] = getRuntimeContext.getState(new ValueStateDescriptor[Boolean]("first-state", Types.of[Boolean]))

    lazy val blackList = new OutputTag[String]("blackList")

    override def processElement(value: AdClickLog,
                                ctx: KeyedProcessFunction[(Long, Long), AdClickLog, AdClickLog]#Context,
                                out: Collector[AdClickLog]): Unit = {
      countState.update(countState.value() + 1)
      val count = countState.value()

      // 第一次处理时， 注册一个定时器， 每天00:00 触发清除
      if(count == 0){
        val ts = (ctx.timerService().currentProcessingTime() / (24 * 60 * 60 * 1000) + 1) * (24 * 60 * 60 * 1000)
        ctx.timerService().registerProcessingTimeTimer(ts)
      }

//      if(count >= 100){
      if(count >= 3){
        // 侧输出对于同一数据只只输出一次
        if(!firstState.value()){
          firstState.update(true)
          ctx.output(blackList,value+"加入黑名单！！！")
        }
      }else{
        out.collect(value)
      }

    }

    override def onTimer(timestamp: Long,
                         ctx: KeyedProcessFunction[(Long, Long), AdClickLog, AdClickLog]#OnTimerContext,
                         out: Collector[AdClickLog]): Unit = {
      countState.clear()
      firstState.clear()
    }
  }

  case class AdClickLog(userId: Long,
                        adId: Long,
                        province: String,
                        city: String,
                        timestamp: Long)

}
