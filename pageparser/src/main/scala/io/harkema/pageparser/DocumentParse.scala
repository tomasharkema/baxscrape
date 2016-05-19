package io.harkema.pageparser

import net.ruippeixotog.scalascraper.model.Element

trait ParseResult[ParseType] {
  def toOption: Option[ParseType]
  def get: ParseType
}

case class ParseSuccess[ParseType](value: ParseType) extends ParseResult[ParseType] {
  def toOption = Some(value)
  def get = value
}
case class ParseFailed[ParseType](errorMessage: String, element: String) extends ParseResult[ParseType] {
  println("Init ParseFailed")
  def toOption = None
  def get = throw new NoSuchElementException("ParseFailed().get for "+ element)
}

trait ParseReads[ParseType] {
  def reads(element: Element): ParseResult[ParseType]
}

object DocumentParse {
  implicit class DocumentParse(document: Element) {
    def as[ParseType](implicit parseReads: ParseReads[ParseType]): ParseType =
      parseReads.reads(document).get
    def validate[ParseType](implicit parseReads: ParseReads[ParseType]): ParseResult[ParseType] =
      parseReads.reads(document)
  }
}