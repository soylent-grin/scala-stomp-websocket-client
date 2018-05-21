package org.github.soylent_grin.scala_stomp_websocket_client.models

import org.github.soylent_grin.scala_stomp_websocket_client.models.Constants._

import scala.collection.mutable

case class Frame(command: String,
                 header: Map[String, String],
                 message: String = "") {
  override def toString = {
    var sb = StringBuilder.newBuilder
    sb = sb.++=(command).++=("\n")
    header.foreach(h => sb = sb.++=(s"${h._1}:${h._2}").++=("\n"))
    sb.++=("\n").++=(message).++=(s"$END\n").toString
  }
}

object Frame {
  def from(msg: String) = {
    val line = msg.replace(END, "").split("\n")
    val command = line.headOption.getOrElse("")
    var onMessage = false
    var header = mutable.Map[String, String]()
    var message = StringBuilder.newBuilder
    (line diff List(command)).foreach(line =>
      if (!onMessage) {
        if (line.trim.nonEmpty) {
          val res = line.split(":")
          header += (res(0) -> res(1))
        } else {
          onMessage = true
        }
      } else {
        message = message.++=(line)
      }
    )
    Frame(command, header.toMap, message.toString())
  }
}