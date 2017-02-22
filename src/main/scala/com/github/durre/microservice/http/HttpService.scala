package com.github.durre.microservice.http

import java.util.UUID

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives.{extractExecutionContext, pass}
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import com.auth0.jwt.{JWT, JWTVerifier}
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Claim
import com.typesafe.scalalogging.Logger
import com.github.durre.microservice.models.{OAuthScope, RequestInfo}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Success


trait HttpService {

  def internalSecretHeader: String = "X-Internal-Secret"
  def serviceToken: Option[String]
  def jwtSecret: String
  def log: Logger
  lazy val jwtVerifier: JWTVerifier = JWT.require(Algorithm.HMAC256(jwtSecret)).build()


  protected def toRequestInfo(claims: Map[String, Claim], jwt: String): Option[RequestInfo] = {
    for {
      userId <- claims.get("userId").map(_.asString).map(UUID.fromString)
      orgId <- claims.get("orgId").map(_.asString).map(UUID.fromString)
      scopes <- claims.get("scopes").map(_.asList(classOf[String]).asScala.toList).map(parseScopes)
    } yield RequestInfo(userId, orgId, scopes, jwt)
  }


  private def parseScopes(scopes: List[String]): Set[OAuthScope] =
    scopes
      .flatMap(scopeFromString)
      .toSet

  private def decodeToken(token: String): Option[RequestInfo] = {
    try {
      val jwt = jwtVerifier.verify(token)
      toRequestInfo(jwt.getClaims.asScala.toMap, token)
    } catch {
      case _: JWTVerificationException => None
    }
  }

  private def jwtAuthenticator(credentials: Credentials): Option[RequestInfo] = credentials match {
    case p @ Credentials.Provided(id) => decodeToken(id)
    case _ =>
      log.error("Invalid credentials provided for internal communication")
      None
  }

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
    * Make sure the client supplied the internal token the services use to communicate
    */
  def verifyServiceToken: Directive0 =
    serviceToken match {

      case Some(internalToken) =>
        optionalHeaderValueByName(internalSecretHeader).flatMap {
          case Some(secret) if secret == internalToken => pass
          case _ => reject(AuthorizationFailedRejection)
        }

      // No internal secure token needed
      case None => pass
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

  /**
    * Verify and extract the provided jwt
    */
  def authorizeJwt: Directive1[RequestInfo] =
    authenticateOAuth2("Microservice Realm", jwtAuthenticator)


  /**
    * Each service has it own set of scopes, which we know nothing about at this point
    */
  protected def scopeFromString(str: String): Option[OAuthScope]

}
