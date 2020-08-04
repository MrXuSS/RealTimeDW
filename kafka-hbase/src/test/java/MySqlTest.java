import org.apache.flink.annotation.Public;
import org.junit.Test;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.sql.*;

/**
 * @Author:XuChengLei
 * @Date:2020-07-13
 */
public class MySqlTest {

    String url = "jdbc:mysql://192.168.2.201:3306/NWPMSKF";
    String user = "root";
    String password="123456";

    Connection connection;

    {
        try {
            connection = DriverManager.getConnection(url,user,password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void updateMySql() throws SQLException {
        String sql = "update hs_jldxx set JLDBH = ? where  GZDBH = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < 100000; i++) {
            statement.setString(1,String.valueOf(i+1));
            statement.setString(2,String.valueOf(i));
            int result = statement.executeUpdate();
            System.out.println(i+"修改成功");
        }
        statement.close();
    }

    @Test
    public void deleteMySql() throws SQLException {
        String sql="delete from hs_jldxx";
        PreparedStatement statement = connection.prepareStatement(sql);
        int result = statement.executeUpdate();
        System.out.println("数据已清空");
    }

    @Test
    public void insertIntoMySql() throws SQLException {
        for (int i = 0; i < 5000; i ++) {
            String sql= "insert into hs_jldxx values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            for (int j = 0; j < 136; j++) {
                preparedStatement.setString(j+1,String.valueOf(i));
            }
            int result = preparedStatement.executeUpdate();
            preparedStatement.close();
            System.out.println(i+"插入成功");
        }
    }
}
