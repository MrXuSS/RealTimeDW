package com.haiyi.kfk.util

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.util

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.kudu.{ColumnSchema, Schema, Type}
import org.apache.kudu.client.{Delete, Insert, KuduClient, KuduSession, KuduTable, OperationResponse, PartialRow, SessionConfiguration, Upsert}

import scala.collection.mutable

/**
 * @Author:XuChengLei
 * @Date:2020-07-14
 *
 */
class KuDuSink(kuduMaster:String,tableName:String) extends RichSinkFunction[util.HashMap[String, String]] {

  private var client: KuduClient = _
  private var table:KuduTable = _

  private var schema:Schema = _
  private var kuduSession:KuduSession = _
  private var out:ByteArrayOutputStream = _
  private var os:ObjectOutputStream = _


  override def open(parameters: Configuration): Unit ={
    out = new ByteArrayOutputStream()
    os = new ObjectOutputStream(out)
    client = new KuduClient.KuduClientBuilder(kuduMaster).defaultSocketReadTimeoutMs(6000).build()
    table = client.openTable(tableName)
    schema = table.getSchema
    kuduSession = client.newSession()
    kuduSession.setFlushMode(SessionConfiguration.FlushMode.AUTO_FLUSH_BACKGROUND)
  }

  override def close(): Unit = {
    kuduSession.close()
    client.close()
    os.close()
    out.close()
  }

  override def invoke(map: util.HashMap[String, String], context: SinkFunction.Context[_]): Unit = {
    val eventType: String = map.get("eventType")
    if(eventType == "INSERT" || eventType == "UPDATE"){
      upSertData(map)
    }else{
      val key:String = "GZDBH"
      val value: String = map.get(key)
      deleteData(key,value)
    }
  }

  def deleteData(key:String,value:String): Unit ={
    val delete: Delete = table.newDelete()
    delete.getRow.addString(key,value)
    val response: OperationResponse = kuduSession.apply(delete)
    if(response != null){
      println(response.getRowError.toString)
    }
    kuduSession.flush()
  }

  def upSertData(map:util.HashMap[String,String]): Unit ={
    val upsert: Upsert = table.newUpsert()
    val row: PartialRow = upsert.getRow
    addRow(row,map)
    val response: OperationResponse = kuduSession.apply(upsert)
    if(response != null){
      println(response.getRowError.toString)
    }
    kuduSession.flush()
  }

  def addRow(row: PartialRow,map:util.HashMap[String,String]): Unit ={
    for (i <- (0 to schema.getColumnCount-1)){
      val key: String = schema.getColumnByIndex(i).getName
      val value: String = map.get(key)
      row.addString(key,value)
    }
  }

}

