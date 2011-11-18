# ScalikeJDBC - A thin JDBC wrapper in Scala

### Just write SQL and start writing applications right now

This library is very simply implemented (similar to SpringJDBC) and it's usage is also very simple.

You just use PreparedStatement and map from java.sql.ResultSet to Option[A]/List[A] by yourself.

## sbt

```scala
resolvers ++= Seq(
  "seratch.github.com releases"  at "http://seratch.github.com/mvn-repo/releases"
)

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "0.1.0" withSources ()
)
```

## Operations

### DB accessor object

```scala
import scalikejdbc._
val conn = DriverManager.getConnection(url, user, password)
val db = new DB(conn)
```

or With Apache Commons DBCP:

```scala
import scalikejdbc._

Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.initialize(url, user, password)
val conn = ConnectionPool.borrow()
val db = new DB(conn)
```

Store a DB instance as thread-local variable:

```scala
def init() = {
  val newDB = ThreadLocalDB.create(conn)
  newDB.begin()
}
// after that..
def doSomething() = {
  val db = ThreadLocalDB.load()
}
```

### Query

```scala
val names: List[String] = db readOnly {
  session => session.asList("select * from emp") { rs =>
    Some(rs.getString("name"))
  }
}
```

```scala
val extractName = (rs: java.sql.ResultSet) => Some(rs.getString("name"))
val name: Option[String] = db readOnly {
  _.asOne("select * from emp where id = ?", 1)(extractName)
}
```

### Update

```scala
val count: Int = db autoCommit {
   _.update("update emp set name = ? where id = ?", "foo", 1)
}
```

### Execute

```scala
db autoCommit {
  _.execute("create table emp (id integer primary key, name varchar(30))")
}
```

## Transaction

### readOnly

```scala
val names = db readOnly {
  session => session.asList("select * from emp") {
    rs => Some(rs.getString("name"))
  }
}
```

```scala
val session = db.readOnlySession()
val names = session.asList("select * from emp") {
  rs => Some(rs.getString("name"))
}
val myName = session.asOne("select * from emp where id = ?", 1) {
  rs => Some(rs.getString("name"))
}
```

```scala
val count = db readOnly {
  _.update("update emp set name = ? where id = ?", "foo", 1)
}
// java.sql.SQLException!
```

### autoCommit

```scala
val count = db autoCommit {
  _.update("update emp set name = ? where id = ?", "foo", 1)
}
// cannot rollback
```

```scala
val session = db.autoCommitSession()
session.update("update emp set name = ? where id = ?", "foo", 1)
session.update("update emp set name = ? where id = ?", "bar", 2)
// cannot rollback
```

### localTx

```scala
val count = db localTx {
  session => {
    session.update("update emp set name = ? where id = ?", "foo", 1)
    session.update("update emp set name = ? where id = ?", "bar", 2)
  }
}
// cannot rollback
```

### withinTx

```scala
db.begin()
val names = db withinTx {
  // if a transaction has not been started, IllegalStateException will be thrown
  session => session.asList("select * from emp") {
    rs => Some(rs.getString("name"))
  }
}
db.rollback() // might throw Exception
```

```scala
db.begin()
val session = db.withinTxSession()
val names = session.asList("select * from emp") {
  rs => Some(rs.getString("name"))
}
db.rollbackIfActive() // never throw Exception
```

### TxFilter

See also: https://github.com/seratch/scalikejdbc/tree/master/src/test/scala/snippet/unfiltered.scala

```scala
class TxFilter extends Filter {

  def init(filterConfig: FilterConfig) {
    Class.forName("org.hsqldb.jdbc.JDBCDriver")
    val (url, user, password) = ("jdbc:hsqldb:mem:hsqldb:TxFilter", "", "")
    ConnectionPool.initialize(url, user, password)
  }

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
    val conn = ConnectionPool.borrow()
    val db = ThreadLocalDB.create(conn)
    db.begin()
    try {
      chain.doFilter(req, res)
      db.commit()
    } catch {
      case e: Exception => {
        db.rollbackIfActive()
        throw e
      }
    }
  }

  def destroy() {}

}
```

Unfiltered example:

```scala
class Hello extends Plan {

  def intent = {
    case req @ GET(Path("/rollbackTest")) => {
      val db = ThreadLocalDB.load()
      db withinTx { _.update("update emp set name = ? where id = ?", "foo", 1) }
      throw new RuntimeException("Rollback Test!")
      // The transaction will rollback.
    }
  }

}

object Server extends App {
  unfiltered.jetty.Http.anylocal
    .filter(new TxFilter)
    .plan(new Hello)
    .run { s => unfiltered.util.Browser.open(
      "http://127.0.0.1:%d/hello".format(s.port))
    }
}
```
