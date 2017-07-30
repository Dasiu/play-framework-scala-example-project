package testhelpers

import config.ExampleComponents
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneServerPerTestWithComponents
import play.api.db.evolutions.Evolutions
import play.api.http.Status
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Helpers, TestServer}
import play.api.{Application, Configuration}
import slick.dbio.DBIO

trait PlayWithFoodWithServerBaseTest extends PlaySpec
  with OneServerPerTestWithComponents
  with DefaultFutureDuration
  with DefaultExecutionContext
  with Status
  with DefaultAwaitTimeout
  with FutureAwaits
  with BeforeAndAfterEach {

  class PlayWithFoodWithTestComponents extends ExampleComponents(context) {

    override def configuration: Configuration = {
      val testConfig = Configuration.from(TestUtils.config)
      val config = super.configuration
      val testConfigMerged = config ++ testConfig
      config.toString
      testConfigMerged
    }

    implicit lazy val testWsClient: WSClient = wsClient
  }

  override def components: PlayWithFoodWithTestComponents = new PlayWithFoodWithTestComponents

  private def runServerAndCleanUpInMemDb(c: PlayWithFoodWithTestComponents) = {
    runServerAndExecute(c.application) {
      val dbapi = c.dbApi
      Evolutions.cleanupEvolutions(dbapi.database("default"))
    }
  }

  override protected def afterEach(): Unit = {
    runServerAndCleanUpInMemDb(components)
  }

  protected def runServerAndExecute[T](app: Application)(blockWithComponents: => T): T = {
    Helpers.running(TestServer(port, app))(blockWithComponents)
  }

  def runAndAwaitResult[T](action: DBIO[T])(implicit components: ExampleComponents): T = {
    TestUtils.runAndAwaitResult(action)(components.actionRunner, duration)
  }

}
