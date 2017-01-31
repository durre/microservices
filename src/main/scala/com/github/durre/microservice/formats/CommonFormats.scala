package com.github.durre.microservice.formats

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID

import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

trait CommonFormats {

  /**
    * UUID <-> JSON
    */
  implicit object UUIDJsonFormat extends JsonFormat[UUID] {
    override def read(json: JsValue): UUID = json match {
      case JsString(uuidString) => try {
        UUID.fromString(uuidString)
      } catch {
        case _: Exception => deserializationError(s"Invalid UUID: $json")
      }
      case _ => deserializationError(s"Invalid UUID: $json")
    }

    override def write(obj: UUID): JsValue = JsString(obj.toString)
  }

  /**
    * LocalDate <-> JSON
    */
  implicit object LocalDateJsonFormat extends JsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = json match {
      case JsString(dateString) => try {
        LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
      } catch {
        case _: Exception => deserializationError(s"Invalid LocalDate: $json")
      }
      case _ => deserializationError(s"Invalid LocalDate: $json")
    }

    override def write(obj: LocalDate): JsValue = JsString(DateTimeFormatter.ISO_DATE.format(obj))
  }

  /**
    * LocalDateTime <-> JSON
    */
  implicit object LocalDateTimeJsonFormat extends JsonFormat[LocalDateTime] {
    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(dateString) => try {
        LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
      } catch {
        case _: Exception => deserializationError(s"Invalid LocalDateTime: $json")
      }
      case _ => deserializationError(s"Invalid LocalDateTime: $json")
    }

    override def write(obj: LocalDateTime): JsValue = JsString(DateTimeFormatter.ISO_DATE_TIME.format(obj))
  }
}
