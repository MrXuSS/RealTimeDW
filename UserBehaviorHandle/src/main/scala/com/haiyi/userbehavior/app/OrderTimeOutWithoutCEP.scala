package com.haiyi.userbehavior.app

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector


/**
 * @author Mr.Xu
 * @create 2020-08-14
 *  不使用FlinkCEP实现迟到数据的检测
 */
object OrderTimeOutWithoutCEP {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(2)  // 每一个并行度上都有独立的watermark

    env.fromElements("1,create,1","2,create,2","2,pay,5","3,create,100","3,pay,101")
        .map(line=>{
          val words = line.split(",")
          OrderAction(words(0), words(1), words(2).toLong * 1000)
        })
        .assignAscendingTimestamps(_.ts)
        .keyBy(_.id)
        .process(new MyKeyProcess)
        .print()

    env.execute("OrderTimeOutWithoutCEP")
  }

  class MyKeyProcess extends KeyedProcessFunction[String, OrderAction, String]{
    // 用来标记是否支付
    lazy val isPayedState: ValueState[Boolean] = getRuntimeContext.getState(new ValueStateDescriptor[Boolean]("isPayed", Types.of[Boolean]))

    override def processElement(value: OrderAction,
                                ctx: KeyedProcessFunction[String, OrderAction, String]#Context,
                                out: Collector[String]): Unit = {
      if(value.behavior == "create" && !isPayedState.value()){
        ctx.timerService().registerEventTimeTimer(value.ts + 5000L)
      }else{
        isPayedState.update(true)
      }
    }

    override def onTimer(timestamp: Long,
                         ctx: KeyedProcessFunction[String, OrderAction, String]#OnTimerContext,
                         out: Collector[String]): Unit = {
      if(isPayedState.value()){  // 说明5s内支付了
        isPayedState.clear()
      }else{
        out.collect(ctx.getCurrentKey + "timeout")
        isPayedState.clear()
      }
    }
  }

  case class OrderAction(
                        id:String,
                        behavior:String,
                        ts:Long
                        )

  case class OrderTimeOut(
                         id:String,
                         behavior:String,
                         info:String
                         )

}
