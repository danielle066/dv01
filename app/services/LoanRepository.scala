package services

import javax.inject.{Inject, Singleton}

import models.Loan
import play.api.{Configuration, Logger}

@Singleton
class LoanRepository @Inject()(config: Configuration) {

  private val log = Logger(getClass)

  val loans: Seq[Loan] = {
    val path = config.get[String]("loan.csv.path")
    log.info(s"Loading loans from $path")
    LoanLoader.loadLoans(path)
  }
}
