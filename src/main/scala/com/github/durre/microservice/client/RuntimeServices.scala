package com.github.durre.microservice.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

/**
  * Just a helper to reduce some boiler plate when initializing ServiceClient's
  */
trait RuntimeServices {

  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
  implicit def ec: ExecutionContext

  def clientConfig: Config
  def serviceNames: Set[String]
  def internalSecret: Option[String]

  lazy val services: Map[String, ServiceClient] = serviceNames.map { name =>
    val conf = clientConfig.getConfig(name)

    val client = new ServiceClient(
      host = conf.getString("host"),
      port = conf.getInt("port"),
      ssl = conf.getBoolean("ssl"),
      internalSecret = internalSecret
    )
    name -> client
  }.toMap

}
