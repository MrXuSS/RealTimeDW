package com.haiyi;

import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.ListTablesResponse;

import java.util.List;

public class ShowTables {
    public static void main(String[] args) throws KuduException {
        // master地址
        final String masteraddr = "node1";
        // 创建kudu的数据库链接
        KuduClient client = new KuduClient.KuduClientBuilder(masteraddr).defaultSocketReadTimeoutMs(6000).build();

        try {
            //获取现有表的列表
            ListTablesResponse tablesList = client.getTablesList();
            List<String> tablesList1 = tablesList.getTablesList();
            //遍历列表中所有信息
            for (String s : tablesList1) {
                System.out.println(s);
            }
        } catch (KuduException e) {
            e.printStackTrace();
        } finally {
            client.close();
        }

    }
}
