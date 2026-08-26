package services

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.time.LocalDate

import com.github.tototoshi.csv.CSVReader
import models.Loan
import play.api.Logger

object LoanLoader {

  private val log = Logger(getClass)

  private val Months: Map[String, Int] = Map(
    "jan" -> 1, "feb" -> 2, "mar" -> 3, "apr" -> 4, "may" -> 5, "jun" -> 6,
    "jul" -> 7, "aug" -> 8, "sep" -> 9, "oct" -> 10, "nov" -> 11, "dec" -> 12
  )

  private val MonthNames: Array[String] =
    Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

  def loadLoans(path: String): Seq[Loan] = {
    val file = new File(path)
    if (!file.exists()) {
      throw new IllegalArgumentException(
        s"No csv at $path. Unzip the Loan Stats file into data/ or set LOAN_CSV / loan.csv.path."
      )
    }

    val br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
    try {
      var headerLine = br.readLine()
      if (headerLine == null) return Seq.empty
      if (!headerLine.contains("loan_amnt")) {
        headerLine = br.readLine()
        if (headerLine == null) {
          throw new IllegalArgumentException(s"CSV at $path has no header row")
        }
      }

      val headers = headerNames(headerLine)
      val reader = CSVReader.open(br)
      var skipped = 0
      val loans = scala.collection.mutable.ArrayBuffer.empty[Loan]
      reader.foreach { cols =>
        val row = headers.zipAll(cols, "", "").toMap
        parseRow(row) match {
          case Some(loan) => loans += loan
          case None => skipped += 1
        }
      }
      log.info(s"parsed ${loans.size} loans from ${file.getName} (skipped $skipped)")
      loans.toVector
    } finally {
      br.close()
    }
  }

  def parseRate(raw: Option[String]): Double =
    raw.map(_.trim.stripSuffix("%").trim).filter(_.nonEmpty).map(_.toDouble).getOrElse(0.0)

  def parseIssueMonth(raw: String): LocalDate = {
    val value = raw.trim
    if (value.isEmpty) throw new IllegalArgumentException("missing issue_d")
    if (value.head.isDigit) {
      val parts = value.split("-")
      LocalDate.of(parts(0).toInt, parts(1).toInt, 1)
    } else {
      val parts = value.split("-")
      val mon = Months.getOrElse(
        parts(0).take(3).toLowerCase,
        throw new IllegalArgumentException(s"bad issue_d '$value'")
      )
      LocalDate.of(parts(1).toInt, mon, 1)
    }
  }

  def issueMonthLabel(d: LocalDate): String =
    s"${MonthNames(d.getMonthValue - 1)}-${d.getYear}"

  private def headerNames(headerLine: String): Seq[String] = {
    val reader = CSVReader.open(new java.io.StringReader(headerLine))
    try reader.readNext().getOrElse(Nil)
    finally reader.close()
  }

  private def parseRow(row: Map[String, String]): Option[Loan] = {
    val amountOpt = toDouble(row.get("loan_amnt"))
    val grade = row.getOrElse("grade", "").trim
    if (amountOpt.isEmpty || grade.isEmpty) return None

    try {
      val low = toInt(row.get("fico_range_low"))
      val high = toInt(row.get("fico_range_high"))
      val fico = (low, high) match {
        case (Some(l), Some(h)) => Some((l + h) / 2)
        case (Some(l), None) => Some(l)
        case (None, Some(h)) => Some(h)
        case _ => None
      }
      fico.map { f =>
        Loan(
          amount = amountOpt.get,
          outstanding = toDouble(row.get("out_prncp")).getOrElse(0.0),
          ratePct = parseRate(row.get("int_rate")),
          grade = grade,
          state = row.getOrElse("addr_state", "").trim,
          issueMonth = parseIssueMonth(row.getOrElse("issue_d", "")),
          fico = f,
          status = row.getOrElse("loan_status", "").trim
        )
      }
    } catch {
      case _: IllegalArgumentException => None
      case _: NumberFormatException => None
    }
  }

  private def toDouble(raw: Option[String]): Option[Double] =
    raw.map(_.trim).filter(_.nonEmpty).flatMap { s =>
      try Some(s.toDouble) catch { case _: NumberFormatException => None }
    }

  private def toInt(raw: Option[String]): Option[Int] =
    toDouble(raw).map(_.toInt)
}
