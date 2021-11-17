package com.github.yandoroshenko.kinesisdemo.storage

import akka.stream.scaladsl.Sink
import com.github.yandoroshenko.kinesisdemo.model.Event

trait Storage[T] {
  def sink: Sink[Event[T], _]
}