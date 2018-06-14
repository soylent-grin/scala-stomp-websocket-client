# Scala STOMP WebSocket Client

Prerequisites
-------------
* Scala 2.11.x

### Installation

Add the following line to your sbt dependencies: 
```scala
"com.github.vooolll" %% "scala-stomp-websocket-client" % "0.1.2"
```

Note: make sure that you have in your `build.sbt`
```scala
resolvers += Resolver.sonatypeRepo("releases")
```


## Done and ready to use

* STOMP 1.1 without heartbeat
* `CONNECT`, `SUBSCRIBE`, `UNSUBSCRIBE`, `DISCONNECT`, `ERROR` frame handling
* Custom WS endpoint and HTTP headers
* Integration with Play! Framework (no Netty-dependency issues because of standalone WS transport)

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