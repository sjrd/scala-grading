package ch.epfl.lamp.grading

import org.scalatest.{Tag, FunSuiteLike}

import java.security._
import java.util.concurrent._

trait GradingSuite extends FunSuiteLike {

  /**
   * For the real grading, ScalaTest is executed in a separate JVM by the ScalaTestRunner
   * with a security manager enabled. You can run the separte JVM using the `scalaTest` task.
   * In this case we run tests in a security manager, and using a test timeout.
   *
   * However, you can still run the tests in SBT's JVM using the standard `test` task. In
   * that case, we can't use the security manager (it has not been activated in the command
   * line), and also we don't use the timeout.
   */
  val securityEnabled: Boolean =
    util.Properties.propIsSet("java.security.manager")

  // Timeout per test, defined in Settings.scala. There's also a global timeout
  // when running Scalatest in a separate JVM, see ScalaTestRunner
  val individualTestTimeout: Int =
    util.Properties.propOrEmpty("scalatest.individualTestTimeout").toInt

  // List of files that are readable under the security manager
  val readableFiles: List[String] =
    util.Properties.propOrEmpty("scalatest.readableFiles").split(":").filterNot(_.isEmpty).toList

  val defaultWeight: Int =
    util.Properties.propOrEmpty("scalatest.defaultWeight").toInt

  /**
   * Run `task` and abort if it takes too long. Seems the only way to do it
   * is using (deprecated) Thread.stop()
   *  http://stackoverflow.com/questions/5715235/java-set-timeout-on-a-certain-block-of-code
   */
  def timeoutTask(task: => Unit): Unit = {
    val executor = Executors.newSingleThreadExecutor()
    val t = new Thread {
      override def run(): Unit = task
    }
    val future: Future[Unit] = executor.submit(new Callable[Unit] {
      override def call(): Unit = t.run()
    })
    try {
      future.get(individualTestTimeout, TimeUnit.SECONDS)
    } catch {
      case to: TimeoutException =>
        t.stop()
        future.cancel(true)
        throw to
    } finally {
      executor.shutdown()
    }
  }

  def runWithoutPrivileges(op: => Unit): Unit = {
    val action = new PrivilegedAction[Unit] {
      def run {
        try { op }
        catch {
          case td: java.lang.ThreadDeath =>
            // appears when there are timeouts. no need to do anything, timeouts
            // are caught below
            ()

          case err: StackOverflowError =>
            // re-throw as error - gives nicer feedback output
            val trace = err.getStackTrace.take(20).mkString(
                "Stack trace:\n", "\n", "\n\nReported through:")
            throw new Exception(
                s"Error occurred during test execution: $err\n$trace")
        }
      }
    }
    val originalContext = AccessController.getContext
    val combiner = new DomainCombiner {
      def combine(p1: Array[ProtectionDomain],
          p2: Array[ProtectionDomain]): Array[ProtectionDomain] = {
        // revoke most permissions
        val permissions = new Permissions()
        permissions.add(new java.util.PropertyPermission("*", "read"))
        for (file <- readableFiles)
          permissions.add(new java.io.FilePermission(file, "read"))
        permissions.add(new java.lang.reflect.ReflectPermission("suppressAccessChecks"))
        permissions.add(new java.lang.RuntimePermission("getenv.*", "read"))
        permissions.add(new java.lang.RuntimePermission("setContextClassLoader"))
        permissions.add(new java.lang.RuntimePermission("modifyThread"))
        permissions.add(new java.lang.RuntimePermission("accessDeclaredMembers"))
        Array(new ProtectionDomain(null, permissions))
      }
    }
    val context = new AccessControlContext(originalContext, combiner)
    try {
      timeoutTask(AccessController.doPrivileged(action, context))
    } catch {
      case to: TimeoutException =>
        throw new Exception(
            s"Test timeout: aborted after $individualTestTimeout " +
            "seconds; Check for infinite loops!")
    }
  }

  def test(testName: String, weight: Int, testTags: Tag*)(testFun: => Unit): Unit = {
    val name = weight.toString + "\n" + testName
    super.test(name, testTags: _*) {
      if (securityEnabled) runWithoutPrivileges(testFun)
      else testFun
    }
  }

  override def test(testName: String, testTags: Tag*)(testFun: => Unit): Unit = {
    test(testName, defaultWeight, testTags: _*)(testFun)
  }

  def ignore(testName: String, weight: Int, testTags: Tag*)(testFun: => Unit): Unit = {
    val name = weight.toString + "\n" + testName
    super.ignore(name, testTags: _*) {
      if (securityEnabled) runWithoutPrivileges(testFun)
      else testFun
    }
  }

  override def ignore(testName: String, testTags: Tag*)(testFun: => Unit): Unit = {
    ignore(testName, defaultWeight, testTags: _*)(testFun)
  }
}
