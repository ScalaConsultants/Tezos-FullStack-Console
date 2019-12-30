package io.scalac.tezos.translator.routes.dto

import cats.data.NonEmptyList
import cats.instances.parallel._
import cats.syntax.either._
import cats.syntax.parallel._
import io.scalac.tezos.translator.model._
import io.scalac.tezos.translator.routes.dto.DTO.{ErrorDTO, Errors}
import io.scalac.tezos.translator.routes.dto.DTOValidation.ValidationResult
import sttp.model.StatusCode
import scala.concurrent.{ExecutionContext, Future}

trait DTOValidation[T] {

  def validate(value: T): ValidationResult[T]

}

object DTOValidation {

  val maxTinyLength = 255
  val maxUsernameLength = 30

  type ValidationResult[A] = Either[NonEmptyList[DTOValidationError], A]

  def apply[T](value: T)(implicit validator: DTOValidation[T]): ValidationResult[T] =
    validator.validate(value)

  def validateDto[T : DTOValidation](value: T)(implicit ec: ExecutionContext): Future[Either[(ErrorDTO, StatusCode), T]] =
    Future {
      DTOValidation(value) match {
        case Right(value) => value.asRight
        case Left(errors) =>
          val errorsList = errors.map(convertValidationErrorsToString).toList
          (Errors(errorsList), StatusCode.BadRequest).asLeft
      }
    }

  def convertValidationErrorsToString: PartialFunction[DTOValidationError, String] = {
    case FieldToLong(field, maxLength)    => s"field $field is too long, max length - $maxLength"
    case FieldIsEmpty(fieldName)          => s"$fieldName field is empty"
    case FieldIsInvalid(fieldName, field) => s"invalid $fieldName - $field"
  }

  sealed trait DTOValidationError extends Product with Serializable

  final case class FieldToLong(field: String, maxLength: Int) extends DTOValidationError

  final case class FieldIsEmpty(field: String) extends DTOValidationError

  final case class FieldIsInvalid(fieldName: String, field: String) extends DTOValidationError

  def checkStringNotEmpty(string: String,
                          onEmpty: => DTOValidationError): ValidationResult[String] = {
    if (string.trim.isEmpty)
      NonEmptyList.one(onEmpty).asLeft
    else
      string.asRight
  }

  def checkStringNotEmptyAndLength(string: String,
                                   maxLength: Int,
                                   onEmpty: => DTOValidationError,
                                   whenMaxLengthExceeds: => DTOValidationError): ValidationResult[String] = {
    checkStringNotEmpty(string, onEmpty)
      .flatMap(checkStringLength(_, maxLength, whenMaxLengthExceeds))
  }

  def checkStringMatchRegExp(string: String,
                             regExp: String,
                             onNonMatch: => DTOValidationError): ValidationResult[String] = {
    if (string.matches(regExp))
      string.asRight
    else
      NonEmptyList.one(onNonMatch).asLeft
  }

  def checkStringLength(string: String,
                        maxLength: Int,
                        whenMaxLengthExceeds: => DTOValidationError): ValidationResult[String] = {
    if (string.length > maxLength)
      NonEmptyList.one(whenMaxLengthExceeds).asLeft
    else
      string.asRight
  }

  implicit val SendEmailDTOValidation: DTOValidation[SendEmailRoutesDto] = { dto => validateSendEmailDTO(dto) }

  def validateSendEmailDTO: SendEmailRoutesDto => ValidationResult[SendEmailRoutesDto] = { dto =>
    val checkingNameResult: ValidationResult[String] =
      checkStringNotEmptyAndLength(dto.name, maxTinyLength, FieldIsEmpty("name"), FieldToLong("name", maxTinyLength))

    val checkingPhoneIsValid: Either[NonEmptyList[DTOValidationError], Option[String]] =
      checkOptionalString(dto.phone, phoneStr => checkStringMatchRegExp(phoneStr, phoneRegex, FieldIsInvalid("phone", phoneStr)))

    val checkContentNotEmpty: ValidationResult[String] = checkStringNotEmpty(dto.content, FieldIsEmpty("content"))

    val checkEmail: Either[NonEmptyList[DTOValidationError], Option[String]] = checkOptionalString(dto.email, checkEmailIsValid)

    val phoneEmailNonEmptyCheck =
      if (checkingPhoneIsValid.right.exists(_.isEmpty) && checkEmail.right.exists(_.isEmpty)) {
        NonEmptyList.one(FieldIsInvalid("email, phone", "At least one field should be filled")).asLeft
      } else {
        ().asRight
      }

    val v = (checkingNameResult, checkingPhoneIsValid,checkEmail, checkContentNotEmpty).parMapN(SendEmailRoutesDto.apply)

    (phoneEmailNonEmptyCheck, v).parMapN((_, dto) => dto)

  }

  private def checkOptionalString(
    maybeStr: Option[String],
    validate: String => ValidationResult[String]
  ): ValidationResult[Option[String]] =
    maybeStr match {
      case Some(v) if v.nonEmpty => validate(v).map(Some(_))
      case _ => Right(None)
    }

  private def checkEmailIsValid(email: String): ValidationResult[String] =
    checkStringNotEmptyAndLength(email, maxTinyLength, FieldIsEmpty("email"), FieldToLong("email", maxTinyLength))
      .flatMap { mail =>
        EmailAddress.fromString(mail).toEither.bimap(
          _ => NonEmptyList.one(FieldIsInvalid("email", mail)),
          a => a.toString
        )
      }
  private def checkAuthorIsValid(value: String, name: String = "author"): ValidationResult[String] = {
    checkStringNotEmptyAndLength(value, maxTinyLength, FieldIsEmpty(name), FieldToLong(name, maxTinyLength))
  }
  private def checkDescriptionsValid(value: String, name: String= "description"): ValidationResult[String] = {
    checkStringNotEmptyAndLength(value, maxTinyLength, FieldIsEmpty(name), FieldToLong(name, maxTinyLength))
  }

  implicit val LibraryDTOValidation: DTOValidation[LibraryEntryRoutesDto] = { dto => validateLibraryEntryRoutesDto(dto) }

  def validateLibraryEntryRoutesDto: LibraryEntryRoutesDto => ValidationResult[LibraryEntryRoutesDto] = { dto =>
    val checkTitle =
      checkStringNotEmptyAndLength(dto.title, maxTinyLength, FieldIsEmpty("title"), FieldToLong("title", maxTinyLength))
    val checkAuthor =
      checkOptionalString(dto.author, a => checkAuthorIsValid(a))
    val checkEmail =
      checkOptionalString(dto.email, checkEmailIsValid).map(_.map(_.toLowerCase))
    val checkDescription =
      checkOptionalString(dto.description, d => checkDescriptionsValid(d))
    val checkMicheline =
      checkStringNotEmpty(dto.micheline, FieldIsEmpty("micheline"))
    val checkMichelson =
      checkStringNotEmpty(dto.michelson, FieldIsEmpty("michelson"))

    (checkTitle, checkAuthor, checkEmail, checkDescription, checkMicheline, checkMichelson).parMapN(LibraryEntryRoutesDto.apply)
  }

  val phoneRegex: String =
    """^\+?\d{6,18}$"""

}
