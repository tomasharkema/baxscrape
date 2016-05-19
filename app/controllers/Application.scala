package controllers

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import scrape.{ScraperWebSocketClientActor, ScraperActor, ScraperService}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, _}

class Application @Inject() (implicit scraperService: ScraperService,
                             executionContext: ExecutionContext,
                             actorSystem: ActorSystem,
                             materializer: Materializer,
                             webJarAssets: WebJarAssets) extends Controller {

  implicit val duration: Duration = 1 minute

  def index = Action(Ok(views.html.index("")))

  def socket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => ScraperWebSocketClientActor.props(out, scraperService.scraperActor))
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Application.socket
      )
    ).as("text/javascript")
  }

}