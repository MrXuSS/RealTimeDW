
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * @Author:XuChengLei
 * @Date:2020-07-07
 */
public class HBaseTest {

    private static Connection connection = null;

    static {
        try {
            //设置Hbase的链接属性
            Configuration configuration = HBaseConfiguration.create();
            configuration.set("hbase.zookeeper.quorum", "node1");

            //获取connection对象
            connection = ConnectionFactory.createConnection(configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取所有数据
     *
     * @param tableName
     * @throws IOException
     */
    public static void getRows(String tableName) throws IOException {
        if (isTableExist(tableName)) {

            Table table = connection.getTable(TableName.valueOf(tableName));

            //创建Scan对象，通过该对象获取该表所有数据
            Scan scan = new Scan();

            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                //result里存放的是这行数据的所有cell，可以通过rawCells方法获取cells
                Cell[] cells = result.rawCells();
                //cells是所有cell的集合，可以遍历它获取每一个cell
                for (Cell cell : cells) {
                    //cell.getQualifierArray()等getXXX方法获取的是这一行所有的数据而不是方法名对应的数据
//                byte[] qualifierArray = cell.getQualifierArray();
//                System.out.println(Bytes.toString(qualifierArray));

                    //通过HBase自带的CellUtil获取cell的所有属性
                    byte[] rowkeyByte = CellUtil.cloneRow(cell);
                    byte[] familyByte = CellUtil.cloneFamily(cell);
                    byte[] columnByte = CellUtil.cloneQualifier(cell);
                    byte[] valueByte = CellUtil.cloneValue(cell);
                    //通过Bytes对象提供的toString方法将字节数组转换成字符串进行打印
                    System.out.println(
                            Bytes.toString(rowkeyByte) + "-" +
                                    Bytes.toString(familyByte) + ":" +
                                    Bytes.toString(columnByte) + "-" +
                                    Bytes.toString(valueByte)
                    );
                }
            }

            table.close();


        } else {
            System.out.println(tableName + "表不存在！");
        }
    }
    /**
     * 判断表是否存在
     *
     * @param tableName
     * @return
     * @throws IOException
     */
    public static Boolean isTableExist(String tableName) throws IOException {
        //获取admin对象
        Admin admin = connection.getAdmin();

        //判断表是否存在，需注意参数类型
        boolean result = admin.tableExists(TableName.valueOf(tableName));

        //用完关闭
        admin.close();

        return result;
    }

    public static void main(String[] args) throws IOException {
        new HBaseTest().getRows("hs_jldxx");
    }

}
