package com.github.yandoroshenko.kinesisdemo.event

import akka.stream.scaladsl.{Flow, Source}
import com.github.yandoroshenko.kinesisdemo.model.implicits._
import com.github.yandoroshenko.kinesisdemo.model.{Event, EventParser}

import scala.util.Try

class FileEventSource(fileName: String) extends EventSource[BigDecimal] {

  override def provideEvents(): Source[Try[Event[BigDecimal]], _] = {
    Source
      .fromIterator { () =>
        val source =
          scala.io.Source.fromFile(fileName)
        source.getLines()
      }
      .via(Flow.fromFunction(EventParser.parse[BigDecimal]))
  }
}
