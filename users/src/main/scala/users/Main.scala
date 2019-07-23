package users

import cats.data._
import cats.implicits._

import users.config._
import users.main._
import cats.effect.IOApp

import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.all._
import cats.effect.IO
import cats.effect.ExitCode

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val config = ApplicationConfig(
      executors = ExecutorsConfig(
        services = ExecutorsConfig.ServicesConfig(
          parallellism = 4)),
      services = ServicesConfig(
        users = ServicesConfig.UsersConfig(
          failureProbability = 0.1,
          timeoutProbability = 0.1)))

    val application = Application.fromApplicationConfig.run(config)
    BlazeServerBuilder[IO].withHttpApp(application.routes.orNotFound)
      .bindLocal(80)
      .serve.compile.drain.as(ExitCode.Success)
  }
}
