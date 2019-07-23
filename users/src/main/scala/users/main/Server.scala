package users.main

import users.config.ServerConfig
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.all._
import cats.effect.IO
import cats.data.Reader
import users.config.ApplicationConfig
import cats.effect.ConcurrentEffect
import cats.effect.Timer
import cats.effect.ContextShift

object Server {
  val reader: Reader[(ServerConfig, Routes), Server] =
    Reader((Server.apply _).tupled)

  val fromApplicationConfig: Reader[ApplicationConfig, Server] =
    (for {
      config ← ServerConfig.fromApplicationConfig
      routes ← Routes.fromApplicationConfig
    } yield (config, routes)) andThen reader
}

case class Server(config: ServerConfig, routes: Routes) {
  def server(implicit concurrentEffect: ConcurrentEffect[IO], timer: Timer[IO]) =
    BlazeServerBuilder[IO].withHttpApp(routes.routes.orNotFound).bindLocal(config.port)
}