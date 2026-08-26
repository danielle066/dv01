package services

import java.time.LocalDate

import models.{GroupBy, Loan, LoanFilters}
import org.scalatestplus.play.PlaySpec

class LoanServiceSpec extends PlaySpec {

  private val Dec = LocalDate.of(2017, 12, 1)
  private val Nov = LocalDate.of(2017, 11, 1)

  private def loan(
    amount: Double = 10000.0,
    outstanding: Double = 0.0,
    ratePct: Double = 10.0,
    grade: String = "A",
    state: String = "CA",
    issueMonth: LocalDate = Dec,
    fico: Int = 700,
    status: String = "Current"
  ): Loan =
    Loan(amount, outstanding, ratePct, grade, state, issueMonth, fico, status)

  "LoanService.summarize" should {

    "weight rate by original balance" in {
      val loans = Seq(
        loan(amount = 10000.0, ratePct = 10.0, grade = "A"),
        loan(amount = 30000.0, ratePct = 20.0, grade = "A")
      )
      val summary = LoanService.summarize(loans, GroupBy.Grade, LoanFilters())
      summary.totals.weightedAvgRate mustBe 17.5
      summary.totals.originalBalance mustBe 40000.0
      summary.buckets.size mustBe 1
    }

    "stack filters with AND" in {
      val loans = Seq(
        loan(amount = 5000.0, ratePct = 8.0, grade = "A", state = "CA", issueMonth = Dec, fico = 720),
        loan(amount = 8000.0, ratePct = 12.0, grade = "B", state = "CA", issueMonth = Dec, fico = 680),
        loan(amount = 9000.0, ratePct = 11.0, grade = "B", state = "TX", issueMonth = Nov, fico = 690)
      )
      val summary = LoanService.summarize(
        loans,
        GroupBy.Grade,
        LoanFilters(state = Some("CA"), issueMonth = Some(Dec))
      )
      summary.totals.loanCount mustBe 2
      summary.buckets.map(_.key) mustBe Seq("A", "B")
      summary.buckets.head.originalBalance mustBe 5000.0
    }

    "group by FICO bands in band order" in {
      val loans = Seq(
        loan(fico = 661, amount = 1000.0),
        loan(fico = 699, amount = 2000.0),
        loan(fico = 801, amount = 3000.0)
      )
      val summary = LoanService.summarize(loans, GroupBy.Fico, LoanFilters())
      summary.buckets.map(_.key) mustBe Seq("660-679", "680-699", "800+")
      summary.buckets.last.pctOfOriginalBalance mustBe 0.5
    }

    "reject unknown groupBy" in {
      intercept[IllegalArgumentException] {
        GroupBy.parse(Some("zip"))
      }.getMessage must include("groupBy")
    }
  }
}
