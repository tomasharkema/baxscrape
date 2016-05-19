package scrape

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.netaporter.uri._
import com.netaporter.uri.dsl._
import helpers.Throttle
import io.harkema.pageparser.DocumentParse._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import play.api.libs.ws.WSClient
import scrape.models.CategoryConverters._
import scrape.models.Converters._
import scrape.models.HomePage
import scrape.models.ProductDetailConverters._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

case class ScrapeRequest(url: Uri)

object ScraperActor {
  def props(implicit scraperService: ScraperService) = Props(new ScraperActor())
}

case object Join
case object Leave
final case class ClientSentMessage(text: String)

trait SocketMessage {
  val text: String

  def toClient = ToClient(this)
}
final case class ToClient(msg: SocketMessage)
final case class ScraperMessage(text: String) extends SocketMessage
final case class IsScraping(isScraping: Boolean) extends SocketMessage {
  val text = s"isScraping:$isScraping"
}
final case class Exception(ex: Throwable) extends SocketMessage {
  val text = ex.toString
}

class ScraperActor(implicit scraperService: ScraperService) extends Actor {
//  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val duration = Duration.Inf

  private val isScraping = new AtomicBoolean(false)

  def receive = process(Set.empty)

  def process(subscribers: Set[ActorRef]): Receive = {
    case Join =>
      println("LISTENER JOINED")
      context become process(subscribers + sender)
      sender ! IsScraping(isScraping.get).toClient

    case Leave =>
      println("LISTENER LEFT")
      context become process(subscribers - sender)

    case msg: ClientSentMessage =>
      println(s"Got Message $msg")
      handleMessage(subscribers, msg.text)

    case msg: ToClient =>
      (subscribers - sender).foreach(_ ! msg)

    case a =>
      println(s"UNHANDLED PROCESS $a")
  }

  def notify(subscribers: Set[ActorRef], msg: ToClient) =
    subscribers.foreach(_ ! msg)

  def handleMessage(subscribers: Set[ActorRef], msg: String) = msg match {
    case "start" =>
      println("START SCRAPING")

      if (!isScraping.getAndSet(true)) {

        scraperService.scrape.map {
          case (homePage, bstock, offerProucts, popularDetails) =>
            Future.sequence(bstock.products.map(_.map(_.detail)))
        } onFailure {
          case ex =>
            notify(subscribers, Exception(ex).toClient)
        }

        notify(subscribers, IsScraping(isScraping.get).toClient)
      }
  }
}

object ScraperWebSocketClientActor {
  def props(out: ActorRef, chat: ActorRef) = Props(new ScraperWebSocketClientActor(out, chat))
}

class ScraperWebSocketClientActor(out: ActorRef, chat: ActorRef) extends Actor {

  override def preStart() = chat ! Join

  override def postStop() = chat ! Leave

  def receive = {
    case text: String =>
      println(s"<== toScraperService: $text")
      chat ! ClientSentMessage(text)

    case ToClient(message) =>
      println(s"==> toClient: ${message.text}")
      out ! message.text
  }
}

class ScraperService @Inject()(ws: WSClient,
                               actorSystem: ActorSystem)(implicit context: ExecutionContext) {

  implicit val self = this
  implicit val duration = Duration.Inf

  private val browser = JsoupBrowser()

  private val _scraperActor = actorSystem.actorOf(ScraperActor.props(this), "scraperActor")
  def scraperActor = _scraperActor

  val host: Uri = "https://bax-shop.nl"

  private def getPageImpl(request: ScrapeRequest): Future[Document] = {
    val url = request.url

    scraperActor ! ScraperMessage(s"Request: $url").toClient

    ws.url(request.url).get().map { res =>
      scraperActor ! ScraperMessage(s"Result: $url").toClient
      browser.parseString(res.body)
    }
  }

  private val throttle = Throttle(getPageImpl, 5)

  def getPage(request: ScrapeRequest) = throttle.performAction(request)

  def getHome = getPage(ScrapeRequest(host)).map(_.root.as[HomePage])

  def scrape = for {
    home <- getHome
    bstock <- Future(home.bstock.detail).flatMap { a => a }
    offerProducts <- Future(home.offers.detail).flatMap { a => a }
    popularDetails <- Future.sequence(home.popular.map(_.detail).toList)
//    categories <- Future.sequence(home.categories.map(_.detail))
  } yield (home, bstock, offerProducts, popularDetails)//, categories)
}
