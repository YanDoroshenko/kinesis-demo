package com.github.yandoroshenko.service

import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import com.github.yandoroshenko.kinesisdemo.event.EventProvider
import com.github.yandoroshenko.kinesisdemo.model.Event
import com.github.yandoroshenko.kinesisdemo.storage.Storage
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class DefaultService(eventProvider: EventProvider[BigDecimal], storage: Storage[BigDecimal])(implicit executionContext: ExecutionContext) extends Service {

  private val log = LoggerFactory.getLogger(getClass)

  def processEvents(eventType: String, from: Long, to: Long): RunnableGraph[Future[Average]] = {
    val parseResult = eventProvider.provideEvents()
    val events = logErrors(parseResult)

    val storedEvents = events.alsoTo(storage.sink)

    val filteredEvents = filter[BigDecimal] { e =>
      e.eventType == eventType &&
        e.timestamp >= from &&
        e.timestamp <= to
    }(events)

    filteredEvents.alsoTo(storage.sink).toMat(average(sum))(Keep.right)
  }

  case class Sum(value: Option[BigDecimal], processedCount: Long)

  case class Average(value: Option[BigDecimal], processedCount: Long)

  def logErrors[T]: Source[Try[Event[T]], _] => Source[Event[T], _] =
    _.splitWhen(_.isFailure)
      .map {
        case Success(event) => Success(event)
        case Failure(ex) => log.warn("Failed to parse event: {}", ex.getMessage)
          Failure(ex)
      }
      .collect {
        case Success(event) => event
      }
      .mergeSubstreams

  def filter[T](predicate: Event[T] => Boolean): Source[Event[T], _] => Source[Event[T], _] =
    _.filter(predicate)

  def average: Sink[Event[BigDecimal], Future[Sum]] => Sink[Event[BigDecimal], Future[Average]] = {
    _.mapMaterializedValue(_.map { sum =>
      Average(sum.value.map(_ / sum.processedCount), sum.processedCount)
    })
  }

  val sum: Sink[Event[BigDecimal], Future[Sum]] = {
    Sink.fold(Sum(None, 0)) { (sum, event) =>
      sum.copy(value = sum.value.map(_ + event.value).orElse(Some(event.value)), sum.processedCount + 1)
    }
  }
}

