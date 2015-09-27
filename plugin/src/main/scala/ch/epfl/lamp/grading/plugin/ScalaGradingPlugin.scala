package ch.epfl.lamp.grading.plugin

import scala.tools.nsc._
import scala.tools.nsc.plugins.{
  Plugin => NscPlugin, PluginComponent => NscPluginComponent
}

class ScalaGradingPlugin(val global: Global) extends NscPlugin {
  import global._

  val name = "scalagrading"
  val description = "Instrumentation for scala-grading"
  val components = {
    if (global.forScaladoc)
      List[NscPluginComponent]()
    else
      List[NscPluginComponent](InstrumentationComponent)
  }

  object scalaGradingOptions extends Instrumentation.Options {
    var instrumentedClassPrefixes: Set[String] = Set.empty
  }

  object InstrumentationComponent extends {
    val global: ScalaGradingPlugin.this.global.type = ScalaGradingPlugin.this.global
    val instrumentationOptions = ScalaGradingPlugin.this.scalaGradingOptions
    override val runsAfter = List("cleanup", "delambdafy")
    override val runsBefore = List("icode", "bcode")
  } with Instrumentation

  override def processOptions(options: List[String],
      error: String => Unit): Unit = {
    import scalaGradingOptions._

    for (option <- options) {
      if (option.startsWith("instrumentClassPrefix:")) {
        instrumentedClassPrefixes +=
          option.stripPrefix("instrumentClassPrefix:")
      } else {
        error("Option not understood: " + option)
      }
    }
  }

  override val optionsHelp: Option[String] = Some(s"""
      |  -P:$name:instrumentClassPrefix:somepack.path
      |     Instrument all classes in the somepack.path package
      """.stripMargin)

}
