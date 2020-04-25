package utils

import java.util.concurrent.TimeUnit

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.{AckReplyConsumer, MessageReceiver, Subscriber, SubscriptionAdminClient, SubscriptionAdminSettings}
import com.google.pubsub.v1.{ProjectSubscriptionName, PubsubMessage}
import com.typesafe.config.ConfigFactory
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver


class PubSubReceiver(host: String, port: Int)
  extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) with Logging {

  private val PROJECT_ID = ConfigFactory.load().getString("project.id")
  private val SUBSCRIPTION_ID = ConfigFactory.load().getString("pubsub.subscription")
  private val TOPIC_NAME = ConfigFactory.load().getString("pubsub.topic")


  def onStart() {
    new Thread("PubSub Receiver") {
      override def run() {
        receive()
      }
    }.start()
  }

  def onStop() {
    // There is nothing much to do as the thread calling receive()
    // is designed to stop by itself if isStopped() returns false
  }

  class MessageReceiverExample extends MessageReceiver {
    def receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer): Unit = {
      //System.out.println("Message Id: " + message.getMessageId)
      store(message.getData.toStringUtf8)
      consumer.ack()
    }
  }

  private def receive() {
    try {

      val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(host, port).usePlaintext()
        .build()

      val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
      val credentialsProvider = NoCredentialsProvider.create()

      val topicClient =
        SubscriptionAdminClient.create(
          SubscriptionAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build())


      val subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID)

      val subscriber: Subscriber =
        Subscriber.newBuilder(subscriptionName, new MessageReceiverExample())
          .setChannelProvider(channelProvider)
          .setCredentialsProvider(credentialsProvider)
          .build()

      subscriber.startAsync().awaitRunning(5, TimeUnit.SECONDS)
      subscriber.awaitTerminated()

      restart("Trying to connect again")
    } catch {
      case e: Throwable => print("Subscriber unexpectedly stopped: " + e)
    }
  }
}
