package se.durre.microservice.models

import java.util.UUID

case class RequestInfo(
  userId: UUID,
  orgId: UUID,
  scopes: Set[OAuthScope],
  jwt: String
)
