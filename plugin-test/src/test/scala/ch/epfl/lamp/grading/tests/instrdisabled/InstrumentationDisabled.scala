package ch.epfl.lamp.grading.tests.instrdisabled

class InstrumentationDisabled {
  def instrumentedCallee(): Unit = {
    assert(true)
  }

  def notCalled(): Unit = {
    assert(false)
  }

  def instrumentedCaller(): Unit = {
    instrumentedCallee()
  }
}
