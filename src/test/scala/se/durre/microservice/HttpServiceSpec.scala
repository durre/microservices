package se.durre.microservice

import java.util.UUID

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.scalalogging.Logger
import org.scalatest.{FunSuite, Matchers}
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.libs.json.{JsObject, Json}
import se.durre.microservice.http.HttpService
import se.durre.microservice.models.OAuthScope


class HttpServiceSpec extends FunSuite with ScalatestRouteTest with Matchers with HttpService  {

  val interCommunicationToken: Option[String] = Some("internal")
  val jwtSecret: String = "secret"

  case object MyScope extends OAuthScope { val name: String = "myscope" }
  case object AnotherScope extends OAuthScope { val name: String = "anotherscope" }

  override protected def scopeFromString(str: String): Option[OAuthScope] = str match {
    case "myscope" => Some(MyScope)
    case "another" => Some(AnotherScope)
    case _ => None
  }

  val route =
    get {
      authorize(Set(MyScope)) { _ =>
        complete("OK")
      }
    }

  def createJwt(claims: JsObject): String = JwtJson.encode(claims, jwtSecret, JwtAlgorithm.HS256)

  test("test authorization reject when you don't supply the internal token") {

    val jwt = createJwt(Json.obj(
      "userId" -> UUID.randomUUID().toString,
      "orgId" -> UUID.randomUUID().toString,
      "scopes" -> "myscope",
      "exp" -> 100
    ))

    val req = Get()
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("test authorization reject when you have the wrong set of scopes") {

    val jwt = createJwt(Json.obj(
      "userId" -> UUID.randomUUID().toString,
      "orgId" -> UUID.randomUUID().toString,
      "scopes" -> "another"
    ))

    val req = Get()
      .addHeader(RawHeader("X-Internal-Secret", interCommunicationToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("test all good when you have all the right tokens") {

    val jwt = createJwt(Json.obj(
      "userId" -> UUID.randomUUID().toString,
      "orgId" -> UUID.randomUUID().toString,
      "scopes" -> Set("myscope")
    ))

    val req = Get()
      .addHeader(RawHeader("X-Internal-Secret", interCommunicationToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  override def log: Logger = Logger("tests")
}
