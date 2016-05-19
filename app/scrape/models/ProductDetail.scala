package scrape.models

import io.harkema.pageparser.{ParseFailed, ParseReads, ParseResult, ParseSuccess}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import scrape.models.ContentExtractorHelpers._

case class ProductDetail(title: String,
                         price: Priceable) extends BaxProductDetail

object ProductDetailConverters {
  implicit val productDetailParseReads = new ParseReads[ProductDetail] {
    def reads(e: Element) = {
      val elements = for {
        title   <- e >?> text("h1.pageTitle")
        price   <- e >?> price("span[itemprop=price]")
      } yield (title, price)

      elements.fold[ParseResult[ProductDetail]](ParseFailed(e.toString, "ProductDetail")) (el =>
        ParseSuccess(ProductDetail.tupled apply el)
      )
    }
  }
}
