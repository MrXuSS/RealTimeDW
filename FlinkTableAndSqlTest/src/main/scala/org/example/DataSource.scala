package org.example

import java.util.Calendar

import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}

import scala.collection.immutable
import scala.util.Random

/**
 * @author Mr.Xu
 * @create 2020-08-28
 *  作为练习时用的数据源
 */

case class SensorReading(id:String,
                         timestamp:Long,
                         temperature:Double)

class SensorReadingSource extends RichParallelSourceFunction[SensorReading]{
  var running:Boolean = true

  override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit =  {
    val random = new Random()

    val taskIdx: Int = this.getRuntimeContext.getIndexOfThisSubtask

    var curFtemp: immutable.IndexedSeq[(String, Double)] = (1 to 10).map {
      i => ("sensor_" + (taskIdx * 10 + i), 65 + (random.nextGaussian() * 20))
    }

    while (running){
      curFtemp = curFtemp.map(t=>(t._1,t._2+(random.nextGaussian()*0.5)))
      val ts: Long = Calendar.getInstance.getTimeInMillis
      curFtemp.foreach(t => sourceContext.collect(SensorReading(t._1,ts,t._2)))
      Thread.sleep(100)
    }
  }

  override def cancel(): Unit = {
    running = false
  }
}
