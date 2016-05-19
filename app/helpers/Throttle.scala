package helpers

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{ExecutionContext, Future}


case class Throttle[RequestType, ResultType] private(perform: RequestType => Future[ResultType], maxJobs: Int = 5)
                                                    (implicit executionContext: ExecutionContext) {
  private val executedJobs = new AtomicInteger(0)

  private val waitingEC = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(Math.min(maxJobs*2, 20)))

  private def performJob(request: RequestType) = Future {

    while(executedJobs.get() > maxJobs) { Thread.sleep(100) }

    println(s"${executedJobs.hashCode()} READY WAITING")
  }(waitingEC).flatMap { _ =>
    println(s"${executedJobs.hashCode()} GOT ROOM. EXECUTING: ${executedJobs.incrementAndGet()}")
    perform(request) andThen {
      case _ =>
        println(s"${executedJobs.hashCode()} READY: ${executedJobs.decrementAndGet()}")
    }
  }

  def performAction(request: RequestType): Future[ResultType] = {
    performJob(request)
  }
}
