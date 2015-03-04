package ch.epfl.lamp.grading.instrumented

import Instrumentation._

trait InstrumentedSuite {
  val instrumentedClass : String

  def M(className: String, methodName: String,
      methodDescriptor: String): MethodCallTrace =
    MethodCallTrace(className, methodName, methodDescriptor)

  def M(methodName: String, methodDescriptor: String): MethodCallTrace =
    M(instrumentedClass, methodName, methodDescriptor)

  def profile(sanityCall: MethodCallTrace)(instrFun: => Unit)(
      instrChecks: => Unit): Unit = {
    resetProfiling()
    startProfiling()
    try {
      instrFun
    } finally {
      stopProfiling()
      if (wasCalled(sanityCall)) {
        instrChecks
      } else {
        println("profiling checks for " + sanityCall.methodName + " disabled")
        printStatistics()
      }
    }
  }

  def wasCalled(call: MethodCallTrace): Boolean =
    getStatistics.get(call) != None

  def checkCalled(call: MethodCallTrace): Unit = {
    assert(wasCalled(call), "expected " + call.methodName + " to be called")
  }

  def checkCalledNamed(methodName: String): Unit = {
    assert(getStatistics exists { case (m, c) =>
      m.className == instrumentedClass &&
      m.methodName == methodName &&
      c > 0
    }, "expected " + methodName + " to be called")
  }

  def checkNotCalled(call: MethodCallTrace): Unit = {
    assert(!wasCalled(call), "expected " + call.methodName + " NOT to be called")
  }
}
