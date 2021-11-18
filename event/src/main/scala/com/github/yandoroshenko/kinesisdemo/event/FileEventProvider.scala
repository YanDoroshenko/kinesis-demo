package com.github.yandoroshenko.kinesisdemo.event

import akka.stream.scaladsl.{Flow, Source}
import com.github.yandoroshenko.kinesisdemo.model.{Event, EventParser}
import com.github.yandoroshenko.kinesisdemo.model.implicits.bigDecimalParser
import com.typesafe.scalalogging.Logger

import scala.util.Try

class FileEventProvider(fileName: String) extends EventProvider[BigDecimal] {

  private val log = Logger(getClass)

  override def provideEvents(): Source[Try[Event[BigDecimal]], _] = {
    log.info("Provide events")

    Source
      .fromIterator { () =>
        val source =
          scala.io.Source.fromFile(fileName)
        source.getLines()
      }
      .via(Flow.fromFunction(EventParser.parse[BigDecimal]))
  }
}
