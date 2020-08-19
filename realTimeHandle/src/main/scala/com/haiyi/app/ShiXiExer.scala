package com.haiyi.app

import java.text.SimpleDateFormat

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
 * @author Mr.Xu
 * @create 2020-08-18
 *
 */
object ShiXiExer {
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

    inputStream
        .process(new MyKeyProcess)
        .print()

    env.execute("ShiXiExer")
  }

  class MyKeyProcess extends KeyedProcessFunction[String, (String, Long, Int),String]{

    lazy val errorCodeState: ListState[(String, Long, Int)] = getRuntimeContext.getListState(
      new ListStateDescriptor[(String, Long, Int)]("errorCode", Types.of[(String, Long, Int)])
    )

    override def processElement(value: (String, Long, Int),
                                ctx: KeyedProcessFunction[String, (String, Long, Int), String]#Context,
                                out: Collector[String]): Unit = {
      if(value._3 != 220){
        errorCodeState.add(value)
      }else{
        errorCodeState.clear()
      }

      import scala.collection.JavaConversions._

      if (errorCodeState.get().iterator().size >= 3) {
        out.collect(errorCodeState.get().iterator().toList.toString())
      }
    }
  }
}
