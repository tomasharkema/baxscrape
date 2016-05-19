package scrape.models

import java.util.concurrent.atomic.AtomicBoolean

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import helpers.{FirstPage, NextPage, OptionIterator, PaginationAsyncIterator}
import io.harkema.pageparser.DocumentParse._
import io.harkema.pageparser.{ParseFailed, ParseReads, ParseResult, ParseSuccess}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
import scrape.{ScrapeRequest, ScraperService}
import scrape.models.ContentExtractorHelpers._
import helpers.FutureHelper._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

case class CategoryProduct(title: String,
                           detailUrl: Uri,
                           price: Priceable,
                           previousPrice: Option[Priceable]) extends BaxProductSmall[ProductDetail]

case class CategoryDetail private(name: String, productCount: Int, e: Element) {
  def products(implicit scraperService: ScraperService,
               executionContext: ExecutionContext,
               duration: Duration) =

    new PaginationAsyncIterator[Element, Element](e, { page =>
      page >?> href(".next") match {
        case Some(url) =>
          scraperService.getPage(ScrapeRequest(scraperService.host / url)).map(doc => Some(doc.root))

        case None =>
          Future.successful(None)
      }
    }, { page =>
      (page >?> element(".product-results")).map(_.children.iterator)
    }).map(_.map(_.as[CategoryProduct](CategoryConverters.categoryProductParseReads)))

  override def toString: String = s"CategoryDetail(name: $name, productCount: $productCount)"
}

case class Category(title: String,
                    detailUrl: Uri) extends DetailPageable[CategoryDetail]

object CategoryConverters {
  implicit val categoryParseReads = new ParseReads[Category] {
    def reads(e: Element) = {
      val elements = for {
        title   <- e >?> text("a")
        url     <- e >?> href("a")
      } yield (title, url)

      elements.fold[ParseResult[Category]](ParseFailed(e.toString, "Category")) (el =>
        ParseSuccess(Category.tupled apply el)
      )
    }
  }

  implicit val categoryProductParseReads = new ParseReads[CategoryProduct] {
    def reads(e: Element): ParseResult[CategoryProduct] = {
      val elements = for {
        productName     <- e >?> text("a.trackable-product")
        productUrl      <- e >?> href("a.trackable-product")
      } yield (productName, productUrl, (e >?> price(".voor-prijs")).getOrElse(NoPrice), e >?> price(".van-prijs"))

      elements.fold[ParseResult[CategoryProduct]](ParseFailed(e.toString, "CategoryProduct")) (el =>
        ParseSuccess(CategoryProduct.tupled apply el)
      )
    }
  }

  implicit val categoryDetailParseReads = new ParseReads[CategoryDetail] {
    def reads(e: Element): ParseResult[CategoryDetail] = {
      val elements = for {
        name <- e >?> text("h1.pageTitle")
        productCountElement <- e >?> element(".displayproductcount")
        productCount <- productCountElement >?> text("strong")
      } yield (name, productCount)

      elements.fold[ParseResult[CategoryDetail]](ParseFailed(e.toString, "CategoryDetail")) { case (name, productCount) =>
        ParseSuccess(CategoryDetail(name, productCount.toInt, e))
      }
    }
  }
}