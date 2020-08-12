package com.haiyi.app

import java.io.{File, Serializable}
import java.util

import com.googlecode.aviator.AviatorEvaluator
import com.googlecode.aviator.runtime.`type`.{AviatorDouble, AviatorObject, AviatorString}
import com.googlecode.aviator.runtime.function.{AbstractFunction, FunctionUtils}
import org.apache.flink.cep.pattern.conditions.SimpleCondition
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.scala._

import scala.collection.Map

/**
 * @Author:XuChengLei
 * @Date:2020-08-07
 *  flinkCEP 动态规则基础实现    https://www.jianshu.com/p/04fd02c4db3c
 */
object FlinkCEP_Groovy_Test2 {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)

    val stream = env.addSource(new SensorReadingSource).keyBy(_.id)

    val pattern: Pattern[SensorReading, SensorReading] = ScriptEngine.getPattern(
      """
        |import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy
        |import org.apache.flink.cep.scala.pattern.Pattern
        |import com.haiyi.app.FlinkCEP_Groovy_Test2.AviatorCondition
        |import com.haiyi.app.SensorReading
        |
        |where1 = new AviatorCondition("getD(\"temperature\") > 10")
        |where2 = new AviatorCondition("getD(\"temperature\") > 10")
        |
        |def get(){
        | return Pattern.<SensorReading>begin("start",AfterMatchSkipStrategy.noSkip())
        |               .where(where1)
        |               .next("next")
        |               .where(where2)
        |               }
        |""".stripMargin, "get")

    val value: PatternStream[SensorReading] = CEP.pattern(stream, pattern)

    val resultStream: DataStream[SensorReading] = value.select(
      (map: Map[String, Iterable[SensorReading]]) => {
        val sensorReading: SensorReading = map.getOrElse("start", null).iterator.next()
        sensorReading
      }
    )

    resultStream.print()

    env.execute("FlinkCEP_Groovy_Test2")
  }

  class AviatorCondition(script:String) extends SimpleCondition[SensorReading] with Serializable{

    AviatorEvaluator.addFunction(new GetStringFieldFunction)
    AviatorEvaluator.addFunction(new GetDoubleFieldFunction)

    override def filter(value: SensorReading): Boolean = {
      val map = new util.HashMap[String, AnyRef]()
      map.put("id",value.id)
      map.put("timestamp",value.timestamp.toString)
      map.put("temperature",value.temperature.toString)

      val result = AviatorEvaluator.execute(script, map).asInstanceOf[Boolean]
      result
    }
  }
    // 处理字符串类型
  class GetDoubleFieldFunction extends AbstractFunction{
    override def getName: String = "getS"

    // env 就是在AviatorCondition中创建的map，通过 AviatorEvaluator.execute(script, map)传进来的
    override def call(env: util.Map[String, AnyRef],
                      arg1: AviatorObject): AviatorObject = {
      val field = FunctionUtils.getStringValue(arg1, env)
      new AviatorString(env.get(field).toString)
    }
  }

  // 处理数值类型
  class GetStringFieldFunction extends AbstractFunction{
    override def getName: String = "getD"

    // env 就是在AviatorCondition中创建的map，通过 AviatorEvaluator.execute(script, map)传进来的
    override def call(env: util.Map[String, AnyRef],
                      arg1: AviatorObject): AviatorObject = {
      val field = FunctionUtils.getStringValue(arg1, env)
      new AviatorDouble(env.get(field).toString.toDouble)
    }
  }
}

