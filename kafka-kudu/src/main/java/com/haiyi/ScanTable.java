package com.haiyi;

import org.apache.kudu.Schema;
import org.apache.kudu.client.*;

import java.util.ArrayList;

public class ScanTable {
    public static void main(String[] args) throws KuduException {
        // master地址
        final String masteraddr = "172.19.138.6,172.19.138.7,172.19.138.8";
        // 创建kudu的数据库链接
        KuduClient client = new KuduClient.KuduClientBuilder(masteraddr).build();
        //打开kudu表
        KuduTable table = client.openTable("LC_CBXX");

        Schema schema = table.getSchema();
        KuduPredicate predicate = KuduPredicate.newComparisonPredicate(schema.getColumn("dqbm"), KuduPredicate.ComparisonOp.EQUAL, "030600");

        //ArrayList<String> arrayList = new ArrayList<String>();
        //arrayList.add("jldbh");


        //创建scanner扫描
        KuduScanner scanner = client.newScannerBuilder(table)
                //.setProjectedColumnNames(arrayList)
                .addPredicate(predicate)
                .build();
        //遍历数据
        while (scanner.hasMoreRows()){
//            for (RowResult rowResult : scanner.nextRows()) {
//                //System.out.println(rowResult.getString("jldbh") + "\t" + rowResult.getString("gzdbh")) ;
//                //System.out.println(rowResult.rowToString());
//            }
            RowResultIterator rowResultIterator = scanner.nextRows();
            while (rowResultIterator.hasNext()){
                RowResult next = rowResultIterator.next();
                //System.out.println(next.getObject("dfny").toString());
                System.out.println(next.rowToString());
            }
        }
    }
}
