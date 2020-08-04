package com.haiyi.app

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.Calendar

import org.apache.flink.api.common.functions.RichFilterFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}

import scala.collection.{immutable, mutable}
import scala.util.Random
import org.apache.flink.streaming.api.scala._
/**
 * @Author:XuChengLei
 * @Date:2020-07-28
 *  预加载维表，将维表数据全部加载到内存中，适用于维表比较小的情况。
 */
object FlinkTableLinkMysql {

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    env.addSource(new SensorReadingSource).filter(new FilterJoin).print()

    env.execute("FlinkTableLinkMysql")
  }

  class FilterJoin extends RichFilterFunction[SensorReading]{

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
      val sql:String = "select * from SensorReading"
      val resultSet: ResultSet = statement.executeQuery(sql)
      while (resultSet.next()){
        val id: String = resultSet.getString("id")
        val timestamp: Long = resultSet.getLong("timestamp")
        val temperature: Double = resultSet.getDouble("temperature")

        map.put(id,SensorReading(id,timestamp,temperature))
      }
    }

    override def close(): Unit = {
      statement.close()
      conn.close()
    }

    override def filter(value: SensorReading): Boolean = {
      if(!map.contains(value.id)){
        false
      }else{
        true
      }
    }

  }

  case class SensorReading(id:String,
                           timestamp:Long,
                           temperature:Double)

  class SensorReadingSource extends RichParallelSourceFunction[SensorReading]{
    var running:Boolean = true

    override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit =  {
      val random = new Random()

      val taskIdx: Int = this.getRuntimeContext.getIndexOfThisSubtask

      val curFtemp: immutable.IndexedSeq[(String, Double)] = (1 to 10).map {
        i => ("sensor_" + (taskIdx * 10 + i), 65 + (random.nextGaussian() * 20))
      }

      while (running){
        curFtemp.map(t=>(t._1,t._2+(random.nextGaussian()),t._2))
        val ts: Long = Calendar.getInstance.getTimeInMillis
        curFtemp.foreach(t => sourceContext.collect(SensorReading(t._1,ts,t._2)))
        Thread.sleep(100)
      }
    }

    override def cancel(): Unit = {
      running = false
    }
  }

}
