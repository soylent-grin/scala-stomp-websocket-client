package org.github.soylent_grin.scala_stomp_websocket_client.services

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.net.URI
import collection.JavaConversions._

import org.github.soylent_grin.scala_stomp_websocket_client.models.Constants._
import org.github.soylent_grin.scala_stomp_websocket_client.models.Frame
import org.java_websocket.client.WebSocketClient

import scala.collection.mutable.ListBuffer

class MessageBrokerClient(uri: String, login: String = "", password: String = "", headers: Map[String, String] = Map.empty) {

  class WebSocketClientImpl(uri: URI, headers: Map[String, String]) extends WebSocketClient(uri, headers) {

    import org.java_websocket.handshake.ServerHandshake

    def onOpen(handshakedata: ServerHandshake): Unit = {
      doConnect()
    }

    def onMessage(message: String): Unit = {
      try {
        val f = Frame.from(message)
        f.command match {
          case CONNECTED =>
            isOpened.set(true)
            connectHandlers.foreach(_ ())
          case ERROR =>
            notifyFailure(new IOException(f.message))
          case MESSAGE =>
            notifyMessage(f.header(DESTINATION), f.message)
          case _ =>
          // do nothing
        }
      } catch {
        case t: Throwable =>
          notifyFailure(t)
      }
    }

    def onClose(code: Int, reason: String, remote: Boolean): Unit = {
      notifyClose()
    }

    def onError(ex: Exception): Unit = {
      notifyFailure(ex)
    }
  }


  private val client = new WebSocketClientImpl(new URI(uri), headers)

  private val messageHandlers = new ConcurrentHashMap[String, (String, String => Unit)]()
  private val connectHandlers = ListBuffer.empty[Unit => Unit]
  private val failureHandlers = ListBuffer.empty[Throwable => Unit]
  private val disconnectHandlers = ListBuffer.empty[Unit => Unit]

  private val isOpened = new AtomicBoolean(false)
  private val subscriptionCounter = new AtomicLong(0)

  // ---

  def connect() = {
    client.connectBlocking()
    this
  }

  def disconnect() = {
    doDisconnect()
    client.close()
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
    println(f.toString)
    client.send(f.toString)
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
