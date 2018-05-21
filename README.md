# Scala STOMP WebSocket Client

Build on top of [WebSocket Scala Client](https://github.com/andyglow/websocket-scala-client) by [andyglow](https://github.com/andyglow)

## Done and ready to use

* STOMP 1.1 without heartbeat
* `CONNECT`, `SUBSCRIBE`, `UNSUBSCRIBE`, `DISCONNECT`, `ERROR` frame handling
* Custom WS endpoint and HTTP headers

## Not ready to use

* `RECEIPT` frame and `receipe-id` header
* `ACK`, `NACK`, `BEGIN`, `COMMIT`, `ABORT` frames
* Custom heartbeat

### Example

```scala

object Main extends App {
  val client = new MessageBrokerClient(
    "ws://example.com/msgbroker/",
    "login",
    "passcode",
    Map(
      "My-Header" -> "123"
    )
  )

  client.onConnect(_ => {
    print("connected!")
    client.subscribe("/topic/one/*", (msg) => {
      println(s"received message! $msg")
      client.unsubscribe("/topic/one/*")
      client.disconnect()
    })
  }).onFailure({
    case t: Throwable =>
      println(s"failure! ${t.getMessage}")
    case _ =>
      // do nothing
  }).onDisconnect(_ => {
    println("disconnected!")
  }).connect()
}


```