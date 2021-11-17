package com.github.yandoroshenko.kinesisdemo

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.github.yandoroshenko.kinesisdemo.event.{EventTransformer, FileEventSource}

import scala.concurrent.ExecutionContext

object Main {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    implicit val mat: Materializer = Materializer(actorSystem)

    val eventSource = new FileEventSource("/home/yan/git/github/kinesis-demo/main/src/main/resources/resident-samples.data")

    HttpApi.server { (eventType, from, to) =>
      EventTransformer.averageForInterval(eventType, from, to)(eventSource.provideEvents()).run().map { average =>
        AverageResponse(eventType, average.value, average.processedCount)
      }
    }(HttpConfig("localhost", 9001))
  }
}
