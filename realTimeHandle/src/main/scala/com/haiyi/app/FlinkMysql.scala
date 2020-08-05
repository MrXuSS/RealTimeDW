package com.haiyi.app

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.Calendar
import java.util.concurrent.TimeUnit

import com.haiyi.app.FlinkTableLinkMysql.SensorReading
import com.haiyi.app.FlinkTableLinkMysql2.SensorReading
import org.apache.flink.api.common.functions.RichFilterFunction
import org.apache.flink.calcite.shaded.com.google.common.cache._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

import scala.collection.{immutable, mutable}
import scala.util.Random

/**
 * @Author:XuChengLei
 * @Date:2020-07-29
 *  测试flink读取mysql可不可以实时读写
 */
object FlinkMysql {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    env.addSource(new Mysql).keyBy(_.)


    env.execute("FlinkTableLinkMysql")
  }

  class Mysql extends  RichParallelSourceFunction[String]{

    var map:mutable.HashMap[String,SensorReading] = _
    var driver:String = "com.mysql.jdbc.Driver"
    var url:String = "jdbc:mysql://192.168.2.201:3306/NWPMSKF"
    var user:String = "root"
    var password:String = "123456"
    var conn:Connection = _
    var statement:Statement = _

    override def open(parameters: Configuration): Unit = {
      map = new mutable.HashMap[String,SensorReading]()
      Class.forName(driver)
      conn = DriverManager.getConnection(url,user,password)
      statement = conn.createStatement()

    }

    override def close(): Unit = {
      statement.close()
      conn.close()
    }

    override def cancel(): Unit = ???

    override def run(ctx: SourceFunction.SourceContext[String]): Unit = {
      val sql:String = "select * from SensorReading"
      val resultSet: ResultSet = statement.executeQuery(sql)
      while (resultSet.next()){
        val id: String = resultSet.getString("id")
        val timestamp: Long = resultSet.getLong("timestamp")
        val temperature: Double = resultSet.getDouble("temperature")

        map.put(id,SensorReading(id,timestamp,temperature))
      }
      ctx.collect(map.toString())
    }

  }

  case class SensorReading(id:String,
                           timestamp:Long,
                           temperature:Double)

}
