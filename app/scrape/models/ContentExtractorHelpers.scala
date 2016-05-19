package scrape.models

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import net.ruippeixotog.scalascraper.model.ElementQuery
import scrape.models.Price._

object ContentExtractorHelpers {
  val href: ElementQuery => Uri = res => res.head.attr("href")
  val price: ElementQuery => Priceable = _.head.text.stringToPrice.get
}
