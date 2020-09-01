package com.haiyi.app

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
 * @author Mr.Xu
 * @create 2020-09-01
 *
 */
object RockDBStateClearTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(3)

    env.setStateBackend(new RocksDBStateBackend("file:\\D:\\Program\\WorkSpace\\IDEA_WorkSpace\\RealTimeDW\\RockDBState"))

    env.addSource(new SensorReadingSource)
        .keyBy(_.id)
        .process(new MyKeyedProcess)

    env.execute("RockDBStateClearTest")
  }

  class MyKeyedProcess extends KeyedProcessFunction[String, SensorReading, String]{

    lazy val listState: ListState[SensorReading] = getRuntimeContext.getListState(
      new ListStateDescriptor[SensorReading]("TestList", Types.of[SensorReading])
    )

    override def processElement(value: SensorReading,
                                ctx: KeyedProcessFunction[String, SensorReading, String]#Context,
                                out: Collector[String]): Unit = {
      listState.add(value)
    }

    override def close(): Unit = {
      listState.clear()
    }
  }

}
