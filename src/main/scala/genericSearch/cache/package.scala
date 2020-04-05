package genericSearch

import genericSearch.common.FieldValue
import genericSearch.model.Entity

package object cache {
  sealed trait CacheBuildError[T <: Entity[T]]
  case class DuplicatedPk[T <: Entity[T]](entity: T, value: FieldValue) extends CacheBuildError[T]
}
