package users.main

import cats.data._
import org.http4s.RhoDsl._
import users.config._
import org.http4s.rho.RhoRoutes
import cats.Monad
import cats.effect.IO
import org.http4s.rho.swagger.syntax.io
import org.http4s.EntityEncoder
import users.domain.User
import org.http4s.Charset
import org.http4s.DefaultCharset
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import fs2.Chunk

object Routes {
  val reader: Reader[Services, Routes] =
    Reader(Routes.apply)

  val fromApplicationConfig: Reader[ApplicationConfig, Routes] =
    Services.fromApplicationConfig andThen reader
}

case class Routes(services: Services) {
  implicit def idEncoder[F[_]](implicit charset: Charset = DefaultCharset): EntityEncoder[F, User.Id] = {
    val hdr = `Content-Type`(MediaType.text.plain).withCharset(charset)
    EntityEncoder.simple(hdr)(id => Chunk.bytes(id.value.getBytes(charset.nioCharset)))
  }

  final val rhoRoutes = new RhoRoutes[IO] {
    GET / "generateId" |>>
      { IO.fromFuture(IO(services.userManagement.generateId())) map { Ok(_) } }
    GET |>> Ok("Hello world")
  }
  final val middleware = io.createRhoMiddleware()
  final val routes = rhoRoutes.toRoutes(middleware)
}
