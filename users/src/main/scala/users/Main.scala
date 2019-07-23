package users

import cats.implicits._

import users.config._
import cats.effect.IOApp

import cats.effect.IO
import cats.effect.ExitCode
import org.apache.log4j.BasicConfigurator
import users.main.Server

object Main extends IOApp {

  override def main(args: Array[String]) = {
    // Log4j basic config is JVM-global (static); there is no way to encapsulate it in a resuable IO.
    BasicConfigurator.configure()
    super.main(args)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val config: ApplicationConfig = ApplicationConfig(
      executors = ExecutorsConfig(services = ExecutorsConfig.ServicesConfig(parallellism = 4)),
      services = ServicesConfig(users = ServicesConfig.UsersConfig(failureProbability = 0.1, timeoutProbability = 0.1)),
      server = ServerConfig(port = 8080))
    val server = Server.fromApplicationConfig.run(config)
    server.server.serve.compile.drain.as(ExitCode.Success)
  }
}
