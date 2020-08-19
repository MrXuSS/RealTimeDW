package com.haiyi.app

import java.text.SimpleDateFormat

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, MapState, MapStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

/**
 * @author Mr.Xu
 * @create 2020-08-18
 *
 * 1. 异常等级
 *         （1）DeviceData > 280，判定为A级异常;
 *         （2）260 < DeviceData <= 280，判定为B级异常;
 *         （3）220 < DeviceData <= 260，判定为C级异常;
 *         （4）否则正常
 *         （5）过滤异常数据，丢失数据按照正常计算
 * 2. 异常持续时间
 *         （1）A级异常持续时间 >= 1小时，则输出异常信息
 *         （2）B级异常持续时间 >= 2小时，则输出异常信息
 *         （3）C级异常持续时间 >= 3小时，则输出异常信息
 *
 *
 *   已实现，但是总感觉不是很完善，其中牵扯到太多的State的读写，以后有时间在回来瞅瞅
 */
object ShiXiExer3 {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val inputStream = env.fromElements("D0000001,2020040201,220",
      "D0000001,2020040202,220",
      "D0000001,2020040203,230",
      "D0000001,2020040204,220",
      "D0000001,2020040205,230",
      "D0000001,2020040206,230",
      "D0000001,2020040207,270",
      "D0000001,2020040208,270",
      "D0000001,2020040209,270",
      "D0000001,2020040210,270",
      "D0000001,2020040211,220",
      "D0000001,2020040212,",
      ",,",
      "D0000001,2020040214,290",
      "D0000001,2020040215,220",
      "D0000002,2020040201,220",
      "D0000002,2020040202,220")
        .filter(line =>{
          val words = line.split(",")
          if(words.size == 3 && words(2).toLong != 220){
            true
          }else{
            false
          }
        })
        .map(line =>{
          val words = line.split(",")
          (words(0), words(1), words(2))
        })
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[(String, String, String)](Time.seconds(0)) {
        override def extractTimestamp(element: (String, String, String)): Long = {
          val simpleDateFormat = new SimpleDateFormat("yyyyMMddHH")
          val ts = simpleDateFormat.parse(element._2).getTime
          ts
        }
      })
        .keyBy(_._1)
        .process(new MyKeyProcess)
        .print()

    env.execute("ShiXiExer3")
  }

  class MyKeyProcess extends KeyedProcessFunction[String, (String, String, String),String]{
      //  保存分等级的非法数据
    lazy val mapState: MapState[String, ListBuffer[(String, String, String)]] = getRuntimeContext.getMapState(
      new MapStateDescriptor[String, ListBuffer[(String, String, String)]]("defErrorCode",
        Types.of[String],
        Types.of[ListBuffer[(String, String, String)]])
    )
    //  保存上一次的非法数据
    lazy val lastTemp: MapState[String, (String, String, String)] = getRuntimeContext.getMapState(
      new MapStateDescriptor[String, (String, String, String)]("lastTemp", Types.of[String], Types.of[(String, String, String)])
    )

    override def processElement(value: (String, String, String),
                                ctx: KeyedProcessFunction[String, (String, String, String), String]#Context,
                                out: Collector[String]): Unit = {

      var key:String = ""

      if( value._3.toLong > 280){
        key = "A"
      }else if(value._3.toLong > 260 && value._3.toLong <= 280){
        key = "B"
      }else if(value._3.toLong > 220 && value._3.toLong <= 260){
        key = "C"
      }

      updateState(key,value)

      val list = mapState.get(key)

      if(key == "A" && list.size >= 1 ){
        out.collect(key+"=="+list)
      }else if(key == "B" && list.size >= 2){
        out.collect(key+"=="+list)
      }else if(key == "C" && list.size >= 3){
        out.collect(key+"=="+list)
      }
    }

    def updateState(key:String, value:(String, String ,String)): Unit ={
      val last = lastTemp.get(key)

      if(last == null){  // 该key第一次到达
        lastTemp.put(key, value)
        mapState.put(key, ListBuffer(value))
      }else if(value._2.toLong - last._2.toLong == 1){  // 数据连续
        val newList = mapState.get(key)
        newList.append(value)
        mapState.put(key, newList)
        lastTemp.put(key, value)
      }else{ // 数据不连续，将之前的state清空，保留当前的state
        mapState.remove(key)
        mapState.put(key,ListBuffer(value))
        lastTemp.put(key,value)
      }
    }
  }
}
