package com.github.phisgr.gatling.grpc.stream

import com.github.phisgr.gatling.grpc.Reflections
import com.github.phisgr.gatling.grpc.stream.StreamCall.Cancelled
import io.gatling.commons.util.Clock
import io.grpc.{ClientCall, Metadata, Status}
import io.netty.channel.EventLoop

class StreamListener[Res](
  state: StreamCall[_, Res, _],
  clock: Clock,
  eventLoop: EventLoop,
  ignoreMessage: Boolean
) extends ClientCall.Listener[Res] {
  override def onHeaders(headers: Metadata): Unit = {}

  override def onMessage(message: Res): Unit = {
    val receiveTime = clock.nowMillis
    if (ignoreMessage) {
      state.call.request(1)
    } else {
      eventLoop.execute { () => state.onRes(message, receiveTime) }
    }
  }

  override def onClose(status: Status, trailers: Metadata): Unit = {
    val receiveTime = clock.nowMillis
    if ((status.getCause ne Cancelled) && (status ne Reflections.SHUTDOWN_NOW_STATUS) && !eventLoop.isShutdown) {
      eventLoop.execute { () => state.onServerCompleted(status, trailers, receiveTime) }
    }
  }
}
