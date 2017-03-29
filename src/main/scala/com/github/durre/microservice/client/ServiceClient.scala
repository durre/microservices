package com.github.durre.microservice.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import akka.pattern.CircuitBreaker
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object ServiceClient {

  private val urlRegex = """((?i:http[s]?)):\/\/((?i:[a-z0-9-.]+)):?(\d+)?""".r

  def extractUrlParts(url: String): (Boolean, String, Option[Int]) =
    url match {
      case urlRegex(protocol, hostname, null) => (protocol.toLowerCase == "https", hostname, None)
      case urlRegex(protocol, hostname, port) => (protocol.toLowerCase == "https", hostname, Some(port.toInt))
      case _ => throw new RuntimeException("Invalid format of ServiceClient url"+url)
    }
}

class ServiceClient(url: String, serviceTokenHeader: Option[String], serviceToken: Option[String] = None)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) {

  import ServiceClient._

  // Append this to every request
  private val staticHeaders = (serviceTokenHeader, serviceToken) match {
    case (Some(header), Some(token)) => Seq(RawHeader(header, token))
    case _ => Seq()
  }

  private lazy val serviceFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = {
    val (ssl, host, maybePort) = extractUrlParts(url)
    if (ssl) Http().outgoingConnectionHttps(host = host, port = maybePort.getOrElse(443))
    else Http().outgoingConnection(host = host, port = maybePort.getOrElse(80))
  }


  private val breaker =
    CircuitBreaker(system.scheduler,
      maxFailures = 5,
      callTimeout = 20.seconds,
      resetTimeout = 30.seconds)

  def get(uri: String, jwt: String): Future[ClientResponse] =
    breaker.withCircuitBreaker(
      Source.single(HttpRequest(method = HttpMethods.GET, uri = uri, headers = authorizationHeader(jwt)))
        .via(serviceFlow)
        .runWith(Sink.head)
        .map(toClientResponse)
    )

  def post(uri: String, json: JsValue = JsNull, jwt: String): Future[ClientResponse] =
    breaker.withCircuitBreaker(
      Source.single(HttpRequest(method = HttpMethods.POST, uri = uri, entity = body(json), headers = authorizationHeader(jwt)))
        .via(serviceFlow)
        .runWith(Sink.head)
        .map(toClientResponse)
    )

  def put(uri: String, json: JsValue = JsNull, jwt: String): Future[ClientResponse] =
    breaker.withCircuitBreaker(
      Source.single(HttpRequest(method = HttpMethods.PUT, uri = uri, entity = body(json), headers = authorizationHeader(jwt)))
        .via(serviceFlow)
        .runWith(Sink.head)
        .map(toClientResponse)
    )

  def patch(uri: String, json: JsValue = JsNull, jwt: String): Future[ClientResponse] =
    breaker.withCircuitBreaker(
      Source.single(HttpRequest(method = HttpMethods.PATCH, uri = uri, entity = body(json), headers = authorizationHeader(jwt)))
        .via(serviceFlow)
        .runWith(Sink.head)
        .map(toClientResponse)
    )

  def delete(uri: String, jwt: String): Future[ClientResponse] =
    breaker.withCircuitBreaker(
      Source.single(HttpRequest(method = HttpMethods.DELETE, uri = uri, headers = authorizationHeader(jwt)))
        .via(serviceFlow)
        .runWith(Sink.head)
        .map(toClientResponse)
    )


  private def body(json: JsValue): RequestEntity = json match {
    case JsNull => HttpEntity.Empty
    case json: JsValue => HttpEntity(ContentTypes.`application/json`, json.toString())
  }

  private def authorizationHeader(jwt: String): Seq[HttpHeader] = Seq(
    Authorization(OAuth2BearerToken(jwt))
  ) ++ staticHeaders



  private def toClientResponse(httpResponse: HttpResponse): ClientResponse = {
    httpResponse.entity match {
      case HttpEntity.Strict(ContentTypes.`application/json`, data) =>
        val json = data.decodeString("UTF-8").parseJson
        json match {
          case obj: JsObject =>
            ClientResponse(
              httpResponse.status.intValue(),
              ResponseBody.JsonObject(obj)
            )
          case array: JsArray =>
            ClientResponse(
              httpResponse.status.intValue(),
              ResponseBody.JsonArray(array)
            )
          case _ => throw new RuntimeException(s"Unexpected JSON: $json")
        }


      case HttpEntity.Strict(ContentTypes.`text/plain(UTF-8)`, data) =>
        ClientResponse(
          httpResponse.status.intValue(),
          ResponseBody.Text(data.decodeString("UTF-8"))
        )

      // There are a lot more cases to cover but this will have to do for now
      case _ =>
        ClientResponse(
          httpResponse.status.intValue(),
          ResponseBody.Empty
        )
    }
  }
}
