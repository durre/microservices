package com.github.durre.microservice

import java.util.UUID

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.scalalogging.Logger
import org.scalatest.{FunSuite, Matchers}
import com.github.durre.microservice.http.HttpService
import com.github.durre.microservice.models.{OAuthScope, RequestInfo}

import scala.concurrent.Future


class HttpServiceSpec extends FunSuite with ScalatestRouteTest with Matchers with HttpService  {

  val serviceToken: Option[String] = Some("internal")
  val jwtSecret: String = "secret"

  case object MyScope extends OAuthScope { val name: String = "myscope" }
  case object AnotherScope extends OAuthScope { val name: String = "anotherscope" }

  override protected def scopeFromString(str: String): Option[OAuthScope] = str match {
    case "myscope" => Some(MyScope)
    case "another" => Some(AnotherScope)
    case _ => None
  }

  val verify = (req: RequestInfo, resourceId: UUID) => req.orgId == resourceId
  val asyncVerify = (req: RequestInfo, resourceId: UUID) => Future.successful(req.orgId == resourceId)

  val route: Route =
    verifyServiceToken {
      authorizeJwt { req =>
        path("protected") {
          get {
            verifyScope(MyScope, req) {
              complete("OK")
            }
          }
        } ~
          path("resource" / JavaUUID) { id =>
            get {
              verifyScope(MyScope, req) {
                verifyOwnership(req, id, verify) {
                  complete("OK")
                }
              }
            }
          } ~
          path("async-resource" / JavaUUID) { id =>
            get {
              verifyScope(MyScope, req) {
                asyncVerifyOwnership(req, id, asyncVerify) {
                  complete("OK")
                }
              }
            }
          }
      }
    }



  val userId: UUID = UUID.randomUUID()
  val orgId: UUID = UUID.randomUUID()
  
  def createJwt(scopes: Set[OAuthScope]): String =
    JWT.create()
      .withClaim("userId", userId.toString)
      .withClaim("orgId", orgId.toString)
      .withArrayClaim("scopes", scopes.map(_.name).toArray)
      .sign(Algorithm.HMAC256(jwtSecret))

  test("test authorization reject when you don't supply the internal token") {

    val jwt = createJwt(Set(MyScope))

    val req = Get("/protected")
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("test authorization reject when you have the wrong set of scopes") {

    val jwt = createJwt(Set(AnotherScope))

    val req = Get("/protected")
      .addHeader(RawHeader(internalSecretHeader, serviceToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("test all good when you have all the right tokens") {

    val jwt = createJwt(Set(MyScope))

    val req = Get("/protected")
      .addHeader(RawHeader(internalSecretHeader, serviceToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  test("forbid access to specific resources") {

    val jwt = createJwt(Set(MyScope))

    val req = Get("/resource/"+UUID.randomUUID().toString)
      .addHeader(RawHeader(internalSecretHeader, serviceToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("grant access to specific resources") {
    
    val jwt = createJwt(Set(MyScope))

    val req = Get("/resource/"+orgId.toString)
      .addHeader(RawHeader(internalSecretHeader, serviceToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  test("async forbid access to specific resources") {

    val jwt = createJwt(Set(MyScope))

    val req = Get("/async-resource/"+UUID.randomUUID().toString)
      .addHeader(RawHeader(internalSecretHeader, serviceToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("async grant access to specific resources") {

    val jwt = createJwt(Set(MyScope))

    val req = Get("/async-resource/"+orgId.toString)
      .addHeader(RawHeader(internalSecretHeader, serviceToken.get))
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  override def log: Logger = Logger("tests")
}
