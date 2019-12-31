package io.scalac.tezos.translator.service

import com.github.t3hnar.bcrypt._
import io.scalac.tezos.translator.model.{AuthUserData, UserModel}
import io.scalac.tezos.translator.repository.UserRepository
import io.scalac.tezos.translator.routes.dto.DTO.Error
import io.scalac.tezos.translator.model.types.Auth.{Password, UserToken, UserTokenReq, Username}
import slick.jdbc.PostgresProfile.api._
import cats.syntax.either._
import io.scalac.tezos.translator.routes.Endpoints.ErrorResponse
import sttp.model.StatusCode
import eu.timepit.refined._
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UserService(repository: UserRepository, db: Database)(implicit ec: ExecutionContext) {

  private val tokenToUser = new scala.collection.concurrent.TrieMap[UserToken, Username]

  @tailrec
  private def createToken(username: Username): UserToken = {
    val newToken = generateRandomToken
    tokenToUser.putIfAbsent(newToken, username) match {
      case None => newToken
      case Some(_) => createToken(username) // token already exists, retry
    }
  }

  @tailrec
  private def generateRandomToken: UserToken = {
    val maybeNewTokenEntry = refineV[UserTokenReq](Random.alphanumeric.take(30).mkString)
    maybeNewTokenEntry match {
      case Left(_)      => generateRandomToken
      case Right(value) => UserToken(value)
    }
  }

  private def checkPassword(user: UserModel, password: Password): Boolean = {
    password.v.value.isBcrypted(user.passwordHash.v.value)
  }

  def authenticateAndCreateToken(username: Username, password: Password): Future[Option[UserToken]] = {
    db.run(repository.getByUsername(username))
      .map { userOption =>
        val isAuthenticated = userOption.exists(user => checkPassword(user, password))
        if (isAuthenticated) Some(createToken(username)) else None
      }
  }

  def authenticate(token: UserToken): Future[Either[ErrorResponse, AuthUserData]] = Future {
    tokenToUser.get(token)
      .fold {
        (Error("Token not found"), StatusCode.Unauthorized).asLeft[AuthUserData]
      } {
        username => AuthUserData(username, token).asRight
      }
  }

  def logout(token: UserToken): Option[Username] = tokenToUser.remove(token)

}
