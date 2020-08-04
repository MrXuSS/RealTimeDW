package com.haiyi;

import org.apache.kudu.client.*;

public class InsertRow {
    public static void main(String[] args) throws KuduException {
        String masterAddr = "node1";
        KuduClient client = new KuduClient.KuduClientBuilder(masterAddr).defaultSocketReadTimeoutMs(6000).build();
        try {
            KuduTable table = client.openTable("student");
            KuduSession kuduSession = client.newSession();
            kuduSession.setFlushMode(SessionConfiguration.FlushMode.MANUAL_FLUSH);
            kuduSession.setMutationBufferSpace(3000);
            for (int i = 1; i < 10; i++) {
                Insert insert = table.newInsert();
                insert.getRow().addInt("id", i);
                insert.getRow().addString("name", i + "号");
                kuduSession.flush();
                kuduSession.apply(insert);
            }
            kuduSession.close();
        } catch (KuduException e) {
            e.printStackTrace();
        } finally {

            client.close();
        }
    }
}
