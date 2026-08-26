package services

import java.nio.file.Files
import java.time.LocalDate

import models.FicoBand
import org.scalatestplus.play.PlaySpec

class LoanLoaderSpec extends PlaySpec {

  private val Fixture = "test/resources/loans-fixture.csv"

  "LoanLoader" should {

    "skip prospectus notes and trailing totals" in {
      val loans = LoanLoader.loadLoans(Fixture)
      loans.size mustBe 12
      loans.foreach { ln =>
        ln.grade must fullyMatch regex "[A-G]"
        ln.amount must be > 0.0
      }
    }

    "strip percent from rate" in {
      LoanLoader.parseRate(Some("  6.08%")) mustBe 6.08
      LoanLoader.parseRate(Some("10.00")) mustBe 10.0
    }

    "accept LC and ISO issue months" in {
      LoanLoader.parseIssueMonth("Dec-2017") mustBe LocalDate.of(2017, 12, 1)
      LoanLoader.parseIssueMonth("2017-12") mustBe LocalDate.of(2017, 12, 1)
    }

    "use FICO midpoint" in {
      val tmp = Files.createTempFile("tiny", ".csv")
      Files.write(
        tmp,
        (
          "Notes\n" +
            "loan_amnt,int_rate,grade,addr_state,issue_d,fico_range_low,fico_range_high,out_prncp,loan_status\n" +
            "10000,10.00%,A,CA,Dec-2017,700,704,5000,Current\n"
        ).getBytes("UTF-8")
      )
      try {
        val loans = LoanLoader.loadLoans(tmp.toString)
        loans.size mustBe 1
        loans.head.fico mustBe 702
        loans.head.ficoBand mustBe FicoBand.Band700_719
      } finally {
        Files.deleteIfExists(tmp)
      }
    }
  }
}
