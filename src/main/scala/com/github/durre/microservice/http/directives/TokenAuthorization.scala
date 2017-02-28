package com.github.durre.microservice.http.directives

import java.util.UUID

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.authenticateOAuth2
import akka.http.scaladsl.server.directives.Credentials
import com.auth0.jwt.{JWT, JWTVerifier}
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Claim
import com.github.durre.microservice.models.{OAuthScope, RequestInfo}

import scala.collection.JavaConverters._

trait TokenAuthorization {

  // The secret used to decode/encode JWT's
  def jwtSecret: String

  // Each service has it own set of scopes, which we know nothing about at this point
  protected def scopeFromString(str: String): Option[OAuthScope]

  // Re-usable verification of JWT's
  private lazy val jwtVerifier: JWTVerifier = JWT
    .require(Algorithm.HMAC256(jwtSecret))
    .build()

  /**
    * Verify and extract the provided jwt
    */
  def authorizeToken(realm: String): Directive1[RequestInfo] =
    authenticateOAuth2(realm, jwtAuthenticator)

  private def parseScopes(scopes: List[String]): Set[OAuthScope] =
    scopes
      .flatMap(scopeFromString)
      .toSet

  /**
    * Authenticate the client
    */
  private def jwtAuthenticator(credentials: Credentials): Option[RequestInfo] = credentials match {
    case p @ Credentials.Provided(id) => decodeToken(id)
    case _ => None
  }

  /**
    * Verifies that the jwt hasn't been tampered with
    */
  private def decodeToken(token: String): Option[RequestInfo] = {
    try {
      val jwt = jwtVerifier.verify(token)
      toRequestInfo(jwt.getClaims.asScala.toMap, token)
    } catch {
      case _: JWTVerificationException => None
    }
  }

  /**
    * Extract data
    */
  private def toRequestInfo(claims: Map[String, Claim], jwt: String): Option[RequestInfo] = {
    for {
      userId <- claims.get("userId").map(_.asString).map(UUID.fromString)
      orgId <- claims.get("orgId").map(_.asString).map(UUID.fromString)
      scopes <- claims.get("scopes").map(_.asList(classOf[String]).asScala.toList).map(parseScopes)
    } yield RequestInfo(userId, orgId, scopes, jwt)
  }
}
