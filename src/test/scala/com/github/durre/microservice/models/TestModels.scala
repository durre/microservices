package com.github.durre.microservice.models

import java.util.UUID

object TestModels {

  case object MyScope extends OAuthScope { val name: String = "myscope" }
  case object AnotherScope extends OAuthScope { val name: String = "anotherscope" }

  def scopeFromString(str: String): Option[OAuthScope] = str match {
    case "myscope" => Some(MyScope)
    case "another" => Some(AnotherScope)
    case _ => None
  }

  val requestInfo = RequestInfo(
    userId = UUID.randomUUID(),
    orgId = UUID.randomUUID(),
    scopes = Set(MyScope),
    jwt = "token"
  )
}
