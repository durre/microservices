package se.durre.microservice.http.models

import java.util.UUID

import se.durre.microservice.models.OAuthScope

/**
  * The JWT claims easily accessible
  */
case class RequestInfo(
  userId: UUID,
  orgId: UUID,
  scopes: Set[OAuthScope],
  jwt: String
)
