package com.github.yandoroshenko.kinesisdemo.event

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.kinesis.scaladsl.KinesisSource
import akka.stream.alpakka.kinesis.{ShardIterator, ShardSettings}
import akka.stream.scaladsl.{Flow, Source}
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import com.github.yandoroshenko.kinesisdemo.model.implicits._
import com.github.yandoroshenko.kinesisdemo.model.{Event, EventParser}
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.{ListShardsRequest, Record, Shard}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.Try

case class KinesisConfig(
    region: String,
    awsAccessKeyId: String,
    awsSecretAccessKey: String,
    streamName: String,
    refreshInterval: FiniteDuration,
    limit: Int
)

class KinesisEventSource(config: KinesisConfig)(implicit actorSystem: ActorSystem, ec: ExecutionContext) extends EventSource[BigDecimal] {
  val amazonKinesisAsync: KinesisAsyncClient =
    KinesisAsyncClient
      .builder()
      .credentialsProvider(
        () =>
          new AwsCredentials {
            override def accessKeyId(): String = config.awsAccessKeyId

            override def secretAccessKey(): String = config.awsSecretAccessKey
          }
      )
      .httpClient(AkkaHttpClient.builder().withActorSystem(actorSystem).build())
      .region(Region.of(config.region))
      .build()

  override def provideEvents(): Source[Try[Event[BigDecimal]], _] = {

    val shards: Source[List[Shard], NotUsed] = Source.future(
      amazonKinesisAsync
        .listShards(ListShardsRequest.builder().streamName(config.streamName).build())
        .asScala
        .map(_.shards().asScala.toList)
    )

    def events(shardIds: List[String]): Source[Record, NotUsed] =
      KinesisSource.basicMerge(
        shardIds.map { shardId =>
          ShardSettings(streamName = config.streamName, shardId)
            .withRefreshInterval(config.refreshInterval)
            .withLimit(config.limit)
            .withShardIterator(ShardIterator.TrimHorizon)
        },
        amazonKinesisAsync
      )

    shards
      .flatMapConcat { shards =>
        events(shards.map(_.shardId()))
      }
      .via(Flow.fromFunction { record =>
        EventParser.parse[BigDecimal](record.data().toString())
      })
  }
}
