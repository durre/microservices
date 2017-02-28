package com.github.durre.microservice.http.directives

import java.util.UUID

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0}
import akka.http.scaladsl.server.directives.BasicDirectives.{extractExecutionContext, pass}
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import com.github.durre.microservice.models.{OAuthScope, RequestInfo}

import scala.concurrent.Future
import scala.util.Success

trait OwnershipAuthorization {

  /**
    * Based on the RequestInfo, verifies that the client owns this specific resource
    */
  def verifyOwnership(req: RequestInfo, resourceId: UUID, verify: (RequestInfo, UUID) => Boolean): Directive0 =
    if (verify(req, resourceId)) pass else reject(AuthorizationFailedRejection)


  /**
    * Based on the RequestInfo, verifies that the client owns this specific resource
    */
  def asyncVerifyOwnership(req: RequestInfo, resourceId: UUID, verify: (RequestInfo, UUID) => Future[Boolean]): Directive0 = {
    extractExecutionContext.flatMap { implicit ec =>
      val futureVerification = verify(req, resourceId)
      onComplete(futureVerification).flatMap {
        case Success(true) => pass
        case _ => reject(AuthorizationFailedRejection)
      }
    }
  }

  /**
    * Verifies that the client has the required scope
    */
  def verifyScope(required: OAuthScope, req: RequestInfo): Directive0 =
    verifyScopes(Set(required), req)

  /**
    * Verifies that the client has the required scopes
    */
  def verifyScopes(required: Set[OAuthScope], req: RequestInfo): Directive0 =
    if (OAuthScope.hasScopes(required, req.scopes)) pass else reject(AuthorizationFailedRejection)
}
