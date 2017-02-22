name := """microservices"""

version := "1.1.0"
organization := "com.github.durre"
scalaVersion := "2.11.8"
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.typesafe.akka" %% "akka-http" % "10.0.3",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.3",
  "com.typesafe.akka" %% "akka-stream" % "2.4.17",
  "com.typesafe.akka" %% "akka-stream-contrib" % "0.6",
  "com.typesafe.akka" %% "akka-stream-contrib-amqp" % "0.6",
  "com.auth0" % "java-jwt" % "3.1.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
