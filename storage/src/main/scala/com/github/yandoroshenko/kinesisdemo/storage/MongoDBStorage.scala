package com.github.yandoroshenko.kinesisdemo.storage

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.alpakka.mongodb.DocumentReplace
import akka.stream.alpakka.mongodb.scaladsl.{MongoSink, MongoSource}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.github.yandoroshenko.kinesisdemo.model.Event
import com.github.yandoroshenko.kinesisdemo.storage.MongoDBStorage.Transformer
import com.mongodb.reactivestreams.client.MongoClients
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.{Filters, ReplaceOptions}
import org.mongodb.scala.model.Sorts

import scala.concurrent.ExecutionContext

case class StorageEvent(_id: String, timestamp: Long, eventType: String, value: String)

class MongoDBStorage[T](config: MongoConfig)(
    implicit actorSystem: ActorSystem,
    executionContext: ExecutionContext,
    transformer: Transformer[T]
) extends Storage[T] {
  private val codecRegistry = fromRegistries(fromProviders(classOf[StorageEvent]), DEFAULT_CODEC_REGISTRY)

  private val client = MongoClients.create(config.url)
  private val db = client.getDatabase(config.database)
  private val collection = db.getCollection(config.collection, classOf[StorageEvent]).withCodecRegistry(codecRegistry)

  override val sink: Sink[Event[T], _] = {
    val futureSink = MongoSource(collection.find(classOf[StorageEvent]).sort(Sorts.descending("timestamp")).limit(1))
      .map(_.timestamp)
      .toMat(Sink.fold(0L)(Math.max))(Keep.right)
      .run()
      .map { maxTimestamp =>
        Flow.apply
          .filter { e: Event[T] =>
            e.timestamp > maxTimestamp
          }
          .map { e =>
            val storageEvent =
              StorageEvent(s"${e.eventType}:${e.timestamp}:${e.value}", e.timestamp, e.eventType, transformer.transform(e.value))
            DocumentReplace(Filters.eq("_id", storageEvent._id), storageEvent)
          }
          .to(MongoSink.replaceOne[StorageEvent](collection, new ReplaceOptions().upsert(true)))
      }

    Sink.futureSink(futureSink)
  }
}

object MongoDBStorage {
  trait Transformer[T] {
    def transform(t: T): String
  }

  implicit case object BigDecimalTransformer extends Transformer[BigDecimal] {
    override def transform(t: BigDecimal): String = t.toString
  }
}
