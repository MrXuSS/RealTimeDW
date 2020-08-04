//package com.haiyi.app
//
//import java.sql.{Connection, DriverManager, ResultSet, Statement}
//import java.util
//import java.util.Calendar
//import java.util.concurrent.TimeUnit
//
//import org.apache.flink.configuration.Configuration
//import org.apache.flink.streaming.api.TimeCharacteristic
//import org.apache.flink.streaming.api.functions.async.{ResultFuture, RichAsyncFunction}
//import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
//import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
//
//import scala.collection.immutable
//import scala.util.Random
//
///**
// * @Author:XuChengLei
// * @Date:2020-07-29
// *  热存储维表，实时的去外部表中查询数据，使用异步IO的方式
// */
//object FlinkTableLinkMysql3 {
//  def main(args: Array[String]): Unit = {
//    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
//    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
//    env.setParallelism(1)
//
//    val stream: DataStream[SensorReading] = env.addSource(new SensorReadingSource)
//
//    AsyncDataStream
//        .orderedWait(stream,new MyAsync,TimeUnit.MICROSECONDS,2)
//
//    env.execute("FlinkTableLinkMysql")
//  }
//
//  class MyAsync extends RichAsyncFunction[SensorReading,SensorReading]{
//
//    var driver:String = "com.mysql.jdbc.Driver"
//    var url:String = "jdbc:mysql://192.168.2.201:3306/NWPMSKF"
//    var user:String = "root"
//    var password:String = "123456"
//    var conn:Connection = _
//    var statement:Statement = _
//
//    override def open(parameters: Configuration): Unit = {
//      Class.forName(driver)
//      conn = DriverManager.getConnection(url,user,password)
//      statement = conn.createStatement()
//    }
//
//    override def close(): Unit = {
//      statement.close()
//      conn.close()
//    }
//
//    override def asyncInvoke(input: SensorReading, resultFuture: ResultFuture[SensorReading]): Unit = {
//      val sensorReading: SensorReading = readMysql(input.id)
//      val readings = new util.ArrayList[SensorReading]()
//      readings.add(sensorReading)
//      resultFuture.complete(readings)
//    }
//
//    override def timeout(input: SensorReading, resultFuture: ResultFuture[SensorReading]): Unit = {
//      val readings = new util.ArrayList[SensorReading]()
//      readings.add(input)
//      resultFuture.complete(readings)
//    }
//
//    def readMysql(in:String):SensorReading ={
//      conn = DriverManager.getConnection(url, user, password)
//      statement = conn.createStatement()
//      val sql:String = "select * from SensorReading where id = '"+ in +"'"
//      val resultSet: ResultSet = statement.executeQuery(sql)
//      if(resultSet.next()) {
//        val id: String = resultSet.getString("id")
//        val timestamp: Long = resultSet.getLong("timestamp")
//        val temperature: Double = resultSet.getDouble("temperature")
//        SensorReading(id, timestamp, temperature)
//      }else{
//        SensorReading(0.toString,0,0)
//      }
//
//    }
//  }
//
//  case class SensorReading(id:String,
//                           timestamp:Long,
//                           temperature:Double)
//
//  class SensorReadingSource extends RichParallelSourceFunction[SensorReading]{
//    var running:Boolean = true
//
//    override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit =  {
//      val random = new Random()
//
//      val taskIdx: Int = this.getRuntimeContext.getIndexOfThisSubtask
//
//      val curFtemp: immutable.IndexedSeq[(String, Double)] = (1 to 10).map {
//        i => ("sensor_" + (taskIdx * 10 + i), 65 + (random.nextGaussian() * 20))
//      }
//
//      while (running){
//        curFtemp.map(t=>(t._1,t._2+(random.nextGaussian()),t._2))
//        val ts: Long = Calendar.getInstance.getTimeInMillis
//        curFtemp.foreach(t => sourceContext.collect(SensorReading(t._1,ts,t._2)))
//        Thread.sleep(100)
//      }
//    }
//
//    override def cancel(): Unit = {
//      running = false
//    }
//  }
//
//}
