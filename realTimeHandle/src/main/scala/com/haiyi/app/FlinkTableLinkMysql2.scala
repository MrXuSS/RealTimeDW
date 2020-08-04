package com.haiyi.app

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util.Calendar
import java.util.concurrent.TimeUnit

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.functions.RichFilterFunction
import org.apache.flink.calcite.shaded.com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache, RemovalListener, RemovalNotification}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

import scala.collection.{immutable, mutable}
import scala.util.Random
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * @Author:XuChengLei
 * @Date:2020-07-29
 *  热存储维表，实时的去外部表中查询数据，瓶颈在于存储的读取速度，使用cache来进行优化。
 */
object FlinkTableLinkMysql2 {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    env.addSource(new SensorReadingSource).filter(new FilterJoin).print()

    env.addSource(new SensorReadingSource)
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
      override def extractTimestamp(element: SensorReading) = {
        element.timestamp
      }
    })



    env.execute("FlinkTableLinkMysql")
  }

  class FilterJoin extends RichFilterFunction[SensorReading]{

    var dim:LoadingCache[String,SensorReading]=_
    var driver:String = "com.mysql.jdbc.Driver"
    var url:String = "jdbc:mysql://192.168.2.201:3306/NWPMSKF"
    var user:String = "root"
    var password:String = "123456"
    var conn:Connection = _
    var statement:Statement = _

    override def open(parameters: Configuration): Unit = {
      dim = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5,TimeUnit.MINUTES)
        .removalListener(new RemovalListener[String,SensorReading] {
          override def onRemoval(removalNotification: RemovalNotification[String, SensorReading]): Unit = {
            removalNotification.getKey+"被移除,值为"+removalNotification.getValue
          }
        }).build(new CacheLoader[String,SensorReading]() {
        override def load(k: String) = {
          val sensorReading: SensorReading = readMysql(k: String)
          sensorReading
        }
      })
    }
    override def close(): Unit = {
      statement.close()
      conn.close()
    }

    override def filter(value: SensorReading): Boolean = {
      val reading: SensorReading = dim.get(value.id)
      if(reading.id == "0"){
        false
      }else{
        true
      }
    }

    def readMysql(in:String):SensorReading ={
      conn = DriverManager.getConnection(url, user, password)
      statement = conn.createStatement()
      val sql:String = "select * from SensorReading where id = '"+ in +"'"
      val resultSet: ResultSet = statement.executeQuery(sql)
      if(resultSet.next()) {
        val id: String = resultSet.getString("id")
        val timestamp: Long = resultSet.getLong("timestamp")
        val temperature: Double = resultSet.getDouble("temperature")
        SensorReading(id, timestamp, temperature)
      }else{
        SensorReading(0.toString,0,0)
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
