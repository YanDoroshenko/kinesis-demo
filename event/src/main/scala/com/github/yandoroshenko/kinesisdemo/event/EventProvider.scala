package com.github.yandoroshenko.kinesisdemo.event

import akka.stream.scaladsl.Source
import com.github.yandoroshenko.kinesisdemo.model.Event

import scala.util.Try

trait EventProvider[T] {
  def provideEvents(): Source[Try[Event[T]], _]
}
