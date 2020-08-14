package com.haiyi.userbehavior.app

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector
/**
 * @author Mr.Xu
 * @create 2020-08-14
 *  实现双流join
 */
object TwoStreamJoin {

  // 两条流join不到的数据输出到侧输出流
  lazy val unmatchPays = new OutputTag[Pay]("unmatch-pays")
  lazy val unmatchReceipts = new OutputTag[Receipt]("unmatch-receipts")

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val payStream = env.fromElements(Pay("1", 1000L), Pay("2", 2000L))
      .assignAscendingTimestamps(_.ts)
      .keyBy(_.txId)

    val receiptStream = env.fromElements(Receipt("2", 3000L))
      .assignAscendingTimestamps(_.ts)
      .keyBy(_.txId)

    val stream = payStream.connect(receiptStream)
      .process(new MyCoProcess)

    stream.print()
    stream.getSideOutput(unmatchPays).print()
    stream.getSideOutput(unmatchReceipts).print()

    env.execute("TwoStreamJoin")
  }

  class MyCoProcess extends CoProcessFunction[Pay, Receipt, String]{

    lazy val payState: ValueState[Pay] = getRuntimeContext.getState(
      new ValueStateDescriptor[Pay]("payState", Types.of[Pay])
    )

    lazy val receiptState: ValueState[Receipt] = getRuntimeContext.getState(
      new ValueStateDescriptor[Receipt]("receiptState", Types.of[Receipt])
    )

    override def processElement1(value: Pay,
                                 ctx: CoProcessFunction[Pay, Receipt, String]#Context,
                                 out: Collector[String]): Unit = {
      val receipt = receiptState.value()
      // 如果receipt不为空，就关联； 为空就state存储然后定义定时器立即执行
      if(receipt != null){
        receiptState.clear()
        out.collect((value, receipt).toString())
      }else{
        payState.update(value)
        ctx.timerService().registerEventTimeTimer(value.ts)
      }
    }

    override def processElement2(value: Receipt,
                                 ctx: CoProcessFunction[Pay, Receipt, String]#Context,
                                 out: Collector[String]): Unit = {
      val pay = payState.value()
      if(pay != null){
        payState.clear()
        out.collect((pay,value).toString())
      }else{
        receiptState.update(value)
        ctx.timerService().registerEventTimeTimer(value.ts)
      }
    }

    override def onTimer(timestamp: Long,
                         ctx: CoProcessFunction[Pay, Receipt, String]#OnTimerContext,
                         out: Collector[String]): Unit = {
      // payState不为空，说明recepit没有与之匹配的数据
      if(payState.value() != null){
        ctx.output(unmatchPays, payState.value())
      }

      if(receiptState.value() != null){
        ctx.output(unmatchReceipts, receiptState.value())
      }

      payState.clear()
      receiptState.clear()
    }
  }

  case class Pay(txId:String, ts:Long)

  case class Receipt(txId:String, ts:Long)
}
