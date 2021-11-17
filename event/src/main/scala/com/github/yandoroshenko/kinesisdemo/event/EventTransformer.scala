package com.github.yandoroshenko.kinesisdemo.event

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import com.github.yandoroshenko.kinesisdemo.model.Event
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object EventTransformer {

  private val log = LoggerFactory.getLogger(getClass)

  case class Sum(value: Option[BigDecimal], processedCount: Long)

  case class Average(value: Option[BigDecimal], processedCount: Long)

  def averageForInterval(eventType: String, from: Long, to: Long)(
      events: Source[Try[Event[BigDecimal]], _]
  )(implicit ec: ExecutionContext, mat: Materializer): RunnableGraph[Future[Average]] = {
    (logErrors andThen
      filter[BigDecimal] { e =>
        e.eventType == eventType &&
        e.timestamp >= from &&
        e.timestamp <= to
      } andThen average)(events)
  }

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

  def average(events: Source[Event[BigDecimal], _])(implicit ec: ExecutionContext, mat: Materializer): RunnableGraph[Future[Average]] = {
    sum(events).mapMaterializedValue(_.map { sum =>
      Average(sum.value.map(_ / sum.processedCount), sum.processedCount)
    })
  }

  def sum(events: Source[Event[BigDecimal], _])(implicit ec: ExecutionContext, mat: Materializer): RunnableGraph[Future[Sum]] = {
    events.toMat(Sink.fold(Sum(None, 0)) { (sum, event) =>
      sum.copy(value = sum.value.map(_ + event.value).orElse(Some(event.value)), sum.processedCount + 1)
    })(Keep.right)
  }
}
