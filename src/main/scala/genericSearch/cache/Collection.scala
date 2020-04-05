package genericSearch.cache

import genericSearch.common.Index
import genericSearch.model.Data


/**
 * A collection holds the raw data in an array
 */
private[genericSearch] trait Collection {
  // use Array for super fast lookup by index - but prevent mutation
  protected val data: Array[Data]

  def foreach(f: Data => Unit): Unit = data.toIterable.foreach(f)

  def zipWithIndex: Iterable[(Data, Index)] = data.toIterable.zipWithIndex.map { case (e, i) => (e, new Index(i)) }

  lazy val size: Int = data.length

  def get(index: Index): Option[Data] =
    if (index.value >= 0 && index.value < size) Some(data.apply(index.value))
    else None
}


private[genericSearch] object Collection {

  def apply(elements: Iterable[Data]): Collection = {

    new Collection {
      // ensure array is not accessible from outside
      override protected val data: Array[Data] = elements.toArray
    }
  }

}


