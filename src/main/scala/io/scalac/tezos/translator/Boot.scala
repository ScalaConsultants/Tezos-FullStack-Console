package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import io.scalac.tezos.translator.actor.EmailSender
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.model.EmailAddress
import io.scalac.tezos.translator.repository.{Emails2SendRepository, LibraryRepository, UserRepository}
import io.scalac.tezos.translator.routes.util.MMTranslator
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, SendEmailsServiceImpl, UserService}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

object Boot {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("tezos-translator")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val log = system.log
    val config = ConfigFactory.load().getConfig("console")
    val httpConfig = config.getConfig("http")
    val host = httpConfig.getString("host")
    val port = httpConfig.getInt("port")
    val configuration = Configuration.getConfig(log)

    implicit val db: PostgresProfile.backend.Database = Database.forConfig("tezos-db")
    val emails2SendRepo = new Emails2SendRepository
    val libraryRepo     = new LibraryRepository(configuration.dbUtility, db)
    val userRepository = new UserRepository
    val email2SendService = new Emails2SendService(emails2SendRepo, db)
    val libraryService    = new LibraryService(libraryRepo, log)
    val userService = new UserService(userRepository, db)

    val bindingFuture =
      for {
        sendEmailsService <- Future.fromTry(SendEmailsServiceImpl(email2SendService, log, configuration.email, configuration.cron))
        cronEmailSender = EmailSender(sendEmailsService, configuration.cron)
        adminEmail <- Future.fromTry(EmailAddress.fromString(configuration.email.receiver))
        routes = new Routes(email2SendService, libraryService, userService, MMTranslator, log, configuration.reCaptcha, adminEmail)
        binding <- Http().bindAndHandle(routes.allRoutes, host, port)
      } yield (cronEmailSender, binding)

    log.info(s"Server online at http://$host:$port\nPress RETURN to stop...")

    StdIn.readLine()
    bindingFuture
      .flatMap { case (cronEmailSender, binding) =>
        cronEmailSender.cancel()
        binding.unbind()
      }
      .onComplete(_ => system.terminate())
  }
}
