package com.haiyi.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @Author:XuChengLei
 * @Date:2020-07-03
 */
public class CanalApp {
    public static void main(String[] args) {
        int mm = 0;
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("192.168.2.201", 11111), "example", "", "");
        while (true){
            canalConnector.connect();
            canalConnector.subscribe("NWPMSKF.*");
            // 一次抓取100条sql执行的结果
            Message message = canalConnector.get(100);

            if(message.getEntries().size() == 0){
                System.out.println("当前没有数据，休息五秒钟");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else {
                for (CanalEntry.Entry entry : message.getEntries()) {
                    if(entry.getEntryType() == CanalEntry.EntryType.ROWDATA){
                        ByteString storeValue = entry.getStoreValue();
                        CanalEntry.RowChange rowChange = null;
                        try {
                            rowChange = CanalEntry.RowChange.parseFrom(storeValue);
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
                        String tableName = entry.getHeader().getTableName();

                        CanalHandler canalHandler = new CanalHandler(rowChange.getEventType(), tableName, rowDatasList);
                        canalHandler.handle();
                        ++mm;
                        System.out.println(mm);
                    }
                }
            }
        }
    }
}
