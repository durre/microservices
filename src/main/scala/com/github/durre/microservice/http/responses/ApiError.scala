package com.github.durre.microservice.http.responses

import spray.json.{JsNull, JsObject, JsString, RootJsonWriter}

case class ApiError(
  errorCode: String,
  message: String,
  pointer: Option[String] = None // None means global error
)

object ApiError {

  def apply(errorCode: String, message: String, pointer: String) =
    new ApiError(errorCode, message, Some(pointer))

  val errorWrite: RootJsonWriter[ApiError] = new RootJsonWriter[ApiError] {
    override def write(e: ApiError) = JsObject(
      "errorCode" -> JsString(e.errorCode),
      "message" -> JsString(e.message),
      "pointer" -> e.pointer.map(JsString(_)).getOrElse(JsNull)
    )
  }
}
