package com.haiyi.app

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.Calendar

import org.apache.flink.api.common.functions.RichFilterFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._

import scala.collection.{immutable, mutable}
import scala.util.Random

/**
 * @Author:XuChengLei
 * @Date:2020-07-28
 *  预加载维表，将维表数据全部加载到内存中，适用于维表比较小的情况。
 */
object FlinkKeyByTest {

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
//    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    val logEventStream: DataStream[LogEvent] = env.fromCollection(List(
      LogEvent("1", 1, "fail", "17863523153"),
      LogEvent("1", 1, "success", "17863523154"),
      LogEvent("1", 1, "fail", "17863523155"),
      LogEvent("1", 1, "fail", "17863523156"),
      LogEvent("1", 1, "fail", "17863523160")
    )).assignAscendingTimestamps(_.eventTime.toLong * 1000)

    logEventStream.print()

    //logEventStream.keyBy(_.userId).sum("num").print()

    env.execute("FlinkKeyByTest")
  }
  case class LogEvent(userId:String,num:Int,eventStatus:String,eventTime:String)
}
