package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._

class ApiFunctionalSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure("loan.csv.path" -> "test/resources/loans-fixture.csv")
      .build()

  "Loan Stats API" should {

    "summarize by grade" in {
      val Some(result) = route(app, FakeRequest(GET, "/api/summary?groupBy=grade"))
      status(result) mustBe OK
      val body = contentAsJson(result)
      (body \ "groupBy").as[String] mustBe "grade"
      (body \ "totals" \ "loanCount").as[Int] mustBe 12
      val keys = (body \ "buckets").as[Seq[play.api.libs.json.JsValue]].map(b => (b \ "key").as[String]).toSet
      keys must contain allOf ("A", "B", "C")
      (body \ "buckets").as[Seq[play.api.libs.json.JsValue]].map(b => (b \ "loanCount").as[Int]).sum mustBe 12
    }

    "combine filters" in {
      val Some(result) = route(app, FakeRequest(GET, "/api/summary?groupBy=grade&state=CA&date=Dec-2017"))
      status(result) mustBe OK
      val body = contentAsJson(result)
      (body \ "totals" \ "loanCount").as[Int] mustBe 1
      ((body \ "buckets")(0) \ "key").as[String] mustBe "A"
    }

    "return 400 for bad groupBy" in {
      val Some(result) = route(app, FakeRequest(GET, "/api/summary?groupBy=zip"))
      status(result) mustBe BAD_REQUEST
    }

    "return meta for dropdowns" in {
      val Some(result) = route(app, FakeRequest(GET, "/api/meta"))
      status(result) mustBe OK
      val body = contentAsJson(result)
      (body \ "loanCount").as[Int] mustBe 12
      (body \ "grades")(0).as[String] mustBe "A"
    }

    "serve the home page" in {
      val Some(result) = route(app, FakeRequest(GET, "/"))
      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
  }
}
