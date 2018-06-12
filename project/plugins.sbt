// workaround for https://github.com/scala/bug/issues/10870
scalaVersion := "2.12.4"

resolvers ++= Seq(
  "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.17")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
