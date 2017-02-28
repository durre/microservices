package com.github.durre.microservice.http.responses

import akka.http.scaladsl.model._
import akka.util.ByteString

object ApiResponses {

  private val contentType = ContentType(MediaTypes.`application/json`)

  private def toHttpResponse(error: ErrorResponse): HttpResponse = HttpResponse(
    error.statusCode,
    entity = HttpEntity(contentType, ByteString(ErrorResponse.errorWrite.write(error).toString()))
  )

  def error(
    msg: String,
    errorCode: String,
    statusCode: Int = StatusCodes.BadRequest.intValue,
    pointer: Option[String] = None): HttpResponse = {

    val error = ErrorResponse(
      statusCode = statusCode,
      errors = Seq(ApiError(errorCode = errorCode, message = msg, pointer = pointer))
    )

    toHttpResponse(error)
  }

  def errors(errors: Seq[ApiError], statusCode: Int = StatusCodes.BadRequest.intValue): HttpResponse = {

    val error = ErrorResponse(
      statusCode = statusCode,
      errors = errors
    )

    toHttpResponse(error)
  }
}
