package com.haiyi.app

import javax.script.{Invocable, ScriptEngineManager}
import org.apache.flink.cep.scala.pattern.Pattern

/**
 * @author Mr.Xu
 * @create 2020-08-10
 *
 */
object ScriptEngine{

  def getPattern(text:String,name:String)={
    val factory = new ScriptEngineManager()
    val engine = factory.getEngineByName("groovy")
    println(engine.toString)
    assert(engine != null)
    engine.eval(text)
    val pattern = engine.asInstanceOf[Invocable].invokeFunction(name)
    pattern.asInstanceOf[Pattern[SensorReading,SensorReading]]
  }
}
