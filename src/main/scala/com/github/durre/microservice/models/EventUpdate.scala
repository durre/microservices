package com.github.durre.microservice.models

import java.util.UUID

import spray.json.JsValue

case class EventUpdate(
  orgId: UUID,
  requiredScopes: Set[OAuthScope],
  payload: JsValue
)
