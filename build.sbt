name := """microservices"""

version := "1.0.0"
organization := "com.github.durre"
scalaVersion := "2.11.8"
libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.typesafe.play" %% "play-json" % "2.4.2",
  "com.typesafe.akka" %% "akka-http" % "10.0.0",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.0",
  "com.typesafe.akka" %% "akka-stream" % "2.4.14",
  "com.typesafe.akka" %% "akka-stream-contrib" % "0.4",
  "com.typesafe.akka" %% "akka-stream-contrib-amqp" % "0.4",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.4",
  "com.pauldijou" %% "jwt-play-json" % "0.5.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
