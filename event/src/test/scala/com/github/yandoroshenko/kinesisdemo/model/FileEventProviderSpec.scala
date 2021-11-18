package com.github.yandoroshenko.kinesisdemo.model

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import com.github.yandoroshenko.kinesisdemo.event.FileEventProvider
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.math.BigDecimal
import scala.util.{Failure, Success, Try}

class FileEventProviderSpec extends AsyncWordSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(actorSystem)

  "File event provider" should {
    "correctly parse example file" in {
      new FileEventProvider("event/src/test/resources/data.data")
        .provideEvents()
        .toMat(Sink.seq[Try[Event[BigDecimal]]])(Keep.right)
        .run()
        .map(_.toList)
        .map {
          case Failure(_) ::
                Success(Event(1567365890, "mars", x)) ::
                Success(Event(1567365890, "neptune", y)) ::
                Failure(_) ::
                Success(Event(1567365890, "mars", z)) ::
                Failure(_) ::
                Nil if x == 65.944 && y == 5 && z == 58.387 =>
            succeed
        }
    }
  }
}
