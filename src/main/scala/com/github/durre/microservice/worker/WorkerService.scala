package com.github.durre.microservice.worker

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.{AmqpConnectionUri, NamedQueueSourceSettings, QueueDeclaration}
import akka.stream.alpakka.amqp.scaladsl.AmqpSource
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContextExecutor

trait WorkerService {

  lazy val config: Config = ConfigFactory.load()
  lazy val log = Logger(config.getString("worker.name"))
  implicit lazy val system = ActorSystem(config.getString("worker.name"))
  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val ec: ExecutionContextExecutor = materializer.executionContext

  lazy val source = AmqpSource(
    settings = NamedQueueSourceSettings(
      connectionSettings = AmqpConnectionUri(config.getString("rabbitmq.uri")),
      queue = config.getString("worker.sourceQueue"),
      declarations = List(QueueDeclaration(name = config.getString("worker.sourceQueue"), durable = true))
    ),
    bufferSize = 2
  )
}
