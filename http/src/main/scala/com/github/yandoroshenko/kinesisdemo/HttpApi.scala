package com.github.yandoroshenko.kinesisdemo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.path

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.Logger
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object HttpApi {
  private val log = Logger(getClass)

  def server(f: (String, Long, Long) => Future[AverageResponse])(config: HttpConfig)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Unit =
    Http(actorSystem).newServerAt(config.host, config.port).bind {
      {
        path(Segment / "average") { eventType =>
          get {
            parameters("from".as[Long], "to".as[Long]) { (from, to) =>
              log.info("query - eventType: {}, from: {}, to: {}", eventType, from, to)

              complete(f(eventType, from, to))
            }
          }
        }
      }
    }.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        actorSystem.log.info("http endpoint url: http://{}:{}/ - started", address.getHostString, address.getPort)
      case Failure(ex) =>
        actorSystem.log.error("http endpoint - failed to bind, terminating system", ex)
        actorSystem.terminate()
    }
}
