package com.github.durre.microservice.client

import org.scalatest.{FunSuite, Matchers}

class ServiceClientSpec extends FunSuite with Matchers {

  test("extract correct url parts") {
    ServiceClient.extractUrlParts("https://localhost:9000") shouldBe (true, "localhost", Some(9000))
    ServiceClient.extractUrlParts("http://localhost:9000") shouldBe (false, "localhost", Some(9000))
    ServiceClient.extractUrlParts("https://test.herokuapp.com") shouldBe (true, "test.herokuapp.com", None)
    ServiceClient.extractUrlParts("http://test.herokuapp.com") shouldBe (false, "test.herokuapp.com", None)
  }
}
