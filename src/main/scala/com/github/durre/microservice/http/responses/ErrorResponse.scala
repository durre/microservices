package com.github.durre.microservice.http.responses

import spray.json.{JsArray, JsNumber, JsObject, RootJsonWriter}

case class ErrorResponse(
  statusCode: Int,
  errors: Seq[ApiError]
)

object ErrorResponse {

  val errorWrite: RootJsonWriter[ErrorResponse] = new RootJsonWriter[ErrorResponse] {
    override def write(e: ErrorResponse) = JsObject(
      "error" -> JsObject(
        "statusCode" -> JsNumber(e.statusCode),
        "errors" -> JsArray(e.errors.map(ApiError.errorWrite.write).toVector)
      )
    )
  }
}
