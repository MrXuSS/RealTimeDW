package com.haiyi.app

import org.apache.flink.contrib.streaming.state.RocksDBStateBackend
import org.apache.flink.runtime.state.StateBackend
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._
import org.apache.kudu.Common.ExternalConsistencyMode

/**
 * @Author:XuChengLei
 * @Date:2020-08-03
 *
 */
object RocksDBStateTest {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    // 开启检查点时间间隔
    env.enableCheckpointing(5000)

    env.setStateBackend(new RocksDBStateBackend("D:\\Program\\WorkSpace\\IDEA_WorkSpace\\checkpoint"))

    // 保证整个应用内端到端的数据一致性
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)

    // CheckPoint 超时时间
    env.getCheckpointConfig.setCheckpointTimeout(50000)

    // 最大同时执行的checkpoint数量
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)

    // 是否删除CheckPoint中存储的数据
    env.getCheckpointConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)

    new RocksDBStateBackend("D:\\Program\\WorkSpace\\IDEA_WorkSpace\\checkpoint")

    env.execute("RocksDBStateTest")

  }
}
