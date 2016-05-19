package helpers

trait OptionIterator[A] extends Iterator[A] {

  var cachedValue: Option[A] = None

  override def hasNext: Boolean = {
    nextOption() match {
      case Some(v) =>
        cachedValue = Some(v)
        true

      case None =>
        false
    }
  }

  override def next(): A = cachedValue.get

  def nextOption(): Option[A]
}
