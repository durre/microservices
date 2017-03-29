package com.github.durre.microservice.http.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.BasicDirectives.provide

import scala.concurrent.Future

trait ResourceFinder {

  def resourceFinder[T](futureResource: Future[Option[T]]): Directive1[T] =
    onSuccess(futureResource).flatMap {
      case Some(resource) => provide(resource)
      case None => complete((StatusCodes.NotFound, "Resource not found"))
    }
}
