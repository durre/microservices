package se.durre.microservice.models

trait OAuthScope {

  // Lets say you have the scope user:write, then maybe you should also have user:read as well?
  def inheritedScopes: Set[OAuthScope] = Set()

  def name: String
}

object OAuthScope {

  def hasScopes(required: Set[OAuthScope], actual: Set[OAuthScope]): Boolean = {
    val allExisting = actual.flatMap(scope => scope.inheritedScopes + scope)
    val allRequired = required.flatMap(scope => scope.inheritedScopes + scope)
    allRequired.subsetOf(allExisting)
  }
}
