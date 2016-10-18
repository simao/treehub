package com.advancedtelematic.data

import eu.timepit.refined.api.{Refined, Validate}
import org.genivi.sota.data.Namespace

object DataType {
  case class ValidCommit()

  type Commit = Refined[String, ValidCommit]

  implicit val validCommit: Validate.Plain[String, ValidCommit] =
    Validate.fromPredicate(
      hash => hash.length == 64 && hash.forall(h => ('0' to '9').contains(h) || ('a' to 'f').contains(h)),
      hash => s"$hash is not a sha-256 commit hash",
      ValidCommit()
    )

  case class Ref(namespace: Namespace, name: RefName, value: Commit, objectId: ObjectId)

  case class RefName(get: String) extends AnyVal

  case class ObjectId(get: String) extends AnyVal

  object ObjectId {
    def from(commit: Commit): ObjectId = ObjectId(commit.get + ".commit")
  }

  case class TObject(namespace: Namespace, id: ObjectId, blob: Array[Byte])
}