package com.github.durre.microservice.models

import java.time.{ZoneId, ZonedDateTime}
import java.util.{Date, UUID}

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

case class Token(token: String)

object Token {

  private val utc = ZoneId.of("UTC")

  def generate(userId: UUID, orgId: UUID, scopes: Set[OAuthScope], jwtSecret: String, validDays: Int): Token = {
    Token(
      JWT.create()
        .withIssuedAt(Date.from(ZonedDateTime.now(utc).toInstant))
        .withExpiresAt(Date.from(ZonedDateTime.now(utc).plusDays(validDays).toInstant))
        .withClaim("userId", userId.toString)
        .withClaim("orgId", orgId.toString)
        .withArrayClaim("scopes", scopes.map(_.name).toArray)
        .sign(Algorithm.HMAC256(jwtSecret))
    )
  }

}
