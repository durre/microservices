package se.durre.microservice.client

import spray.json.{JsArray, JsObject}

case class ClientResponse(
  statusCode: Int,
  body: ResponseBody
)

sealed trait ResponseBody

object ResponseBody {
  case object Empty extends ResponseBody
  case class JsonObject(json: JsObject) extends ResponseBody
  case class JsonArray(json: JsArray) extends ResponseBody
  case class Text(value: String) extends ResponseBody
}

case class ClientError(msg: String)
