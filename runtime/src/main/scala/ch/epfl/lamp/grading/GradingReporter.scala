package ch.epfl.lamp.grading

import java.io.{File, PrintWriter, FileOutputStream}
import scala.collection.mutable.ListBuffer
import org.scalatest.exceptions.TestFailedException
import org.scalatest.Reporter
import org.scalatest.events._
import scala.pickling.directSubclasses
import scala.pickling.Defaults._
import scala.pickling.json._
import Entry._

@directSubclasses(Array(classOf[SuiteStart], classOf[SuiteEnd],
  classOf[TestStart], classOf[TestSuccess], classOf[TestFailure]))
sealed abstract class Entry
object Entry {
  final case class SuiteStart(suiteId: String) extends Entry
  final case class SuiteEnd(suiteId: String) extends Entry
  final case class TestStart(testId: String) extends Entry
  final case class TestSuccess(testId: String) extends Entry
  final case class TestFailure(testId: String, msg: String) extends Entry
}

/**
 *  Test report produced by the scala-grading consists of a json-serialized
 *  list of entries delimited by new lines.
 *
 *  For example for a test suite "Suite1" with tests "a" (weight 1) and
 *  "b" (weight 2) where second test failed and first one succeed one would
 *  get a following report:
 *
 *    Seq(
 *      SuiteStart("foo::3"),
 *      TestStart("foo::a::1"),
 *      TestSuccess("foo::a::1"),
 *      TestStart("foo::a::2"),
 *      TestFailure("foo::b::2", "FooBarException"),
 *      SuiteEnd("foo::3")
 *    )
 *
 *  Due to matching start/end records it's easy to tell if the thing crashed or not.
 */
class GradingReporter extends Reporter {
  private val outfile: File = {
    val prop = System.getProperty("scalatest.reportFile")
    if (prop == null) sys.error("scalatest.reportFile property not defined")
    new File(prop)
  }

  private def record(entry: Entry) = {
    val p = new PrintWriter(new FileOutputStream(outfile, true))
    try {
      p.print(entry.pickle.value)
      p.print("\n\n")
    } finally {
      p.close()
    }
  }

  def apply(event: Event): Unit = event match {
    /* Discovery suite is automatically injected by
     * scalatest. We are not interested in it.
     */
    case e: SuiteStarting if e.suiteName != "DiscoverySuite" => record(SuiteStart(e.suiteName))
    case e: SuiteCompleted if e.suiteName != "DiscoverySuite" => record(SuiteEnd(e.suiteName))
    /* We don't get a `TestStarting` for ignored tests, but we do get here
     * one tests that use `pending`.
     */
    case e: TestStarting => record(TestStart(e.testName))
    case e: TestSucceeded => record(TestSuccess(e.testName))
    case e: TestPending => record(TestSuccess(e.testName))
    case e: TestFailed =>
      record(TestFailure(e.testName, e.message + (e.throwable match {
        case None => ""
        case Some(testFailed: TestFailedException) => ""
        case Some(thrown) =>
          /* The standard output is captured by sbt and printed as
           * `testing tool debug output` in the feedback.
           */
          println("[test failure log] test name: " + e.testName)
          println(exceptionString(thrown) + "\n\n")
          "\n[exception was thrown] detailed error message in debug output section below"
      })))
    case _ => ()
  }

  def exceptionString(e: Throwable): String =
    e.toString + "\n" + e.getStackTrace.take(25).map(_.toString).mkString("\n")
}
