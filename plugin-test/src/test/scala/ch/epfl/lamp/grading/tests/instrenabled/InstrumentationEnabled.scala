package ch.epfl.lamp.grading.tests.instrenabled

class InstrumentationEnabled {
  def instrumentedCallee(): Unit = {
    assert(true)
  }

  def notCalled(): Unit = {
    assert(false)
  }

  def instrumentedCaller(): Unit = {
    instrumentedCallee()
  }

  def signature(z: Boolean, c: Char, b: Byte, s: Short, i: Int, j: Long,
      f: Float, d: Double, str: String, ai: Array[Int], astr: Array[String]): Int = {
    42
  }
}
