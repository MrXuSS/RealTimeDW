package com.haiyi.kfh.util

import java.util

import com.haiyi.kfh.entity.Hs_jldxx
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.hadoop.hbase.{HBaseConfiguration, HConstants, TableName}
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory, Put, Table}
import org.apache.hadoop.hbase.util.Bytes

/**
 * @Author:XuChengLei
 * @Date:2020-07-06
 *
 */
class HBaseSink(tableName:String,family:String) extends  RichSinkFunction[Hs_jldxx]{


  var conn:Connection= _
  var table: Table = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    val conf = HBaseConfiguration.create()
    conf.set(HConstants.ZOOKEEPER_QUORUM,"node1")
    conf.set(HConstants.ZOOKEEPER_CLIENT_PORT,"2181")
    conn = ConnectionFactory.createConnection(conf)
    table = conn.getTable(TableName.valueOf(tableName))
  }

  override def invoke(hs_jldxx: Hs_jldxx, context: SinkFunction.Context[_]): Unit = {
    val currentTime: Long = System.currentTimeMillis()
    val put = new Put(Bytes.toBytes(hs_jldxx.GZDBH))
    addHBaseColumn(put,hs_jldxx)
    table.put(put)
  }

  override def close(): Unit = {
    table.close()
    conn.close()
  }

  def addHBaseColumn(put: Put,hs_jldxx: Hs_jldxx): Unit ={
    val columns: Array[String] = Array("GZDBH",
                                        "JLDBH",
                                        "YWLBDM",
                                        "JLDXH",
                                        "CBJHBH",
                                        "CBQDBH",
                                        "CBZQ",
                                        "JLDCBSXH",
                                        "DFNY",
                                        "BQCBCS",
                                        "YCBCS",
                                        "JSLXDM",
                                        "CZCS",
                                        "CZNY",
                                        "JSHH",
                                        "YHBH",
                                        "YHMC",
                                        "YDDZ",
                                        "YHZTDM",
                                        "DJBBBH",
                                        "JLDYTDM",
                                        "JLFSDM",
                                        "JLDYDJDM",
                                        "XLXDBS",
                                        "TQBS",
                                        "WYJJSFSDM",
                                        "YHWYJQSR",
                                        "DJCLBH",
                                        "YGDDL",
                                        "DYBH",
                                        "DYZH",
                                        "DLJSFSDM",
                                        "DLDBZ",
                                        "DBKJBZ",
                                        "DLFTGS",
                                        "BSFTFSDM",
                                        "YGBSXYZ",
                                        "WGBSXYZ",
                                        "BSJFBZ",
                                        "XSJSFSDM",
                                        "YGXSJSZ",
                                        "WGXSJSZ",
                                        "XSJFBZ",
                                        "XSFTBZ",
                                        "XSFTXYZ",
                                        "YDRL",
                                        "PTBBDM",
                                        "CTBBDM",
                                        "YDLBDM",
                                        "DJDM",
                                        "DJ",
                                        "GLYSBZDM",
                                        "HYFLDM",
                                        "FSJFBZ",
                                        "JBDFJSFSDM",
                                        "XLHDZ",
                                        "GDLL",
                                        "JBDFFTFS",
                                        "JBDFFTZ",
                                        "LTDFJSFS",
                                        "JTDLBL",
                                        "ZJSL",
                                        "FGZS",
                                        "SJLX",
                                        "YGZDBH",
                                        "GLYSKHFSDM",
                                        "JFYXJ",
                                        "GDDWBM",
                                        "JSBZ",
                                        "HSZTDM",
                                        "SCCBRQ",
                                        "CBRQ",
                                        "YDTS",
                                        "CYTZDF",
                                        "YGFBKJDL",
                                        "YGBSDL",
                                        "YGXSDL",
                                        "YGFTDL",
                                        "YGTBDL",
                                        "YGZDL",
                                        "WGFBKJDL",
                                        "WGBSDL",
                                        "WGXSDL",
                                        "WGFTDL",
                                        "WGTBDL",
                                        "KFRQ",
                                        "JSRQ",
                                        "FXRQ",
                                        "JTLX",
                                        "YGCJDL",
                                        "YGHBDL",
                                        "WGCJDL",
                                        "WGHBDL",
                                        "NLJDL",
                                        "MFDL",
                                        "TBDF",
                                        "YSDF",
                                        "WGZDL",
                                        "ZJRL",
                                        "JFRL",
                                        "JFXL",
                                        "JBFDJ",
                                        "SJLL",
                                        "TZXS",
                                        "ZBJLDBH",
                                        "KJXBDL",
                                        "JFDL",
                                        "DDDF",
                                        "JBDF",
                                        "LTDF",
                                        "FJFHJ",
                                        "ZDF",
                                        "PJLXDM",
                                        "YHLBDM",
                                        "CXDM",
                                        "YXBZ",
                                        "CZRBS",
                                        "CZSJ",
                                        "DWBM",
                                        "DQBM",
                                        "CJSJ",
                                        "SCDL",
                                        "SCGZDBH",
                                        "CDZXLDF",
                                        "GTSYDL",
                                        "CJSBZ",
                                        "GLBH",
                                        "JTNF",
                                        "BZ",
                                        "TCDM",
                                        "TCLJDF",
                                        "TCKSSJ",
                                        "TCJSSJ",
                                        "TBGZDBH",
                                        "JLDZTDM",
                                        "HCRQ")
    for (elem <- columns) {
      put.addColumn(Bytes.toBytes(family),Bytes.toBytes(elem),Bytes.toBytes(hs_jldxx.get(elem)))
    }
  }

}
