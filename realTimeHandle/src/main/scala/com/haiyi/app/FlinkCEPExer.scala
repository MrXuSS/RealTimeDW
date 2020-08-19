package com.haiyi.app

import java.text.SimpleDateFormat

import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

import scala.collection.Map

/**
 * @author Mr.Xu
 * @create 2020-08-18
 *
 */
object FlinkCEPExer {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val inputStream = env.fromElements("D0000001,2020040201,220",
        "D0000001,2020040202,220",
        "D0000001,2020040203,230",
        "D0000001,2020040204,220",
        "D0000001,2020040205,230",
        "D0000001,2020040206,230",
        "D0000001,2020040207,270",
        "D0000001,2020040208,270",
        "D0000001,2020040209,270",
        "D0000001,2020040210,270",
        "D0000001,2020040211,220",
        "D0000001,2020040212,",
        ",,",
        "D0000001,2020040214,290",
        "D0000001,2020040215,220",
        "D0000002,2020040201,220",
        "D0000002,2020040202,220")
        .filter(line => {
        val words = line.split(",")
        words.size == 3
      })
      .map(line => {
        val words = line.split(",")
        val ts = new SimpleDateFormat("yyyyMMddHH").parse(words(1)).getTime
        (words(0), ts, words(2).toInt)
      })
      .assignAscendingTimestamps(_._2)
      .keyBy(_._1)

    val strategy = AfterMatchSkipStrategy.skipToFirst("next")
    val cepPattern = Pattern.begin[(String, Long, Int)]("begin")
      .where(_._3 > 220)
      .next("next")
      .where(_._3 > 220).timesOrMore(2).consecutive()
      //.within(Time.hours(3))

    val patternStream: PatternStream[(String, Long, Int)] = CEP.pattern(inputStream, cepPattern)

//    val resultStream: DataStream[((String, Long, Int), List[(String, Long, Int)])] = patternStream.select(
  val resultStream = patternStream.select(
    (map: Map[String, Iterable[(String, Long, Int)]]) => {
      val begin = map.getOrElse("begin", null).iterator.next()
      val nextIter = map.getOrElse("next", null).iterator.toList
      (begin, nextIter)
    })

    resultStream.print()

    env.execute("FlinkCEPExer")
  }
}
