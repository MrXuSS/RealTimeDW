package com.haiyi.kfh.util

import org.apache.hadoop.conf
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.{ Connection, ConnectionFactory, Delete, Table}
import org.apache.hadoop.hbase.util.Bytes


/**
 * @Author:XuChengLei
 * @Date:2020-07-08
 *
 */
object HBaseUtil {

  val configuration: conf.Configuration = HBaseConfiguration.create()
  configuration.set("hbase.zookeeper.quorum","node1")
  var conn:Connection = ConnectionFactory.createConnection(configuration)
  val table: Table = conn.getTable(TableName.valueOf("hs_jldxx"))

  def deleteByRowKey(tableName:String,rowKey:String): Unit ={
    val delete = new Delete(Bytes.toBytes(rowKey))
    table.delete(delete)
    println(rowKey+"已删除")
  }
}
