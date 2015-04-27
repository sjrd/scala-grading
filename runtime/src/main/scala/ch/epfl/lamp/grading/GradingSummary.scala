package ch.epfl.lamp.grading

import java.io.{ File, PrintWriter }
import scala.Predef.{ any2stringadd => _, _ }
import scala.util.Try
import scala.collection.mutable
import scala.pickling.directSubclasses
import scala.pickling.Defaults._
import scala.pickling.json._
import Entry._

final case class GradingSummary(score: Int, maxScore: Int, feedback: String)
object GradingSummary {
  class StringSplitExtractor(splitter: String) {
    def unapplySeq(str: String) = Array.unapplySeq(str.split(splitter))
  }
  object ToInt {
    def unapply(s: String) = Try(s.toInt).toOption
  }

  /** suiteName::suiteWeight */
  object SuiteId extends StringSplitExtractor("::")

  /** suiteName::testName::testWeight */
  object TestId extends StringSplitExtractor("::")

  final case class Suite(val name: String, val weight: Int,
    var complete: Boolean = false,
    val tests: mutable.Map[String, Test] = mutable.Map.empty)

  final case class Test(val name: String, val weight: Int,
    var failure: Option[String])

  /** Given test completion state, compute the grade and provide feedback. */
  def apply(suites: List[Suite]): GradingSummary = {
    val score = suites.map { suite =>
      suite.tests.values.map { test =>
        test.failure.fold(test.weight)(_ => 0)
      }.sum
    }.sum
    val maxScore = suites.map(_.weight).sum
    val feedback = {
      val sb = new StringBuilder
      sb.append {
        "Your solution achieved a testing score of %d out of %d.\n\n".format(score, maxScore)
      }
      if (score == maxScore)
        sb.append("Great job!!!\n\n")
      else {
        sb.append("""Below you can see a short feedback for every test that failed,
                    |indicating the reason for the test failure and how many points
                    |you lost for each individual test.
                    |
                    |Tests that were aborted took too long too complete or crashed the
                    |JVM. Such crashes can arise due to infinite non-terminitaing
                    |loops or recursion (StackOverflowException) or excessive mamory
                    |consumption (OutOfMemoryException).
                    |
                    |""".stripMargin)
        for {
          s <- suites
          t <- s.tests.values
          msg <- t.failure
        } {
          sb.append(s"[Test Description] ${t.name}\n")
          sb.append(s"[Observed Error] $msg\n")
          sb.append(s"[Lost Points] ${t.weight}\n\n")
        }
      }
      sb.toString
    }
    GradingSummary(score, maxScore, feedback)
  }

  /** Replay the event records from the file and construct test completion state. */
  def apply(file: File): GradingSummary = {
    val jsons = io.Source.fromFile(file).getLines.mkString("\n").split("\n\n")
    val entries = jsons.map(_.unpickle[Entry])
    val suites = mutable.Map.empty[String, Suite]
    entries.foreach {
      case SuiteStart(SuiteId(name, ToInt(weight))) =>
        suites += name -> Suite(name, weight)
      case SuiteEnd(SuiteId(name, _)) =>
        suites(name).complete = true
      case TestStart(TestId(suiteName, name, ToInt(weight))) =>
        suites(suiteName).tests +=
          name -> Test(name, weight, failure = Some("test has been aborted"))
      case TestSuccess(TestId(suiteName, name, _)) =>
        suites(suiteName).tests(name).failure = None
      case TestFailure(TestId(suiteName, name, _), msg: String) =>
        suites(suiteName).tests(name).failure = Some(msg)
    }
    GradingSummary(suites.values.toList)
  }
}

object GradingSummaryRunner {
  def main(args: Array[String]): Unit = {
    val inPath = args(0)
    val outPath = args(1)
    val summary = GradingSummary(new File(inPath))
    val writer = new PrintWriter(new File(outPath))
    try writer.print(summary.pickle.value)
    finally writer.close()
  }
}
