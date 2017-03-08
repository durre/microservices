package com.github.durre.microservice.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

/**
  * Move some of the boiler plate in here
  */
trait HttpService {

  val config: Config = ConfigFactory.load()
  lazy val serviceName: String = config.getString("http.serviceName")
  lazy val httpInterface: String = config.getString("http.interface")
  lazy val httpPort: Int = config.getInt("http.port")

  lazy val log = Logger(serviceName)
  implicit lazy val system = ActorSystem(serviceName)
  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val executionContext: ExecutionContext = system.dispatcher

  /**
    * The user implements this
    */
  def route: Route

  def startService(): Unit = {
    Http().bindAndHandle(route, httpInterface, httpPort)
    log.info(s"Service ($serviceName) up and running at port $httpPort")
  }

}
