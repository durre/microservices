package com.github.durre.microservice.http.directives

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}

class InternalServiceSecuritySpec extends FunSuite with ScalatestRouteTest with Matchers with InternalServiceSecurity {

  val serviceToken = "secret"
  val secretHeader = "X-Secret-Header"

  val route: Route =
    verifyServiceToken(serviceToken, secretHeader) {
      path("users") {
        get {
          complete("OK")
        }
      }
    }

  test("reject request when you don't supply the internal token") {
    val req = Get("/users")

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("reject request when you supply the wrong internal token") {
    val req = Get("/users")
      .addHeader(RawHeader(secretHeader, "wrongToken"))

    req ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("grant access when you supply the correct token") {
    val req = Get("/users")
      .addHeader(RawHeader(secretHeader, serviceToken))

    req ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }
}
