package com.github.durre.microservice.worker

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.scaladsl.AmqpSink
import akka.stream.alpakka.amqp.{AmqpConnectionUri, AmqpSinkSettings, QueueDeclaration}
import akka.stream.scaladsl.{Flow, MergeHub, RunnableGraph, Sink, Source}
import akka.util.ByteString
import spray.json.JsValue

abstract class WorkPublisher[T](rabbitMqUri: String, durable: Boolean = true, ttl: Option[Int] = None)(implicit system: ActorSystem, mat: ActorMaterializer) {

  protected def queueName: String
  protected def toJson: (T) => JsValue

  private val queueArguments: Map[String, AnyRef] = ttl.map(t => Map("x-message-ttl" -> Int.box(t))).getOrElse(Map())


  private lazy val sink = AmqpSink.simple(
    AmqpSinkSettings(
      AmqpConnectionUri(rabbitMqUri),
      None,
      Some(queueName),
      List(QueueDeclaration(name = queueName, durable = durable, arguments = queueArguments))
    )
  )

  private lazy val serializer = Flow[T].map { job =>
    ByteString(toJson(job).toString())
  }

  private lazy val runnableGraph: RunnableGraph[Sink[T, NotUsed]] =
    MergeHub.source[T](perProducerBufferSize = 16)
      .via(serializer)
      .to(sink)

  private lazy val toQueue: Sink[T, NotUsed] = runnableGraph.run()

  /**
    * Publish the entity to the amqp queue
    */
  def publish(job: T): Unit =
    Source.single(job).runWith(toQueue)
}
