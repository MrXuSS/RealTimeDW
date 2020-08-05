package com.haiyi.app

import java.util.Calendar

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.{immutable, mutable}
import scala.util.Random

/**
 * @Author:XuChengLei
 * @Date:2020-08-04
 *
 */
object WindowProcessFunctionTest {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val stream: DataStream[SensorReading] = env.addSource(new SensorReadingSource)

//    stream
//      .keyBy(_.id)
//        .timeWindow(Time.seconds(2))
//        .aggregate(new MyAvg)
//        .print()

//
//  stream.keyBy(_.id)
//      .timeWindow(Time.seconds(2))
//      .process(new HighAndLowTempProcessFunction)
//      .print()


    stream.keyBy(_.id)
        .timeWindow(Time.seconds(2))
        .aggregate(new AggMaxMin,new HighLowTemp)
        .print()

    env.execute("KeyedProcessFunctionTest")
  }


  class AggMaxMin extends AggregateFunction[SensorReading,(String,Double,Double),(String,Double,Double)]{
    override def createAccumulator(): (String, Double, Double) = ("",0,0)

    override def add(value: SensorReading, accumulator: (String, Double, Double)): (String, Double, Double) = {
      (value.id, value.temperature.max(accumulator._2),value.temperature.min(accumulator._3))
    }

    override def getResult(accumulator: (String, Double, Double)): (String, Double, Double) = {
      accumulator
    }

    override def merge(a: (String, Double, Double), b: (String, Double, Double)): (String, Double, Double) = {
      (a._1, a._2.max(b._2), a._3.min(b._3))
    }
  }

  class HighLowTemp extends ProcessWindowFunction[(String,Double,Double),String,String,TimeWindow]{
    override def process(key: String,
                         context: Context,
                         elements: Iterable[(String, Double, Double)],
                         out: Collector[String]): Unit = {
      val temp: (String, Double, Double) = elements.iterator.next()
      val end: Long = context.window.getEnd
      out.collect((key,temp._2,temp._3, end).toString())
    }
  }


  class HighAndLowTempProcessFunction extends ProcessWindowFunction[SensorReading,String,String,TimeWindow]{
    private val stringToLong = new mutable.HashMap[String, Long]()
    override def process(key: String,
                         context: Context,
                         elements: Iterable[SensorReading],
                         out: Collector[String]): Unit = {
      val result: Iterable[Double] = elements.map(_.temperature)
      val end: Long = context.window.getEnd
      out.collect((key,result.max,result.min,end).toString())
    }
  }

  class MyAvg extends AggregateFunction[SensorReading,(String,Double,Int),String]{
    override def createAccumulator(): (String, Double, Int) = ("",0,0)

    override def add(value: SensorReading, accumulator: (String, Double, Int)): (String, Double, Int) = {
      (value.id, value.temperature+accumulator._2, accumulator._3+1)
    }

    override def getResult(accumulator: (String, Double, Int)): String = {
      accumulator._1+":"+(accumulator._2/accumulator._3)
    }

    override def merge(a: (String, Double, Int), b: (String, Double, Int)): (String, Double, Int) ={
      (a._1,a._2+b._2,a._3+b._3)
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

}
