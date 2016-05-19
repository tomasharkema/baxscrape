package helpers

import scala.concurrent.Future
import scala.util.{Failure, Success}

object FutureHelper {
  implicit class OptionToFuture[A](option: Option[A]) {
    implicit def toFuture: Future[A] = option.fold[Future[A]](Future.failed(new NoSuchElementException("None.get"))) (res =>
      Future.successful(res)
    )
  }
}
