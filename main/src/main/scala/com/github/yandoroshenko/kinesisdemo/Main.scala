package com.github.yandoroshenko.kinesisdemo

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.github.yandoroshenko.kinesisdemo.event.FileEventProvider
import com.github.yandoroshenko.kinesisdemo.storage.{MongoConfig, MongoDBStorage}
import com.github.yandoroshenko.service.DefaultService

import scala.concurrent.ExecutionContext

object Main {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    implicit val mat: Materializer = Materializer(actorSystem)

    val eventSource = new FileEventProvider("main/src/main/resources/data.data")
    val storage = new MongoDBStorage[BigDecimal](MongoConfig("mongodb://127.0.0.1:27017", "kinesisdemo", "events"))
    val service = new DefaultService(eventSource, storage)

    storage.source.to(Sink.foreach(println))

    HttpApi.server { (eventType, from, to) =>
      service.processEvents(eventType, from, to).run() .map { average =>
        AverageResponse(eventType, average.value, average.processedCount)
      }
    }(HttpConfig("localhost", 9001))
  }
}
