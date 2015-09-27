package ch.epfl.lamp.grading.tests

import org.scalatest.FunSuite

import ch.epfl.lamp.grading.instrumented.InstrumentedSuite

import instrenabled._
import instrdisabled._

class InstrumentationTest extends FunSuite with InstrumentedSuite {

  val instrumentedClass = classOf[InstrumentationEnabled].getName.replace('.', '/')

  test("instrumentation enabled") {
    val obj = new InstrumentationEnabled
    var enabled = false
    profile(M("instrumentedCaller", "()V")) {
      obj.instrumentedCaller()
    } {
      enabled = true
    }
    assert(enabled, "instrumentation is disabled on InstrumentationEnabled")
  }

  test("checkCalled") {
    val obj = new InstrumentationEnabled
    profile(M("instrumentedCaller", "()V")) {
      obj.instrumentedCaller()
    } {
      checkCalled(M("instrumentedCallee", "()V"))
    }
  }

  test("checkNotCalled") {
    val obj = new InstrumentationEnabled
    profile(M("instrumentedCaller", "()V")) {
      obj.instrumentedCaller()
    } {
      checkNotCalled(M("notCalled", "()V"))
    }
  }

  test("binary signatures") {
    val obj = new InstrumentationEnabled
    var enabled = false
    profile(M("signature", "(ZCBSIJFDLjava/lang/String;[I[Ljava/lang/String;)I")) {
      obj.signature(false, 'a', 0, 1, 2, 3L, 1.5f, 1.7, "hello",
          Array(3), Array("hello"))
    } {
      enabled = true
    }
    assert(enabled, "instrumentation is disabled on signature() - " +
        "perhaps the binary encoding of signatures is wrong")
  }

  test("instrumentation disabled") {
    val className = classOf[InstrumentationDisabled].getName.replace('.', '/')
    val obj = new InstrumentationDisabled
    var enabled = false
    profile(M(className, "instrumentedCaller", "()V")) {
      obj.instrumentedCaller()
    } {
      enabled = true
    }
    assert(!enabled, "instrumentation is enabled on InstrumentationDisabled")
  }
}
