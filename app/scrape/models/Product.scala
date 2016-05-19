package scrape.models

import com.netaporter.uri.Uri
import scrape.{ScrapeRequest, ScraperService}
import io.harkema.pageparser.DocumentParse._

import scala.concurrent.{ExecutionContext, Future}
import ProductDetailConverters._
import io.harkema.pageparser.ParseReads
import helpers.FutureHelper._

trait Currency {
  val symbol: String
  val abbr: String
}
case object EUR extends Currency {
  val symbol = "€"
  val abbr = "EUR"
}

trait Priceable {}

case object NoPrice extends Priceable

trait Price extends Priceable {
  val amount: Int
  val currency: Currency
}

case class FlatPrice(tens: Int, currency: Currency) extends Price {
  val amount = tens * 100
}
case class CentPrice(tens: Int, cents: Int, currency: Currency) extends Price {
  val amount = tens * 100 + cents

  def round: FlatPrice = FlatPrice(tens + (if (cents >= 50) 1 else 0), currency)
}

object Price {
  def apply(string: String): Option[Price] = {
    val matches = "([0-9]{1,})[,|.](-|[0-9]{2})".r findFirstMatchIn string

    matches.fold[Option[Price]](None) { mtch =>

      if (mtch.groupCount != 2)
        None
      else
        (mtch.group(1), mtch.group(2)) match {
          case (tens, "-") =>
            Some(FlatPrice(tens.toInt, EUR))

          case (tens, cents) =>
            Some(CentPrice(tens.toInt, cents.toInt, EUR))

          case _ =>
            None
        }
    }
  }

  implicit class PriceConvert(string: String) {
    implicit def stringToPrice: Option[Priceable] = Price(string)
  }
}


trait DetailPageable[DetailType] {
  val detailUrl: Uri

  def detail(implicit scraperService: ScraperService,
             executionContext: ExecutionContext,
             parseReads: ParseReads[DetailType]) =
    scraperService.getPage(ScrapeRequest(detailUrl))
      .map(res => res.root.as[DetailType])
}

trait BaxProduct {
  val title: String
  val price: Priceable
}

trait BaxProductSmall[DetailType] extends BaxProduct with DetailPageable[DetailType] {}

trait BaxProductDetail extends BaxProduct {}