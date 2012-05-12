package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class ExecutableSQLParserSpec extends FlatSpec with ShouldMatchers {

  behavior of "ExecutableSQLParser"

  it should "parse a str select query" in {
    val sql = "select * from user where id = /* 'id */123 and user_name = /* 'userName */'Alice'"
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(2)
    params(0) should equal('id)
    params(1) should equal('userName)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal("select * from user where id = ? and user_name = ?")
  }

  it should "parse a str select query using double quart" in {
    val sql = "select * from user where id = /* 'id */123 and user_name = /* 'userName */\"Alice\""
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(2)
    params(0) should equal('id)
    params(1) should equal('userName)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal("select * from user where id = ? and user_name = ?")
  }

  it should "parse an all capital str select query" in {
    val sql = "SELECT * FROM USER WHERE ID = /* 'id */123 AND USER_NAME = /* 'userName */'Alice'"
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(2)
    params(0) should equal('id)
    params(1) should equal('userName)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal("SELECT * FROM USER WHERE ID = ? AND USER_NAME = ?")
  }

  it should "parse an all capital str select query using functions" in {
    val sql = "SELECT MAX(TEMP_F), MIN(TEMP_F), AVG(RAIN_I), ID, (TEMP_F-32)*5/(9+1) FROM STATS GROUP BY ID;"
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(0)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal(sql)
  }

  it should "parse a str select query with table name" in {
    val sql = "select user.* from user where id = /* 'id */123 and user_name = /* 'userName */'Alice'"
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(2)
    params(0) should equal('id)
    params(1) should equal('userName)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal("select user.* from user where id = ? and user_name = ?")
  }

  it should "parse a str select query with other comments" in {
    val sql =
      """select *  -- wildcard
         from user /* TODO something? */
         where id = /* 'id */123 -- TODO
         and user_name = /* 'userName */'Alice' /* first name */
        |
      """.stripMargin
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(2)
    params(0) should equal('id)
    params(1) should equal('userName)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal(
      "select * from user where id = ? and user_name = ?"
    )
  }

  it should "parse a str contains >" in {
    val sql =
      """SELECT customer_state, COUNT(customer_id) As total
        | FROM customers
        | WHERE group = /*'groupName*/'blue'
        | GROUP BY customer_state
        | HAVING COUNT(customer_id) > 5
        |;
      """.stripMargin
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(1)
    params(0) should equal('groupName)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal(
      "SELECT customer_state, COUNT(customer_id) As total FROM customers WHERE group = ? " +
        "GROUP BY customer_state HAVING COUNT(customer_id) > 5;"
    )
  }

  it should "parse an insert SQL" in {
    val sql =
      """INSERT INTO customers(customer_id, customer_name)
        | VALUES(/*'customerId*/'12345', /* 'customerName */'GIS Experts')
      """.stripMargin
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(2)
    params(0) should equal('customerId)
    params(1) should equal('customerName)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal(
      "INSERT INTO customers(customer_id, customer_name) VALUES(?, ?)"
    )
  }

  it should "parse an insert SQL without parameters" in {
    val sql =
      """INSERT INTO customers(customer_id, customer_name)
        | VALUES('12345', 'GIS Experts')
      """.stripMargin
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(0)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal(
      "INSERT INTO customers(customer_id, customer_name) VALUES('12345', 'GIS Experts')"
    )
  }

  it should "parse an insert SQL without parameters using double quart" in {
    val sql =
      """INSERT INTO customers(customer_id, customer_name)
        | VALUES("12345", "GIS Experts")
      """.stripMargin
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(0)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal(
      "INSERT INTO customers(customer_id, customer_name) VALUES(\"12345\", \"GIS Experts\")"
    )
  }

  it should "parse an update SQL" in {
    val sql =
      """UPDATE
           customers
         SET
           rating = /*'rating*/'Good'
         FROM
           orders
         WHERE
           orderdate > /*'orderDate*/'2005-01-01'
           and
           orders.customer_id = customers.customer_id
      """.stripMargin
    val params = ExecutableSQLParser.extractAllParameters(sql)
    params.size should equal(2)
    params(0) should equal('rating)
    params(1) should equal('orderDate)
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
    sqlWithPlaceHolders should equal(
      "UPDATE customers SET rating = ? FROM orders WHERE orderdate > ? and orders.customer_id = customers.customer_id"
    )
  }

  it should "parse ddl" in {
    val createTable = "create table user (id bigint auto_increment, name varchar(256), created_at timestamp)"
    ExecutableSQLParser.extractAllParameters(createTable).size should equal(0)
    ExecutableSQLParser.convertToSQLWithPlaceHolders(createTable) should equal(createTable)
    val dropTable = "drop table user"
    ExecutableSQLParser.extractAllParameters(dropTable).size should equal(0)
    ExecutableSQLParser.convertToSQLWithPlaceHolders(dropTable) should equal(dropTable)
  }

}
