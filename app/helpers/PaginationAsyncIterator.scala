package helpers

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

sealed trait Page[+PageType]
case object FirstPage extends Page[Nothing]
case class NextPage[+PageType](page: PageType) extends Page[PageType]

class PaginationAsyncIterator[PageType, ElementType](firstPage: PageType,
                                                     pageProvider: PageType => Future[Option[PageType]],
                                                     elementResolver: PageType => Option[Iterator[ElementType]])
                                                    (implicit executionContext: ExecutionContext,
                                                     duration: Duration)
  extends Iterator[Future[ElementType]] {

  private val lock = new Object
  private var currentPage = firstPage
  private var currentElement = elementResolver(firstPage)

  private def _hasNext: Boolean =
    if (currentElement.get.hasNext)
      true
    else
      Await.result(pageProvider(currentPage), duration) match {
        case Some(page) =>
          currentPage = page

          elementResolver(currentPage) match {
            case Some(itr) =>
              currentElement = Some(itr)
              _hasNext

            case _ =>
              false
          }

        case _ =>
          false
      }

  override def hasNext: Boolean = lock.synchronized(_hasNext)

  override def next(): Future[ElementType] = lock.synchronized(Future.successful(currentElement.get.next()))
}