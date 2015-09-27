package ch.epfl.lamp.grading.plugin

import scala.tools.nsc
import nsc._

abstract class Instrumentation
    extends plugins.PluginComponent with transform.Transform {

  val instrumentationOptions: Instrumentation.Options

  import global._
  import definitions._
  import rootMirror._

  val phaseName = "gradinginstr"

  override def newPhase(p: nsc.Phase): StdPhase = new InstrumentationPhase(p)

  private class InstrumentationPhase(prev: nsc.Phase) extends Phase(prev) {
    override def name: String = phaseName
    override def description: String = "Instrumentation for scala-grading"
  }

  override protected def newTransformer(unit: CompilationUnit): Transformer =
    new InstrumentationTransformer(unit)

  private class InstrumentationTransformer(unit: CompilationUnit)
      extends Transformer {

    private val prefixesWithDots =
      instrumentationOptions.instrumentedClassPrefixes.map(_ + ".")

    private lazy val ProfilerModule =
      getRequiredModule("ch.epfl.lamp.grading.instrumented.Profiler")
    private lazy val ProfilerModuleClass =
      ProfilerModule.moduleClass
    private lazy val methodCalledSym =
      ProfilerModuleClass.info.member(newTermName("methodCalled")).asMethod

    override def transform(tree: Tree): Tree = tree match {
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        val sym = tree.symbol
        if (!prefixesWithDots.exists(sym.fullName.startsWith(_)) ||
            sym.isConstructor) {
          // don't instrument
          super.transform(tree)
        } else {
          // do instrument
          val className = sym.owner.fullName('/')
          val methodName = sym.name.toString
          val methodDescriptor = computeMethodDescriptor(sym.tpe)
          val newRhs = typer.typed {
            atPos(tree.pos) {
              Block(
                  gen.mkMethodCall(methodCalledSym, List(
                      Literal(Constant(className)),
                      Literal(Constant(methodName)),
                      Literal(Constant(methodDescriptor)))),
                  rhs)
            }
          }
          treeCopy.DefDef(tree, mods, name, tparams, vparamss, tpt, newRhs)
        }

      case _ =>
        super.transform(tree)
    }

    private def computeMethodDescriptor(tpe: Type): String = {
      val paramTypeNames = tpe.params map (p => internalName(p.tpe))
      val resultTypeName = internalName(tpe.resultType)
      paramTypeNames.mkString("(", "", ")") + resultTypeName
    }

    /** Computes the internal name for a type. */
    private def internalName(tpe: Type): String = tpe.normalize match {
      case ThisType(ArrayClass)            => internalClassName(ObjectClass)
      case ThisType(sym)                   => internalClassName(sym)
      case SingleType(_, sym)              => internalClassName(sym)
      case ConstantType(_)                 => internalName(tpe.underlying)
      case TypeRef(_, sym, args)           => internalName(sym, args)
      case ClassInfoType(_, _, ArrayClass) => abort("ClassInfoType to ArrayClass!")
      case ClassInfoType(_, _, sym)        => internalClassName(sym)
      case tpe: AnnotatedType              => internalName(tpe.underlying)

      case norm =>
        abort("Unknown type: %s, %s [%s, %s] TypeRef? %s".format(
            tpe, norm, tpe.getClass, norm.getClass, tpe.isInstanceOf[TypeRef]))
    }

    private def internalName(sym: Symbol, targs: List[Type]): String = sym match {
      case ArrayClass => "[" + internalName(targs.head)
      case _          => internalClassName(sym)
    }

    private def internalClassName(sym: Symbol): String = sym match {
      case UnitClass    => "V"
      case BooleanClass => "Z"
      case CharClass    => "C"
      case ByteClass    => "B"
      case ShortClass   => "S"
      case IntClass     => "I"
      case LongClass    => "J"
      case FloatClass   => "F"
      case DoubleClass  => "D"

      case _ =>
        "L" + sym.fullName('/') + (if (sym.isModuleClass) "$" else "") + ";"
    }
  }
}

object Instrumentation {
  trait Options {
    def instrumentedClassPrefixes: Set[String]
  }
}
