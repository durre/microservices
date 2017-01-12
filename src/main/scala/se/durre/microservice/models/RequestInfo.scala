package se.durre.microservice.models

import java.util.UUID

/**
  * The JWT claims easily accessible
  */
case class RequestInfo(
  userId: UUID,
  orgId: UUID,
  scopes: Set[OAuthScope],
  jwt: String
)
