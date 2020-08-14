package com.haiyi.userbehavior.app

import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

import scala.collection.Map

/**
 * @author Mr.Xu
 * @create 2020-08-14
 *  使用FlinkCEP处理迟到元素
 */
object OrderTimeOutWitCEP {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(2)  // 每一个并行度上都有独立的watermark

    val stream = env.socketTextStream("192.168.2.201", 9999, '\n')
      .map(line => {
        val words = line.split(",")
        OrderAction(words(0), words(1), words(2).toLong * 1000)
      })
      .assignAscendingTimestamps(_.ts)
      .keyBy(_.id)

    val pattern = Pattern.begin[OrderAction]("begin")
      .where(_.behavior == "create")
      .next("next")
      .where(_.behavior == "pay")
      .within(Time.seconds(5))

    val patternStream: PatternStream[OrderAction] = CEP.pattern(stream, pattern)

    val resultStream = patternStream.select(new OutputTag[OrderTimeOut]("timeout")) {
      // 柯里化，只有一条匹配第一个， 两条匹配第二个
      (map: Map[String, Iterable[OrderAction]], ts: Long) => {
        val action = map.getOrElse("begin", null).iterator.next()
        OrderTimeOut(action.id, action.behavior, "TimeOut")
      }
    } {
      (map: Map[String, Iterable[OrderAction]]) => {
        val action = map.getOrElse("next", null).iterator.next()
        OrderTimeOut(action.id, action.behavior, "success")
      }
    }

    val resultOut = resultStream.getSideOutput(new OutputTag[OrderTimeOut]("timeout"))
    resultStream.print()
    resultOut.print()

    env.execute("OrderTimeOutWitCEP")
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
