package scrape.models

import com.netaporter.uri.Uri
import io.harkema.pageparser.DocumentParse._
import io.harkema.pageparser.{ParseFailed, ParseReads, ParseResult, ParseSuccess}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element => _, elementList => _, text => _}
import scrape.models.CategoryConverters._
import scrape.models.ContentExtractorHelpers._

case class PopularProduct(title: String,
                          detailUrl: Uri,
                          price: Priceable,
                          previousPrice: Priceable) extends BaxProductSmall[ProductDetail]

case class HomePage(popular: Iterable[PopularProduct],
                    categories: Iterable[Category],
                    bstock: Category,
                    offers: Category)

object Converters {

  implicit val popularElementReads = new ParseReads[PopularProduct] {
    def reads(e: Element) = {
      val elements = for {
        productName     <- e >?> text(".qvproductname")
        productUrl      <- e >?> href("a.trackable-product")
      } yield (productName, productUrl, (e >?> price(".voor-prijs")).getOrElse(NoPrice), (e >?> price(".voor-prijs")).getOrElse(NoPrice))

      elements.fold[ParseResult[PopularProduct]](ParseFailed(e.toString, "PopularProduct")) (el =>
        ParseSuccess(PopularProduct.tupled apply el)
      )
    }
  }

  implicit val homePageParseReads = new ParseReads[HomePage] {
    def reads(e: Element) = {
      val requiredElements = for {
        popular           <- e >?> element("#populair")
        popularElements   <- Option(popular.children.map(_.as[PopularProduct]))
        sections          <- e >?> elementList("nav.catmenu")
        categoryElements  <- Option(sections.head.children.head.children.map(_.as[Category]))
        bstock            <- sections(1).children.head.children.head.map(_.as[Category]).headOption
        offers            <- sections(1).children.head.children.slice(1, 3).tail.map(_.as[Category]).headOption
      } yield (popularElements, categoryElements, bstock, offers)

      requiredElements.fold[ParseResult[HomePage]](ParseFailed(e.toString, "HomePage")) (el =>
        ParseSuccess(HomePage.tupled apply el)
      )
    }
  }
}