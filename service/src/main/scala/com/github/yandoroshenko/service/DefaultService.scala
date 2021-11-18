package com.github.yandoroshenko.service

import akka.stream.scaladsl.{Keep, RunnableGraph, Sink}
import com.github.yandoroshenko.kinesisdemo.event.EventProvider
import com.github.yandoroshenko.kinesisdemo.model.Event
import com.github.yandoroshenko.kinesisdemo.storage.Storage
import com.github.yandoroshenko.service.DefaultService.{Average, Sum}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DefaultService(eventProvider: EventProvider[BigDecimal], storage: Storage[BigDecimal])(implicit executionContext: ExecutionContext)
  extends Service {

  private val log = Logger(getClass)

  def processEvents(eventType: String, from: Long, to: Long): RunnableGraph[Future[Average]] = {
    log.info("processEvents - eventType: {}, from: {}, to: {}", eventType, from, to)

    eventProvider
      .provideEvents()
      .divertTo(errorLog, _.isFailure)
      .collect {
        case Success(e) => e
      }
      .wireTap(storage.sink)
      .filter { e =>
        e.eventType == eventType &&
        e.timestamp >= from &&
        e.timestamp <= to
      }
      .toMat(average(sum))(Keep.right)
  }

  val errorLog: Sink[Try[Event[BigDecimal]], _] =
    Sink.foreach {
      case Failure(ex) =>
        log.warn("Failed to parse event: {}", ex.getMessage)
      case _ => ()
    }

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

object DefaultService {
  case class Sum(value: Option[BigDecimal], processedCount: Long)

  case class Average(value: Option[BigDecimal], processedCount: Long)
}
