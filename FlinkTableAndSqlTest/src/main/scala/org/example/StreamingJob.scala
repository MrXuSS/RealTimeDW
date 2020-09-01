package org.example

import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.scala._
import org.apache.flink.table.api.scala.StreamTableEnvironment

object StreamingJob {
  def main(args: Array[String]) {
    // set up the streaming execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val tableEnv: StreamTableEnvironment = StreamTableEnvironment.create(env)

    val inputStream: DataStream[SensorReading] = env.addSource(new SensorReadingSource)
//    tableEnv.fromDataStream(inputStream)
//      .filter("id == 'sensor_10'")
//      .select("id,timestamp,temperature")
//        .toAppendStream[(String, Long, Double)]
//        .print()

    tableEnv.createTemporaryView("sensor", inputStream)
    val sql: String = "select id,`timestamp`,temperature from sensor where id = 'sensor_10'"
    tableEnv.sqlQuery(sql)
        .toAppendStream[(String, Long, Double)]
        .print()


    // execute program
    env.execute("Flink Streaming Scala API Skeleton")
  }
}
