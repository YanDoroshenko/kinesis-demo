package com.github.yandoroshenko.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.github.yandoroshenko.kinesisdemo.model.Event
import com.github.yandoroshenko.kinesisdemo.storage.Storage
import com.github.yandoroshenko.service.DefaultService.{Average, Sum}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.util.{Random, Success}

// Note: There's no support for property-based async testing
class DefaultServiceSpec extends AsyncWordSpec with Matchers {

  implicit val actorSystem: ActorSystem = ActorSystem()

  def event(): Event[BigDecimal] = Event(Random.nextLong(), Random.nextString(100), BigDecimal(Random.nextDouble()))

  def eventList(): Seq[Event[BigDecimal]] = {
    (0 to Random.nextInt(100)).map(_ => event())
  }

  "Default service" should {
    "calculate sum correctly" in {
      val events = eventList()

      val source = Source(events)
      val service = new DefaultService(() => Source.empty, new Storage[BigDecimal] {
        override val sink: Sink[Event[BigDecimal], _] = Sink.ignore
      })

      source.runWith(Sink.seq).flatMap { es =>
        val sum = es.map(_.value).reduceOption(_ + _)
        val count = es.size
        source.runWith(service.sum).map {
          _ shouldEqual Sum(sum, count)
        }
      }
    }

    "calculate average correctly" in {
      val events = eventList()
      val source = Source(events)
      val service = new DefaultService(() => Source.empty, new Storage[BigDecimal] {
        override val sink: Sink[Event[BigDecimal], _] = Sink.ignore
      })

      val sum = events.map(_.value).reduceOption(_ + _)
      val count = events.size

      source.runWith(service.average(service.sum)).map {
        _ shouldEqual Average(sum.map(_ / count), count)
      }
    }

    "process events correctly" in {
      val events = eventList()
      val from = Random.nextLong()
      val to = Random.nextLong()

      val service = new DefaultService(() => Source(events.map(Success.apply)), new Storage[BigDecimal] {
        override val sink: Sink[Event[BigDecimal], _] = Sink.ignore
      })

      val h = events.head
      service.processEvents(h.eventType, from, to).run().map { avg =>
        val relevantEvents = events.filter { e =>
          e.eventType == h.eventType && e.timestamp >= from && e.timestamp <= to
        }
        val sum = relevantEvents.map(_.value).sum
        val count = relevantEvents.size
        avg shouldEqual Average(Some(sum / count), count)
      }
    }
  }
}
