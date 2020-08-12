package com.haiyi.app

import org.apache.flink.cep.pattern.conditions.SimpleCondition
import org.apache.flink.cep.scala.pattern.Pattern

/**
 * @Author:XuChengLei
 *@Date:2020-08-07
 *
 */
class Rule implements Serializable {

    private static final long serialVersionUID = 6018083278972127758

    def run(){
         return Pattern.<String>begin("begin")
        .where(new SimpleCondition<SensorReading>() {
            @Override
            boolean filter(SensorReading value) throws Exception {
                value.id().equals("sensor_1")
            }
        })
    }

    class LogEventCondition extends SimpleCondition<SensorReading> implements Serializable{

        private String script;

        static {

        }

        @Override
        boolean filter(SensorReading value) throws Exception {
            return false
        }
    }

}
