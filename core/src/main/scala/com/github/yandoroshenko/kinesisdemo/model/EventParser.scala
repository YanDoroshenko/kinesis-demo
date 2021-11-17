package com.github.yandoroshenko.kinesisdemo.model

import scala.util.Try

object EventParser {
  def parse[T](s: String)(implicit parser: Parser[T]): Try[Event[T]] = {
    Try {
      s.split(",").map(_.trim).toList match {
        case timestamp :: eventType :: value :: Nil =>
          Event(timestamp.toLong, eventType, parser.parse(value))
      }
    }
  }
}

trait Parser[T] {
  def parse(s: String): T
}
