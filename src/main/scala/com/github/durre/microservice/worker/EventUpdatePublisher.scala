package com.github.durre.microservice.worker

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import com.github.durre.microservice.formats.CommonFormats
import com.github.durre.microservice.models.EventUpdate
import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsString, JsValue}

/**
  * Gathers all updates that affect the client so we can push them out using a websocket
  */
class EventUpdatePublisher (rabbitMqUri: String)(implicit system: ActorSystem, mat: ActorMaterializer)
  extends WorkPublisher[EventUpdate](rabbitMqUri, durable = false, ttl = Some(60 * 1000 * 10))
    with SprayJsonSupport
    with DefaultJsonProtocol
    with CommonFormats {

  val queueName: String = "event-updates"

  override protected def toJson: (EventUpdate) => JsValue = (o) =>
    JsObject(
      "orgId" -> UUIDJsonFormat.write(o.orgId),
      "requiredScopes" -> JsArray(o.requiredScopes.map(s => JsString(s.name)).toVector),
      "payload" -> o.payload
    )
}
