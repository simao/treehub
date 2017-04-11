package com.advancedtelematic.data

import java.nio.file.{Path, Paths}
import java.util.Base64

import com.advancedtelematic.common.DigestCalculator
import com.advancedtelematic.libats.data.Namespace
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import cats.syntax.either._
import com.advancedtelematic.libats.messaging_datatype.DataType.{Commit, ValidCommit}
import org.apache.commons.codec.binary.Hex

import scala.util.Try

object DataType {
  import com.advancedtelematic.libats.data.ValidationUtils._

  case class Ref(namespace: Namespace, name: RefName, value: Commit, objectId: ObjectId)

  case class RefName(get: String) extends AnyVal

  case class ValidObjectId()

  implicit val validObjectId: Validate.Plain[String, ValidObjectId] =
    Validate.fromPredicate(
      objectId => {
        val (sha, objectType) = objectId.splitAt(objectId.indexOf('.'))
        validHex(64, sha) && objectType.nonEmpty
      },
      objectId => s"$objectId must be in format <sha256>.objectType",
      ValidObjectId()
    )

  type ObjectId = Refined[String, ValidObjectId]

  implicit class ObjectIdOps(value: ObjectId) {
    def path(parent: Path): Path = {
      val (prefix, rest) = value.get.splitAt(2)
      Paths.get(parent.toString, prefix, rest)
    }

    def filename: Path = path(Paths.get("/")).getFileName
  }

  object ObjectId {
    def from(commit: Commit): ObjectId = ObjectId.parse(commit.get + ".commit").right.get

    def parse(string: String): Either[String, ObjectId] = refineV[ValidObjectId](string)
  }

  case class TObject(namespace: Namespace, id: ObjectId, byteSize: Long)

  protected def toBase64(value: String): Try[Array[Byte]] = {
    Try(Base64.getDecoder.decode(value.replace("/", "").replace("_", "/")))
  }

  case class ValidDeltaId()
  type DeltaId = Refined[String, ValidDeltaId]



  implicit val validDeltaId: Validate.Plain[String, ValidDeltaId] =
    Validate.fromPredicate(
      v => {
        val parts = v.split("-")
        parts.length == 2 && toBase64(parts.head).isSuccess && toBase64(parts.last).isSuccess
      },
      v => s"$v is not a valid DeltaId (cc/mbase64(from.rest)-mbase64(to))",
      ValidDeltaId()
    )

  implicit class DeltaIdOps(value: DeltaId) {
    def asObjectId: Either[Throwable, ObjectId] = for {
      toStr <- Either.catchNonFatal(value.get.split("-").last)
      toBytes <- Either.fromTry(toBase64(toStr))
      toCommit <- Either.catchNonFatal(Hex.encodeHexString(toBytes))
      objectId <- ObjectId.parse(s"$toCommit.commit").leftMap(s => new IllegalArgumentException(s))
    } yield objectId
  }

  object Commit {
    def from(bytes: Array[Byte]): Either[String, Commit] =
      refineV[ValidCommit](DigestCalculator.byteDigest()(bytes))
  }
}

object Codecs {
  import io.circe.{Encoder, Decoder}
  import DataType._

  implicit val refNameEncoder: Encoder[RefName] = Encoder[String].contramap(_.get)
  implicit val refNameDecoder: Decoder[RefName] = Decoder[String].map(RefName.apply)
}
