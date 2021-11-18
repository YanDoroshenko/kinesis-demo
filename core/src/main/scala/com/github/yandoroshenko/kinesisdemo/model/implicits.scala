package com.github.yandoroshenko.kinesisdemo.model

object implicits {
  implicit val bigDecimalParser: Parser[BigDecimal] = BigDecimal.apply
}
