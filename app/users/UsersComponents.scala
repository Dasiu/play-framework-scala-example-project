package users

import authentication.AuthenticationsComponents
import com.softwaremill.macwire.wire
import play.api.mvc.ControllerComponents
import users.controllers.UserController
import users.repositories.UserRepo
import users.services.api.{UserCreator, UserProvider}
import users.services.{UserCreatorImpl, UserProviderImpl, UserRegistrationService, UserRegistrationValidator}

trait UsersComponents extends AuthenticationsComponents with WithControllerComponents {
  lazy val userController: UserController = wire[UserController]
  lazy val userRepo: UserRepo = wire[UserRepo]
  lazy val userCreator: UserCreator = wire[UserCreatorImpl]
  lazy val userProvider: UserProvider = wire[UserProviderImpl]
  lazy val userRegistrationService: UserRegistrationService = wire[UserRegistrationService]
  lazy val userRegistrationValidator: UserRegistrationValidator = wire[UserRegistrationValidator]
}

trait WithControllerComponents {
  def controllerComponents: ControllerComponents
}