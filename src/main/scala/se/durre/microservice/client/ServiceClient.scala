package se.durre.microservice.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.pattern.CircuitBreaker
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ServiceClient(host: String, port: Int, ssl: Boolean, internalSecret: Option[String])(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) {

  assert(
    !(host.startsWith("http://") || host.startsWith("https://")),
    s"Please don't include the protocol (http / https) in the hostname: $host"
  )

  private val breaker =
    CircuitBreaker(system.scheduler,
      maxFailures = 5,
      callTimeout = 20.seconds,
      resetTimeout = 30.seconds)

  private lazy val serviceFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    if (ssl) Http().outgoingConnectionHttps(host = host, port = port)
    else Http().outgoingConnection(host = host, port = port)

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
  )

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
