package services

import models._

object LoanService {

  def summarize(loans: Seq[Loan], groupBy: GroupBy, filters: LoanFilters): Summary = {
    val rows = if (filters.isEmpty) loans else loans.filter(filters.matches)
    val totalsRaw = accumulate(rows)

    val grouped = rows.groupBy(bucketKey(_, groupBy))
    val buckets = sortedKeys(grouped, groupBy).map { key =>
      val stats = accumulate(grouped(key))
      Bucket(
        key = key,
        loanCount = stats.count,
        originalBalance = roundMoney(stats.orig),
        currentBalance = roundMoney(stats.curr),
        avgLoanSize = roundMoney(stats.avgSize),
        weightedAvgRate = roundRate(stats.wRate),
        weightedAvgFico = roundFico(stats.wFico),
        pctOfOriginalBalance = roundPct(stats.orig, totalsRaw.orig)
      )
    }

    Summary(
      groupBy = groupBy.value,
      filters = Map(
        "state" -> filters.state,
        "grade" -> filters.grade,
        "issueMonth" -> filters.issueMonth.map(LoanLoader.issueMonthLabel),
        "ficoBand" -> filters.ficoBand.map(_.label)
      ),
      totals = Totals(
        loanCount = totalsRaw.count,
        originalBalance = roundMoney(totalsRaw.orig),
        currentBalance = roundMoney(totalsRaw.curr),
        avgLoanSize = roundMoney(totalsRaw.avgSize),
        weightedAvgRate = roundRate(totalsRaw.wRate),
        weightedAvgFico = roundFico(totalsRaw.wFico)
      ),
      buckets = buckets
    )
  }

  def meta(loans: Seq[Loan]): Map[String, Any] = {
    val months = loans.map(_.issueMonth).distinct.sortBy(d => (d.getYear, d.getMonthValue))
    Map(
      "loanCount" -> loans.size,
      "groupBy" -> Seq("grade", "state", "date", "fico"),
      "grades" -> loans.map(_.grade).distinct.sorted,
      "states" -> loans.map(_.state).filter(_.nonEmpty).distinct.sorted,
      "issueMonths" -> months.map(LoanLoader.issueMonthLabel),
      "ficoBands" -> FicoBand.all.map(_.label)
    )
  }

  private def bucketKey(loan: Loan, groupBy: GroupBy): String = groupBy match {
    case GroupBy.Grade => loan.grade
    case GroupBy.State => loan.state
    case GroupBy.Date => LoanLoader.issueMonthLabel(loan.issueMonth)
    case GroupBy.Fico => loan.ficoBand.label
  }

  private def sortedKeys(grouped: Map[String, Seq[Loan]], groupBy: GroupBy): Seq[String] =
    groupBy match {
      case GroupBy.Fico =>
        val order = FicoBand.all.map(_.label).zipWithIndex.toMap
        grouped.keys.toSeq.sortBy(k => order.getOrElse(k, 99))
      case _ => grouped.keys.toSeq.sorted
    }

  private case class Acc(
    count: Int,
    orig: Double,
    curr: Double,
    avgSize: Double,
    wRate: Double,
    wFico: Double
  )

  private def accumulate(rows: Seq[Loan]): Acc = {
    var count = 0
    var orig = 0.0
    var curr = 0.0
    var rateW = 0.0
    var ficoW = 0.0
    rows.foreach { ln =>
      count += 1
      orig += ln.amount
      curr += ln.outstanding
      rateW += ln.ratePct * ln.amount
      ficoW += ln.fico * ln.amount
    }
    Acc(
      count = count,
      orig = orig,
      curr = curr,
      avgSize = if (count == 0) 0.0 else orig / count,
      wRate = if (orig == 0) 0.0 else rateW / orig,
      wFico = if (orig == 0) 0.0 else ficoW / orig
    )
  }

  private def roundMoney(v: Double): Double = BigDecimal(v).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  private def roundRate(v: Double): Double = BigDecimal(v).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
  private def roundFico(v: Double): Double = BigDecimal(v).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  private def roundPct(part: Double, whole: Double): Double =
    if (whole == 0) 0.0
    else BigDecimal(part / whole).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
}
