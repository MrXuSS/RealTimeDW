package com.haiyi;

import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CreateTable {

    private static ColumnSchema newColumn(String name, Type type, boolean iskey) {
        ColumnSchema.ColumnSchemaBuilder column = new ColumnSchema.ColumnSchemaBuilder(name, type);
        column.key(iskey);
        return column.build();
    }

    public static void main(String[] args) throws KuduException {
        // master地址
        String masteraddr = "node1";
        // 创建kudu的数据库链接
        KuduClient client = new KuduClient.KuduClientBuilder(masteraddr).defaultSocketReadTimeoutMs(6000).build();

        // 设置表的schema
        List<ColumnSchema> columns = new LinkedList<ColumnSchema>();
        /**
         与 RDBMS 不同，Kudu 不提供自动递增列功能，因此应用程序必须始终在插入期间提供完整的主键
         */
        addColumns(columns);
        Schema schema = new Schema(columns);
        //创建表时提供的所有选项
        CreateTableOptions options = new CreateTableOptions();
        // 设置表的replica备份和分区规则
        List<String> parcols = new LinkedList<String>();
        parcols.add("GZDBH");

        //设置表的备份数
        options.setNumReplicas(1);

        //设置hash分区和数量
        options.addHashPartitions(parcols, 3);
        try {
            client.createTable("hs_jldxx", schema, options);
        } catch (KuduException e) {
            e.printStackTrace();
        } finally {

            client.close();
        }
    }

    public static void addColumns(List<ColumnSchema> columns){
        columns.add(newColumn("GZDBH",Type.STRING,true));
        ArrayList<String> columnsArrayList = new ArrayList<String>(Arrays.asList("JLDBH",
                "YWLBDM",
                "JLDXH",
                "CBJHBH",
                "CBQDBH",
                "CBZQ",
                "JLDCBSXH",
                "DFNY",
                "BQCBCS",
                "YCBCS",
                "JSLXDM",
                "CZCS",
                "CZNY",
                "JSHH",
                "YHBH",
                "YHMC",
                "YDDZ",
                "YHZTDM",
                "DJBBBH",
                "JLDYTDM",
                "JLFSDM",
                "JLDYDJDM",
                "XLXDBS",
                "TQBS",
                "WYJJSFSDM",
                "YHWYJQSR",
                "DJCLBH",
                "YGDDL",
                "DYBH",
                "DYZH",
                "DLJSFSDM",
                "DLDBZ",
                "DBKJBZ",
                "DLFTGS",
                "BSFTFSDM",
                "YGBSXYZ",
                "WGBSXYZ",
                "BSJFBZ",
                "XSJSFSDM",
                "YGXSJSZ",
                "WGXSJSZ",
                "XSJFBZ",
                "XSFTBZ",
                "XSFTXYZ",
                "YDRL",
                "PTBBDM",
                "CTBBDM",
                "YDLBDM",
                "DJDM",
                "DJ",
                "GLYSBZDM",
                "HYFLDM",
                "FSJFBZ",
                "JBDFJSFSDM",
                "XLHDZ",
                "GDLL",
                "JBDFFTFS",
                "JBDFFTZ",
                "LTDFJSFS",
                "JTDLBL",
                "ZJSL",
                "FGZS",
                "SJLX",
                "YGZDBH",
                "GLYSKHFSDM",
                "JFYXJ",
                "GDDWBM",
                "JSBZ",
                "HSZTDM",
                "SCCBRQ",
                "CBRQ",
                "YDTS",
                "CYTZDF",
                "YGFBKJDL",
                "YGBSDL",
                "YGXSDL",
                "YGFTDL",
                "YGTBDL",
                "YGZDL",
                "WGFBKJDL",
                "WGBSDL",
                "WGXSDL",
                "WGFTDL",
                "WGTBDL",
                "KFRQ",
                "JSRQ",
                "FXRQ",
                "JTLX",
                "YGCJDL",
                "YGHBDL",
                "WGCJDL",
                "WGHBDL",
                "NLJDL",
                "MFDL",
                "TBDF",
                "YSDF",
                "WGZDL",
                "ZJRL",
                "JFRL",
                "JFXL",
                "JBFDJ",
                "SJLL",
                "TZXS",
                "ZBJLDBH",
                "KJXBDL",
                "JFDL",
                "DDDF",
                "JBDF",
                "LTDF",
                "FJFHJ",
                "ZDF",
                "PJLXDM",
                "YHLBDM",
                "CXDM",
                "YXBZ",
                "CZRBS",
                "CZSJ",
                "DWBM",
                "DQBM",
                "CJSJ",
                "SCDL",
                "SCGZDBH",
                "CDZXLDF",
                "GTSYDL",
                "CJSBZ",
                "GLBH",
                "JTNF",
                "BZ",
                "TCDM",
                "TCLJDF",
                "TCKSSJ",
                "TCJSSJ",
                "TBGZDBH",
                "JLDZTDM",
                "HCRQ"));
        for (String column : columnsArrayList) {
            columns.add(newColumn(column,Type.STRING,false));
        }
    }
}
