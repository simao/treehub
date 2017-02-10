package com.advancedtelematic.treehub.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directives, _}
import akka.stream.{ActorMaterializer, Materializer}
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.Namespace
import org.genivi.sota.http.{NamespaceDirectives, TokenValidator}

import scala.concurrent.ExecutionContext

object Http {
  import Directives._

  lazy val extractNamespace = NamespaceDirectives.fromConfig()

  def deviceNamespace(deviceRegistry: DeviceRegistry)
                     (implicit ec: ExecutionContext): Directive1[Namespace] = {
    DeviceIdDirectives.extractFromToken.flatMap { deviceId =>
      onSuccess(deviceRegistry.fetchMyDevice(deviceId).map(_.namespace))
    }
  }

  // TODO: Should be Materializer instead of ActorMaterializer
  def tokenValidator(implicit s: ActorSystem, mat: ActorMaterializer): Directive0 = TokenValidator().fromConfig

  def transformAtsAuthHeader: Directive0 = mapRequest { req ⇒
    req.mapHeaders { headers ⇒
      val atsAuthHeader = headers.find(_.is("x-ats-authorization"))

      atsAuthHeader match {
        case Some(h) ⇒ headers :+ RawHeader("Authorization", h.value())
        case None ⇒ headers
      }
    }
  }
}
