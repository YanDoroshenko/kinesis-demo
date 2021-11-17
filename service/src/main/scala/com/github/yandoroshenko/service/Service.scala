package com.github.yandoroshenko.service

import akka.stream.scaladsl.RunnableGraph

trait Service {
  def processEvents(eventType: String, from: Long, to: Long): RunnableGraph[_]
}
