package se.durre.microservice.worker

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.contrib.amqp.{AmqpConnectionUri, AmqpSource, NamedQueueSourceSettings, QueueDeclaration}
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContextExecutor

trait WorkerService {

  def rabbitMqUri: String
  def workerConfig: Config

  lazy val logger = Logger(workerConfig.getString("name"))
  implicit lazy val system = ActorSystem(workerConfig.getString("name"))
  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val ec: ExecutionContextExecutor = materializer.executionContext

  lazy val source = AmqpSource(
    settings = NamedQueueSourceSettings(
      connectionSettings = AmqpConnectionUri(rabbitMqUri),
      queue = workerConfig.getString("sourceQueue"),
      declarations = List(QueueDeclaration(name = workerConfig.getString("sourceQueue"), durable = true))
    ),
    bufferSize = 2
  )
}
