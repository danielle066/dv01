package models

import java.time.LocalDate

case class Loan(
  amount: Double,
  outstanding: Double,
  ratePct: Double,
  grade: String,
  state: String,
  issueMonth: LocalDate,
  fico: Int,
  status: String
) {
  def ficoBand: FicoBand = FicoBand.fromScore(fico)
}

sealed abstract class FicoBand(val label: String) extends Product with Serializable

object FicoBand {
  case object Below660 extends FicoBand("<660")
  case object Band660_679 extends FicoBand("660-679")
  case object Band680_699 extends FicoBand("680-699")
  case object Band700_719 extends FicoBand("700-719")
  case object Band720_739 extends FicoBand("720-739")
  case object Band740_759 extends FicoBand("740-759")
  case object Band760_779 extends FicoBand("760-779")
  case object Band780_799 extends FicoBand("780-799")
  case object Band800Plus extends FicoBand("800+")

  val all: Seq[FicoBand] = Seq(
    Below660,
    Band660_679,
    Band680_699,
    Band700_719,
    Band720_739,
    Band740_759,
    Band760_779,
    Band780_799,
    Band800Plus
  )

  def fromScore(score: Int): FicoBand =
    if (score < 660) Below660
    else if (score < 680) Band660_679
    else if (score < 700) Band680_699
    else if (score < 720) Band700_719
    else if (score < 740) Band720_739
    else if (score < 760) Band740_759
    else if (score < 780) Band760_779
    else if (score < 800) Band780_799
    else Band800Plus

  def fromLabel(label: String): Option[FicoBand] = {
    val wanted = label.trim.toLowerCase
    all.find(_.label.toLowerCase == wanted)
  }
}

sealed abstract class GroupBy(val value: String) extends Product with Serializable

object GroupBy {
  case object Grade extends GroupBy("grade")
  case object State extends GroupBy("state")
  case object Date extends GroupBy("date")
  case object Fico extends GroupBy("fico")

  def parse(raw: Option[String]): GroupBy = {
    val key = raw.getOrElse("grade").trim.toLowerCase.replace('-', '_')
    key match {
      case "grade" => Grade
      case "state" => State
      case "date" | "issue_d" | "issue_month" | "month" => Date
      case "fico" | "fico_band" | "ficoband" => Fico
      case _ => throw new IllegalArgumentException("groupBy must be one of: grade, state, date, fico")
    }
  }
}

case class LoanFilters(
  state: Option[String] = None,
  grade: Option[String] = None,
  issueMonth: Option[LocalDate] = None,
  ficoBand: Option[FicoBand] = None
) {
  def matches(loan: Loan): Boolean =
    state.forall(_ == loan.state) &&
      grade.forall(_ == loan.grade) &&
      issueMonth.forall(_ == loan.issueMonth) &&
      ficoBand.forall(_ == loan.ficoBand)

  def isEmpty: Boolean =
    state.isEmpty && grade.isEmpty && issueMonth.isEmpty && ficoBand.isEmpty
}

case class Bucket(
  key: String,
  loanCount: Int,
  originalBalance: Double,
  currentBalance: Double,
  avgLoanSize: Double,
  weightedAvgRate: Double,
  weightedAvgFico: Double,
  pctOfOriginalBalance: Double
)

case class Totals(
  loanCount: Int,
  originalBalance: Double,
  currentBalance: Double,
  avgLoanSize: Double,
  weightedAvgRate: Double,
  weightedAvgFico: Double
)

case class Summary(
  groupBy: String,
  filters: Map[String, Option[String]],
  totals: Totals,
  buckets: Seq[Bucket]
)
