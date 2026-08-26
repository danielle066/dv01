package controllers

import javax.inject._

import models.{FicoBand, GroupBy, LoanFilters}
import play.api.libs.json._
import play.api.mvc._
import services.{LoanLoader, LoanRepository, LoanService}

@Singleton
class ApiController @Inject()(cc: ControllerComponents, repo: LoanRepository)
  extends AbstractController(cc) {

  implicit private val totalsWrites: Writes[models.Totals] = Json.writes[models.Totals]
  implicit private val bucketWrites: Writes[models.Bucket] = Json.writes[models.Bucket]

  def summary(
    groupBy: Option[String],
    state: Option[String],
    grade: Option[String],
    date: Option[String],
    issueMonth: Option[String],
    ficoBand: Option[String]
  ): Action[AnyContent] = Action {
    try {
      val group = GroupBy.parse(groupBy)
      val monthRaw = issueMonth.orElse(date).map(_.trim).filter(_.nonEmpty)
      val fico = ficoBand.map(_.trim).filter(_.nonEmpty).map { label =>
        FicoBand.fromLabel(label).getOrElse {
          throw new IllegalArgumentException(s"unknown ficoBand '$label'")
        }
      }
      val filters = LoanFilters(
        state = state.map(_.trim.toUpperCase).filter(_.nonEmpty),
        grade = grade.map(_.trim.toUpperCase).filter(_.nonEmpty),
        issueMonth = monthRaw.map(LoanLoader.parseIssueMonth),
        ficoBand = fico
      )
      val result = LoanService.summarize(repo.loans, group, filters)
      Ok(Json.obj(
        "groupBy" -> result.groupBy,
        "filters" -> Json.obj(
          "state" -> result.filters("state"),
          "grade" -> result.filters("grade"),
          "issueMonth" -> result.filters("issueMonth"),
          "ficoBand" -> result.filters("ficoBand")
        ),
        "totals" -> Json.toJson(result.totals),
        "buckets" -> Json.toJson(result.buckets)
      ))
    } catch {
      case e: IllegalArgumentException =>
        BadRequest(Json.obj("error" -> e.getMessage, "detail" -> e.getMessage))
    }
  }

  def meta: Action[AnyContent] = Action {
    val m = LoanService.meta(repo.loans)
    Ok(Json.obj(
      "loanCount" -> m("loanCount").asInstanceOf[Int],
      "groupBy" -> m("groupBy").asInstanceOf[Seq[String]],
      "grades" -> m("grades").asInstanceOf[Seq[String]],
      "states" -> m("states").asInstanceOf[Seq[String]],
      "issueMonths" -> m("issueMonths").asInstanceOf[Seq[String]],
      "ficoBands" -> m("ficoBands").asInstanceOf[Seq[String]]
    ))
  }
}
