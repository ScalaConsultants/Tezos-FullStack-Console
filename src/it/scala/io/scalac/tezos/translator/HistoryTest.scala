package io.scalac.tezos.translator

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForEachTestContainer, MySQLContainer}
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.model.{HistoryViewModel, Translation, TranslationDomainModel}
import io.scalac.tezos.translator.repository.{Emails2SendRepository, LibraryRepository, TranslationRepository}
import io.scalac.tezos.translator.routes.JsonSupport
import io.scalac.tezos.translator.schema.TranslationTable
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, TranslationsService}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.JdbcBackend
import slick.jdbc.MySQLProfile.api._

class HistoryTest extends FlatSpec with Matchers with ScalatestRouteTest with JsonSupport with ForEachTestContainer {
  override lazy val container = MySQLContainer()

  private trait TranslationsFixture extends DbTestBase {
    implicit val repository: TranslationRepository = new TranslationRepository

    implicit val db: JdbcBackend#DatabaseDef = DbTestBase.dbFromContainer(container)

    private val service = new TranslationsService

    val emails2SendRepo = new Emails2SendRepository
    val libraryRepo     = new LibraryRepository
    val email2SendService = new Emails2SendService(emails2SendRepo, db)
    val libraryService    = new LibraryService(libraryRepo, db)
    val log: LoggingAdapter = system.log
    val config: Configuration = Configuration.getConfig(log)

    val routes: Route = new Routes(service, email2SendService, libraryService, Fakes.MMTranslatorFake, log, config).allRoutes

    override def testDb = db

    createTables()

    private val addMichelineTranslation = addTranslation(Translation.FromMicheline, _, _)

    private val addMichelsonTranslation = addTranslation(Translation.FromMichelson, _, _)

    private def addTranslation(from: Translation.From, source: String, translation: String) =
      runDB(
        TranslationTable.translations += TranslationDomainModel(id = None, from, source, translation, createdAt = DateTime.now)
      )

    (1 to 100) foreach { i =>
      addMichelineTranslation(s"from micheline $i", s"to michelson $i")
      addMichelsonTranslation(s"from michelson $i", s"to micheline $i")
    }
  }

  "A History" should "return 10 mixed translations if query parameters not specified" in new TranslationsFixture {
    Get("/v1/translations") ~> routes ~> check {
      status shouldBe StatusCodes.OK

      val history = responseAs[List[HistoryViewModel]]
      history should have size 10

      val expectedSources = List("from michelson 100", "from micheline 100", "from michelson 99", "from micheline 99",
        "from michelson 98", "from micheline 98", "from michelson 97", "from micheline 97", "from michelson 96", "from micheline 96")
      val expectedTranslations = List("to micheline 100", "to michelson 100", "to micheline 99", "to michelson 99",
        "to micheline 98", "to michelson 98", "to micheline 97", "to michelson 97", "to micheline 96", "to michelson 96")

      history.map(_.source) shouldBe expectedSources
      history.map(_.translation) shouldBe expectedTranslations
    }
  }

  it should "fails for unknown source" in new TranslationsFixture {
    Get("/v1/translations?source=lorem") ~> Route.seal(routes) ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[String] shouldBe "The query parameter 'source' was malformed:\n'lorem' is not a valid `source` value"
    }
  }

  it should "return only micheline to michelson translations" in new TranslationsFixture {
    Get("/v1/translations?source=micheline") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 10

      val expectedSources = List("from micheline 100", "from micheline 99", "from micheline 98", "from micheline 97",
        "from micheline 96", "from micheline 95", "from micheline 94", "from micheline 93", "from micheline 92", "from micheline 91")
      val expectedTranslations = List("to michelson 100", "to michelson 99", "to michelson 98", "to michelson 97",
        "to michelson 96", "to michelson 95", "to michelson 94", "to michelson 93", "to michelson 92", "to michelson 91")

      history.map(_.source) shouldBe expectedSources
      history.map(_.translation) shouldBe expectedTranslations
    }
  }

  it should "return only michelson to micheline translations" in new TranslationsFixture {
    Get("/v1/translations?source=michelson") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 10

      val expectedSources = List("from michelson 100", "from michelson 99", "from michelson 98", "from michelson 97",
        "from michelson 96", "from michelson 95", "from michelson 94", "from michelson 93", "from michelson 92", "from michelson 91")

      history.map(_.source) shouldBe expectedSources
    }
  }

  it should "return limited history entries filtered by micheline" in new TranslationsFixture {
    Get("/v1/translations?source=micheline&limit=3") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 3

      val expectedSources = List("from micheline 100", "from micheline 99", "from micheline 98")
      val expectedTranslations = List("to michelson 100", "to michelson 99", "to michelson 98")

      history.map(_.source) shouldBe expectedSources
      history.map(_.translation) shouldBe expectedTranslations
    }
  }

  it should "return limited history entries filtered by michelson" in new TranslationsFixture {
    Get("/v1/translations?source=michelson&limit=3") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val history = responseAs[List[HistoryViewModel]]
      history should have size 3

      val expectedSources = List("from michelson 100", "from michelson 99", "from michelson 98")
      val expectedTranslations = List("to micheline 100", "to micheline 99", "to micheline 98")

      history.map(_.source) shouldBe expectedSources
      history.map(_.translation) shouldBe expectedTranslations
    }
  }
}
