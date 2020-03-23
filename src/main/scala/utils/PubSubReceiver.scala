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

//object PubSubReceiver {
//  def main(args: Array[String]): Unit = {
//    val projectId = "my-sample-project-191923"
//    val subscription = "tweets-subscription"
//    val topic = "testTopic"
//
//    val privateKey =
//      """-----BEGIN RSA PRIVATE KEY-----
//        |MIIBOgIBAAJBAJHPYfmEpShPxAGP12oyPg0CiL1zmd2V84K5dgzhR9TFpkAp2kl2
//        |9BTc8jbAY0dQW4Zux+hyKxd6uANBKHOWacUCAwEAAQJAQVyXbMS7TGDFWnXieKZh
//        |Dm/uYA6sEJqheB4u/wMVshjcQdHbi6Rr0kv7dCLbJz2v9bVmFu5i8aFnJy1MJOpA
//        |2QIhAPyEAaVfDqJGjVfryZDCaxrsREmdKDlmIppFy78/d8DHAiEAk9JyTHcapckD
//        |uSyaE6EaqKKfyRwSfUGO1VJXmPjPDRMCIF9N900SDnTiye/4FxBiwIfdynw6K3dW
//        |fBLb6uVYr/r7AiBUu/p26IMm6y4uNGnxvJSqe+X6AxR6Jl043OWHs4AEbwIhANuz
//        |Ay3MKOeoVbx0L+ruVRY5fkW+oLHbMGtQ9dZq7Dp9
//        |-----END RSA PRIVATE KEY-----""".stripMargin
//
//
//    val clientEmail = s"test-123@$projectId.iam.gserviceaccount.com"
//
//
//    implicit val system: ActorSystem = ActorSystem()
//    implicit val mat: ActorMaterializer = ActorMaterializer()
//    val config = PubSubConfig(projectId, clientEmail, privateKey)
//
//
//
//
//    val subscriptionSource: Source[ReceivedMessage, Cancellable] =
//      GooglePubSub.subscribe(subscription, config)
//
//    val ackSink: Sink[AcknowledgeRequest, Future[Done]] =
//      GooglePubSub.acknowledge(subscription, config)
//
//    subscriptionSource
//      .map { message =>
//        // do something fun
//        print(message.message)
//        message.ackId
//      }
//      .groupedWithin(1000, 1.minute)
//      .map(AcknowledgeRequest.apply)
//      .to(ackSink)
//      .run()
//  }
//}


//import com.google.cloud.pubsub.v1.AckReplyConsumer
//import com.google.cloud.pubsub.v1.Subscriber
//import com.google.pubsub.v1.ProjectSubscriptionName
//
//object PubSubReceiver {
//  private val PROJECT_ID = "my-sample-project-191923"
//
//  class MessageReceiverExample extends MessageReceiver {
//    def receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer): Unit = {
//      System.out.println("Message Id: " + message.getMessageId + " Data: " + message.getData.toStringUtf8)
//      // Ack only after all work for the message is complete.
//      consumer.ack()
//    }
//  }
//
//
//  def main(args: Array[String]): Unit = {
//
//
//    val hostport = System.getenv("PUBSUB_EMULATOR_HOST")
//
//    val channel: grpc.ManagedChannel =  ManagedChannelBuilder
//      .forAddress("localhost", 8085).usePlaintext()
//      .build()
//
//    val channelProvider =
//      FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
//    val credentialsProvider = NoCredentialsProvider.create()
//
//    // Set the channel and credentials provider when creating a `TopicAdminClient`.
//    // Similarly for SubscriptionAdminClient
//    val topicClient =
//    SubscriptionAdminClient.create(
//      SubscriptionAdminSettings.newBuilder()
//        .setTransportChannelProvider(channelProvider)
//        .setCredentialsProvider(credentialsProvider)
//        .build())
//
//    val subscriptionId = "tweets-subscription"
//    val subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, subscriptionId)
//    try { // create a subscriber bound to the asynchronous message receiver
//      val subscriber: Subscriber =
//        Subscriber.newBuilder(subscriptionName, new MessageReceiverExample())
//          .setChannelProvider(channelProvider)
//          .setCredentialsProvider(credentialsProvider)
//          .build()
//      subscriber.startAsync().awaitRunning()
//
//      subscriber.awaitTerminated()
//    } catch {
//      case e: IllegalStateException =>
//        System.out.println("Subscriber unexpectedly stopped: " + e)
//    }
//  }
//}
//
//object PubSubReceiver {
//  def main(args: Array[String]): Unit = {
//    val projectId = "my-sample-project-191923"
//    val subscription = "tweets-subscription"
//
//    val request = StreamingPullRequest()
//      .withSubscription(s"projects/$projectId/subscriptions/$subscription")
//      .withStreamAckDeadlineSeconds(10)
//
//    val subscriptionSource: Source[ReceivedMessage, Future[Cancellable]] =
//      GooglePubSub.subscribe(request, pollInterval = 1.second)
//
//
//    val ackSink: Sink[AcknowledgeRequest, Future[Done]] =
//      GooglePubSub.acknowledge(parallelism = 1)
//
//    subscriptionSource
//      .map { message =>
//        // do something fun
//        print(message.message)
//        message.ackId
//      }
//      .groupedWithin(10, 1.second)
//      .map(ids => AcknowledgeRequest(ackIds = ids))
//      .to(ackSink)
//  }
//}


//object PubSubReceiver {
//  def main(args: Array[String]): Unit = {
//    val projectId = "my-sample-project-191923"
//    val subscription = "tweets-subscription"
//    val topic = "testTopic"
//
//    implicit val system: ActorSystem = ActorSystem()
//    implicit val mat: ActorMaterializer = ActorMaterializer()
//    val config = PubSubConfig(projectId, LocalPubSub.props.mailbox, LocalPubSub.props.dispatcher)
//
//
//    val subscriptionSource: Source[ReceivedMessage, Cancellable] =
//      GooglePubSub.subscribe(subscription, config)
//
//    val ackSink: Sink[AcknowledgeRequest, Future[Done]] =
//      GooglePubSub.acknowledge(subscription, config)
//
//    subscriptionSource
//      .map { message =>
//        // do something fun
//        print(message.message)
//        message.ackId
//      }
//      .groupedWithin(1000, 1.minute)
//      .map(AcknowledgeRequest.apply)
//      .to(ackSink)
//      .run()
//  }
//}

