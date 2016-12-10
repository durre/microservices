package se.durre.microservice.models

import org.scalatest.{Matchers, FlatSpec}

class OAuthScopeSpec extends FlatSpec with Matchers {

  case object OneScope extends OAuthScope  { val name: String = "onescope" }
  case object AnotherScope extends OAuthScope { val name: String = "anotherscope" }
  case object ThirdScope extends OAuthScope {
    override val inheritedScopes: Set[OAuthScope] = Set(AnotherScope)
    val name: String = "thirdscope"
  }

  "OAuthScope" should "require all scopes" in {
    OAuthScope.hasScopes(required = Set(OneScope, AnotherScope), actual = Set(OneScope)) shouldBe false
  }

  it should "grant access when user has all scopes" in {
    OAuthScope.hasScopes(required = Set(OneScope, AnotherScope), actual = Set(OneScope, AnotherScope)) shouldBe true
  }

  it should "get required scope by inheritance" in {
    OAuthScope.hasScopes(required = Set(OneScope, AnotherScope), actual = Set(OneScope, ThirdScope)) shouldBe true
  }
}
