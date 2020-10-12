package com.haiyi.canal;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.protocol.CanalEntry;

import java.util.List;

/**
 * @Author:XuChengLei
 * @Date:2020-07-03
 */
public class CanalHandler {

    private CanalEntry.EventType eventType;

    private String tableName;

    private List<CanalEntry.RowData> rowDataList;


    public CanalHandler(CanalEntry.EventType eventType, String tableName, List<CanalEntry.RowData> rowDataList) {
        this.eventType = eventType;
        this.tableName = tableName;
        this.rowDataList = rowDataList;
    }

    public void handle(){
        if("hs_jldxx".equals(tableName)){
            rowDateListToKafka("testEs");
        }
    }

    public void rowDateListToKafka(String kafkaTopic){
        JSONObject jsonObject = new JSONObject();
        for (CanalEntry.RowData rowData : rowDataList) {
            if(CanalEntry.EventType.DELETE == eventType ){
                List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
                for (CanalEntry.Column column : beforeColumnsList) {
                    //System.out.println(column.getName()+":::"+column.getValue());
                    jsonObject.put(column.getName(),column.getValue());
                }
                jsonObject.put("eventType",eventType.toString());
                jsonObject.put("tableName",tableName);
                MyKafkaSender.send(kafkaTopic,tableName+jsonObject.getString("GZDBH").toString(),jsonObject.toJSONString());
            }else {
                List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
                for (CanalEntry.Column column : afterColumnsList) {
                    //System.out.println(column.getName()+":::"+column.getValue());
                    jsonObject.put(column.getName(),column.getValue());
                }
                jsonObject.put("eventType",eventType.toString());
                jsonObject.put("tableName",tableName);
                MyKafkaSender.send(kafkaTopic,tableName+jsonObject.getString("GZDBH").toString(),jsonObject.toJSONString());
            }
        }
    }
}
