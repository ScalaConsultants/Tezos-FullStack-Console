package io.scalac.tezos.translator.routes
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Route}
import io.scalac.tezos.translator.config.CaptchaConfig
import io.scalac.tezos.translator.model.{EmailAddress, SendEmail}
import io.scalac.tezos.translator.routes.dto.DTO.Error
import io.scalac.tezos.translator.routes.directives.DTOValidationDirective._
import io.scalac.tezos.translator.routes.directives.ReCaptchaDirective._
import io.scalac.tezos.translator.routes.dto.SendEmailRoutesDto
import io.scalac.tezos.translator.service.Emails2SendService

import scala.util.{Failure, Success}

class MessageRoutes(
  service: Emails2SendService,
  log: LoggingAdapter,
  reCaptchaConfig: CaptchaConfig,
  adminEmail: EmailAddress
)(implicit actorSystem: ActorSystem) extends HttpRoutes {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def routes: Route =
    (path ("message") & pathEndOrSingleSlash & withReCaptchaVerify(log, reCaptchaConfig)(actorSystem)
      & withSendMessageValidation & post) { sendEmail =>
      val operationPerformed = service.addNewEmail2Send(SendEmail.fromSendEmailRoutesDto(sendEmail, adminEmail))
      onComplete(operationPerformed) {
        case Success(_)   => complete(StatusCodes.OK)
        case Failure(err) =>
          log.error(s"Can't add email to send, err - $err")
          complete(StatusCodes.InternalServerError, Error("Can't save payload"))
      }
    }

  def withSendMessageValidation: Directive[Tuple1[SendEmailRoutesDto]] = withDTOValidation[SendEmailRoutesDto]

}
