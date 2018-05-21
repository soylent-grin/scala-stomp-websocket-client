package org.github.soylent_grin.scala_stomp_websocket_client.services

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import com.github.andyglow.websocket.util.Uri
import com.github.andyglow.websocket.{Websocket, WebsocketClient}
import org.github.soylent_grin.scala_stomp_websocket_client.models.Constants._
import org.github.soylent_grin.scala_stomp_websocket_client.models.Frame

import scala.collection.mutable.ListBuffer

class MessageBrokerClient(uri: String, login: String = "", password: String = "", headers: Map[String, String] = Map.empty) {

  private val client = new WebsocketClient.Builder[String](
    Uri(uri),
    receive,
    WebsocketClient.Builder.Options(
      {
        case t: Throwable =>
          notifyFailure(t)
        case _ =>

      },
      notifyClose,
      headers = headers
    )
  ).build()

  private var ws: Option[Websocket] = None

  private val messageHandlers = new ConcurrentHashMap[String, (String, String => Unit)]()
  private val connectHandlers = ListBuffer.empty[Unit => Unit]
  private val failureHandlers = ListBuffer.empty[Throwable => Unit]
  private val disconnectHandlers = ListBuffer.empty[Unit => Unit]

  private val isOpened = new AtomicBoolean(false)
  private val subscriptionCounter = new AtomicLong(0)

  // ---

  def connect() = {
    ws = Some(client.open())
    doConnect()
    this
  }

  def disconnect() = {
    doDisconnect()
    this
  }

  def subscribe(topic: String, callback: String => Unit) = {
    val subscriptionId = makeSubscriptionId()

    messageHandlers.put(
      subscriptionId,
      (normalizeTopic(topic), callback)
    )

    doSubscribe(topic, subscriptionId)
    this
  }

  def unsubscribe(topic: String) = {
    val s = messageHandlers.keySet().iterator()
    while(s.hasNext) {
      val key = s.next()
      val h = messageHandlers.get(key)
      if (normalizeTopic(topic) == h._1) {
        messageHandlers.remove(key)
        doUnsubscribe(key)
      }
    }
  }

  def close() = {
    this
  }

  def onConnect(f: Unit => Unit) = {
    connectHandlers += f
    this
  }

  def onFailure(f: Throwable => Unit) = {
    failureHandlers += f
    this
  }

  def onDisconnect(f: Unit => Unit) = {
    disconnectHandlers += f
    this
  }

  // ---

  private def doConnect() = {
    sendFrame(
      Frame(
        CONNECT,
        Map(
          LOGIN -> login,
          PASSCODE -> password,
          HEARTBEAT -> "0, 0",
          ACCEPT_VERSION -> "1.1, 1.0, 1.2"
        )
      )
    )
  }

  private def doSubscribe(destination: String, id: String) = {
    sendFrame(
      Frame(
        SUBSCRIBE,
        Map(
          DESTINATION -> destination,
          ID -> id
        )
      )
    )
  }

  private def doUnsubscribe(id: String) = {
    sendFrame(
      Frame(
        UNSUBSCRIBE,
        Map(
          ID -> id
        )
      )
    )
  }

  private def doDisconnect() = {
    sendFrame(
      Frame(
        DISCONNECT,
        Map()
      )
    )
  }

  private def sendFrame(f: Frame) = {
    ws match {
      case Some(w) =>
        println(s"sending: ${f.toString}")
        w ! f.toString
      case _ =>
        // not connected
    }
  }

  private def receive: PartialFunction[Any, Unit] = {
    case msg: String =>
      try {
        val f = Frame.from(msg)
        f.command match {
          case CONNECTED =>
            isOpened.set(true)
            connectHandlers.foreach(_())
          case ERROR =>
            notifyFailure(new IOException(f.message))
          case MESSAGE =>
            notifyMessage(f.header(DESTINATION), f.message)
          case _ =>
            // do nothing
        }
      }
    case _ =>
      // do nothing
  }

  private def notifyMessage(destination: String, msg: String): Unit = {
    val s = messageHandlers.keySet().iterator()
    while(s.hasNext) {
      val h = messageHandlers.get(s.next())
      if (destination.matches(h._1)) {
        h._2(msg)
      }
    }
  }

  private def notifyFailure(t: Throwable): Unit = {
    failureHandlers.foreach(h => {
      h(t)
    })
  }

  private def notifyClose(u: Unit): Unit = {
    disconnectHandlers.foreach(h => {
      h()
    })
  }

  private def makeSubscriptionId(): String = s"sub-${subscriptionCounter.incrementAndGet()}"

  private def normalizeTopic(topic: String): String = topic.replace("*", ".*")

}
