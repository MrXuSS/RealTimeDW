package com.haiyi.app

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util
import java.util.Properties

import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat
import org.apache.flink.api.java.typeutils.RowTypeInfo
import org.apache.flink.api.scala.DataSet
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import org.apache.flink.types.Row

/**
 * @Author:XuChengLei
 * @Date:2020-07-09
 * 根据地区统计地区用电量等，这部分需要实时加上历史数据。
 */
object ElectricityConsumptionKeyByAreaApp{
  def main(args: Array[String]): Unit = {

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "node1:9092")
    properties.setProperty("group.id", "consumer-group122")
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    properties.setProperty("auto.offset.reset", "earliest")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)
    env.enableCheckpointing(5000)

    // 1.获取kafka中的json字符串
    val stream: DataStream[String] = env.addSource(new FlinkKafkaConsumer011[String]("topic_canal_mysql", new SimpleStringSchema(), properties))
    // 2.清洗数据：因为之前往kafka中写数据测试时，有脏数据的写入；保留可以转换为Hs_jldxx的数据
    val filterStream: DataStream[String] = stream.filter {
      jsonStr =>
        val jSONObject: JSONObject = JSON.parseObject(jsonStr)
        val keys: util.Set[String] = jSONObject.keySet()
        if (keys.size() < 135 ) {
          false
        }else{
          true
        }
    }

    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://192.168.2.201:3306/NWPMSKF"
    val username = "root"
    val password = "123456"
    var connection:Connection = null

    // 3.实时数据与历史数据对接
    filterStream.map {
      jsonStr => {
        println(jsonStr)
        val jSONObject: JSONObject = JSON.parseObject(jsonStr)
        val gzdbh = jSONObject.get("GZDBH").toString.toInt
        val jldbh = jSONObject.get("JLDBH").toString.toInt
        var sql:String = "select * from hs_jldxx where GZDBH = "+ gzdbh
        Class.forName(driver)
        if(connection == null) {
          connection = DriverManager.getConnection(url,username,password)
        }
        val statement: Statement = connection.createStatement()

        val resultSet: ResultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
          val gzdbh2: String = resultSet.getString("GZDBH")
          val jldbh2: String = resultSet.getString("JLDBH")
          println((gzdbh, jldbh2.toInt + jldbh.toInt))
        }
      }

    }

//    // 3.对数据进行进行存储
//    val resultStream: DataStream[String] = filterStream.map {
//      jsonStr =>
//        val jSONObject: JSONObject = JSON.parseObject(jsonStr)
//        val areaKey = jSONObject.getString("DQBM")
//        var countKey = "YDLBDM"
//        if (electricityByAreaMap.containsKey(areaKey)) {
//          val count: Double = electricityByAreaMap.get(areaKey)
//          electricityByAreaMap.put(areaKey, count + jSONObject.getString(countKey).toDouble)
//        } else {
//          electricityByAreaMap.put(areaKey, jSONObject.getString(countKey).toDouble)
//        }
//        val nObject = new JSONObject()
//        nObject.put("result", electricityByAreaMap)
//        nObject.toString
//    }

 //   resultStream.print()

    env.execute("ElectricityConsumptionGroupByAreaApp")
  }
}
