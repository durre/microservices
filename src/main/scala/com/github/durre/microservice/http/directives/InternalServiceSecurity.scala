package com.github.durre.microservice.http.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.BasicDirectives.pass

/**
  * In the best of worlds you might have one single public facing api. An api gateway.
  * That might not be the case, for instance if you're using Heroku without "private spaces".
  *
  * This directive offers a poor mans extra layer of security.
  */
trait InternalServiceSecurity {

  /**
    * Make sure the client supplied the internal token the services use to communicate
    */
  def verifyServiceToken(token: String, tokenHeader: String): Directive0 =
    optionalHeaderValueByName(tokenHeader).flatMap {

      case Some(secret) if secret == token => pass

      // Might actually be better to return a 404 here?
      case _ => complete((StatusCodes.Forbidden, "Missing service token"))
    }
}
