package se.durre.microservice.http

import java.util.UUID

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.Credentials
import com.typesafe.scalalogging.Logger
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.libs.json.Reads._
import play.api.libs.json._
import se.durre.microservice.http.models.RequestInfo
import se.durre.microservice.models.OAuthScope

import scala.util.Success


trait HttpService {

  private val internalSecretHeader = "X-Internal-Secret"
  def interCommunicationToken: Option[String]
  def jwtSecret: String
  def log: Logger


  protected def toRequestInfo(json: JsObject, jwt: String): Option[RequestInfo] = {
    for {
      userId <- (json \ "userId").asOpt[String].map(UUID.fromString)
      orgId <- (json \ "orgId").asOpt[String].map(UUID.fromString)
      scopes <- (json \ "scopes").asOpt[List[String]].map(parseScopes)
    } yield RequestInfo(userId, orgId, scopes, jwt)
  }

  private def parseScopes(scopes: List[String]): Set[OAuthScope] =
    scopes
      .flatMap(scopeFromString)
      .toSet


  private def decodeToken(token: String): Option[RequestInfo] = {
    JwtJson.decodeJson(token, jwtSecret, Seq(JwtAlgorithm.HS256)) match {
      case Success(json) => toRequestInfo(json, token)
      case _ => None
    }
  }

  private def jwtAuthenticator(credentials: Credentials): Option[RequestInfo] = credentials match {
    case p @ Credentials.Provided(id) => decodeToken(id)
    case _ =>
      log.error("Invalid credentials provided for internal communication")
      None

  }

  def authorize(required: Set[OAuthScope], orgId: Option[UUID] = None): Directive1[RequestInfo] =
    interCommunicationToken match {

      case Some(internalToken) =>
        optionalHeaderValueByName(internalSecretHeader).flatMap {
          case Some(secret) if secret == internalToken =>

            // Verify & extract the JWT into a RequestInfo object
            authenticateOAuth2("Microservice Realm", jwtAuthenticator).flatMap { req =>
              if (orgId.isDefined && !orgId.contains(req.orgId)) reject(AuthorizationFailedRejection)
              else if (!OAuthScope.hasScopes(required, req.scopes)) reject(AuthorizationFailedRejection)
              else provide(req)
            }

          case None => reject(AuthorizationFailedRejection)
        }

      // No internal secure token needed. Just verify the JWT.
      case None => authenticateOAuth2("Microservice Realm", jwtAuthenticator).flatMap { req =>
        if (orgId.isDefined && !orgId.contains(req.orgId)) reject(AuthorizationFailedRejection)
        else if (!OAuthScope.hasScopes(required, req.scopes)) reject(AuthorizationFailedRejection)
        else provide(req)
    }
  }

  /**
    * Each service has it own set of scopes, which we know nothing about at this point
    */
  protected def scopeFromString(str: String): Option[OAuthScope]

}
