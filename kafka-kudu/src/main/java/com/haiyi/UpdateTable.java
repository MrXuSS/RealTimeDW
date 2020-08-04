package com.haiyi;

import org.apache.kudu.client.*;

public class UpdateTable {
    public static void main(String[] args) throws KuduException{
        // master地址
        String masteraddr = "hadoop102,hadoop103,hadoop104";
        // 创建kudu的数据库链接
        KuduClient client = new KuduClient.KuduClientBuilder(masteraddr).build();
        // 打开表
        KuduSession session = null;
        try {
            KuduTable table = client.openTable("student");
            session = client.newSession();
            session.setFlushMode(SessionConfiguration.FlushMode.AUTO_FLUSH_SYNC);
            //更新数据
            Update update = table.newUpdate();
            PartialRow row = update.getRow();
            row.addInt("id", 1);
            row.addString("name", "di");
            session.apply(update);
        } catch (KuduException e) {
            e.printStackTrace();
        } finally {

            session.close();

            client.close();
        }
    }
}
