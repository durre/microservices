package com.github.durre.microservice.http.directives

import java.util.UUID

import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.durre.microservice.models.TestModels.{AnotherScope, MyScope}
import com.github.durre.microservice.models.{RequestInfo, TestModels}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

class OwnershipAuthorizationSpec extends FunSuite with ScalatestRouteTest with Matchers with OwnershipAuthorization {

  val verifyOrganizationOwner = (req: RequestInfo, resourceId: UUID) => req.orgId == resourceId
  val asyncVerifyOrganizationOwner = (req: RequestInfo, resourceId: UUID) => Future.successful(req.orgId == resourceId)

  test("grant access when you have the correct scope") {
    val req = TestModels.requestInfo.copy(scopes = Set(MyScope))
    val route = verifyScope(MyScope, req) { complete("OK") }

    Get("/") ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  test("grant access when you have the correct scopes") {
    val req = TestModels.requestInfo.copy(scopes = Set(MyScope, AnotherScope))
    val route = verifyScopes(Set(MyScope, AnotherScope), req) { complete("OK") }

    Get("/") ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  test("reject request when you have the wrong scopes") {
    val req = TestModels.requestInfo.copy(scopes = Set(AnotherScope))
    val route = verifyScope(MyScope, req) { complete("OK") }

    Get("/") ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("forbid access to a resource you down own") {
    val req = TestModels.requestInfo
    val resourceId = UUID.randomUUID()
    val route = verifyOwnership(req, resourceId, verifyOrganizationOwner) { complete("OK") }

    Get("/") ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("grant access to a resource you own") {
    val req = TestModels.requestInfo
    val route = verifyOwnership(req, req.orgId, verifyOrganizationOwner) { complete("OK") }

    Get("/") ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  test("async forbid access to a resource you down own") {
    val req = TestModels.requestInfo
    val resourceId = UUID.randomUUID()
    val route = asyncVerifyOwnership(req, resourceId, asyncVerifyOrganizationOwner) { complete("OK") }

    Get("/") ~> route ~> check {
      rejection === AuthorizationFailedRejection
    }
  }

  test("async grant access to a resource you own") {
    val req = TestModels.requestInfo
    val route = asyncVerifyOwnership(req, req.orgId, asyncVerifyOrganizationOwner) { complete("OK") }

    Get("/") ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

}
