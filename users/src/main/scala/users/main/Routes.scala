package users.main

import cats.data._
import org.http4s.RhoDsl._
import users.config._
import org.http4s.rho.RhoRoutes
import cats.Monad
import cats.effect.IO
import org.http4s.rho.swagger.syntax.io

object Routes {
  val reader: Reader[Services, Routes] =
    Reader(Routes.apply)

  val fromApplicationConfig: Reader[ApplicationConfig, Routes] =
    Services.fromApplicationConfig andThen reader
}

case class Routes(services: Services) {
  final val rhoRoutes = new RhoRoutes[IO] {
    GET |>> Ok("Hello world")
  }
  final val middleware = io.createRhoMiddleware()
  final val routes = rhoRoutes.toRoutes(middleware)
}
