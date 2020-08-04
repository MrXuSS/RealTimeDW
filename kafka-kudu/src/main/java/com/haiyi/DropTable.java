package com.haiyi;

import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;

public class DropTable {
    public static void main(String[] args) throws KuduException {
        String masterAddress = "node1";
        KuduClient client = new KuduClient.KuduClientBuilder(masterAddress).defaultSocketReadTimeoutMs(6000).build();
        try {
            client.deleteTable("hs_jldxx");
        } catch (KuduException e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}
