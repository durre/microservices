package com.github.durre.microservice.http.directives

import java.util.UUID

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.durre.microservice.models.TestModels.MyScope
import com.github.durre.microservice.models.{OAuthScope, TestModels}
import org.scalatest.{FunSuite, Matchers}

class TokenAuthorizationSpec extends FunSuite with ScalatestRouteTest with Matchers with TokenAuthorization {

  val jwtSecret: String = "secret"
  override protected def scopeFromString(str: String): Option[OAuthScope] = TestModels.scopeFromString(str)

  val userId: UUID = UUID.randomUUID()
  val orgId: UUID = UUID.randomUUID()

  def createJwt(scopes: Set[OAuthScope], secret: String = jwtSecret): String =
    JWT.create()
      .withClaim("userId", userId.toString)
      .withClaim("orgId", orgId.toString)
      .withArrayClaim("scopes", scopes.map(_.name).toArray)
      .sign(Algorithm.HMAC256(secret))

  val route: Route =
    authorizeToken("realm") { _ =>
      path("users") {
        get {
          complete("OK")
        }
      }
    }

  test("reject request when you dont supply the Authorization header") {
    val req = Get("/users")

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("reject request when you supply an invalid jwt") {
    val jwt = createJwt(Set(MyScope), secret = "invalidsecret")

    val req = Get("/users")
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("grant access when you supply a valid jwt") {
    val jwt = createJwt(Set(MyScope), secret = jwtSecret)

    val req = Get("/users")
      .addHeader(Authorization(OAuth2BearerToken(jwt)))

    req ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

}
