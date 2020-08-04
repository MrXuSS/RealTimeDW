package com.haiyi.app

import java.sql.{Connection, DriverManager}

/**
 * @Author:XuChengLei
 * @Date:2020-07-21
 *
 */
object MySqlTest {

  def main(args: Array[String]): Unit = {
    // connect to the database named "mysql" on the localhost
    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://192.168.2.201/NWPMSKF"
    val username = "root"
    val password = "123456"

    var connection:Connection = null

    try {

      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)

      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("select * from hs_jldxx")
      while ( resultSet.next() ) {
        val name = resultSet.getString("GZDBH")
        val password = resultSet.getString("JLDBH")
        println( name + ", " + password)
      }
    } catch {
      case e => e.printStackTrace

    }
    connection.close()
  }
}
