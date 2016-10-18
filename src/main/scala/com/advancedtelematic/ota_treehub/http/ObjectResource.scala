package com.advancedtelematic.ota_treehub.http

import akka.http.scaladsl.server.{Directive1, PathMatcher1}
import akka.stream.Materializer
import com.advancedtelematic.data.DataType.{ObjectId, TObject}
import com.advancedtelematic.ota_treehub.db.ObjectRepositorySupport
import org.genivi.sota.data.Namespace
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext


class ObjectResource(namespace: Directive1[Namespace])
                    (implicit db: Database, ec: ExecutionContext, mat: Materializer) extends ObjectRepositorySupport {
  import akka.http.scaladsl.server.Directives._

  val PrefixedObjectId: PathMatcher1[ObjectId] = (Segment / Segment).tmap { case (oprefix, osuffix) =>
    Tuple1(ObjectId(oprefix + osuffix))
  }

  val route = namespace { ns =>
    path("objects" / PrefixedObjectId) { objectId =>
      get {
        // TODO: Access control to ns
        val dbIO = objectRepository.findBlob(objectId)
        val f = db.run(dbIO)
        complete(f)
      } ~
        post {
          fileUpload("file") { case (_, content) =>
            val f = for {
              (digest, content) <- ObjectUpload.readFile(content)
              _ <- db.run(objectRepository.create(TObject(ns, objectId, content.toArray)))
            } yield digest

            complete(f)
          }
        }
    }
  }
}
