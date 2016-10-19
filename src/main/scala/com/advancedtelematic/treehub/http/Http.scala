package com.advancedtelematic.treehub.http

import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import akka.http.scaladsl.server.{Directives, _}
import org.genivi.sota.data.Namespace

object Http {
  import Directives._

  def extractNamespace(allowEmpty: Boolean = false): Directive1[Namespace] = {
    optionalHeaderValueByName("x-ats-namespace").flatMap {
      case Some(h) => provide(Namespace(h))
      case None =>
        authenticateBasic[Namespace]("treehub", {
          case Provided(c) => Some(Namespace(c))
          case Missing =>
            if(allowEmpty)
              Some(Namespace("default"))
            else
              None
        })
    }
  }
}
