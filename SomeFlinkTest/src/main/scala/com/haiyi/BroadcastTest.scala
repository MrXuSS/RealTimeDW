package com.haiyi

import org.apache.flink.api.common.state.{MapStateDescriptor, ReadOnlyBroadcastState}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
 * @author Mr.Xu
 * @create 2020-09-04
 *
 */
object BroadcastTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val input1: DataStream[(String, String)] = env.socketTextStream("192.168.2.201", 8888, '\n')
      .map(line => {
        val words: Array[String] = line.split(" ")
        (words(0), words(1))
      })
    val input2: DataStream[(String, String)] = env.socketTextStream("192.168.2.201", 9999, '\n')
      .map(line => {
        val words: Array[String] = line.split(" ")
        (words(0), words(1))
      })

    val input2Broadcast: BroadcastStream[(String, String)] = input2.broadcast(
      new MapStateDescriptor[String, String]("broadcastState", Types.of[String], Types.of[String]))

    input1
        .keyBy(_._1)
        .connect(input2Broadcast)
        .process(new MyKeyByProcess)
        .print()


    env.execute("BroadcastTest")
  }

  class MyKeyByProcess extends KeyedBroadcastProcessFunction[String,(String,String),(String,String),String]{
    override def processElement(value: (String, String),
                                ctx: KeyedBroadcastProcessFunction[String, (String, String), (String, String), String]#ReadOnlyContext,
                                out: Collector[String]): Unit = {

      val broadState: ReadOnlyBroadcastState[String, String] = ctx.getBroadcastState(new MapStateDescriptor[String, String]("broadcastState", Types.of[String], Types.of[String]))
      val str: String = broadState.get(value._1)
      out.collect(value + "===" + str)

    }

    override def processBroadcastElement(value: (String, String),
                                         ctx: KeyedBroadcastProcessFunction[String, (String, String), (String, String), String]#Context,
                                         out: Collector[String]): Unit = {

      ctx.getBroadcastState(new MapStateDescriptor[String, String]("broadcastState",Types.of[String],Types.of[String]))
        .put(value._1, value._2)
    }
  }

}
