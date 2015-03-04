InstrAgent
==========

Hacky steps to try this out
---------------------------

- From this directory, do `sbt package`

- Go back to the parent directory and run sbt with the java option
  `-J-javaagent:instragent/target/scala-2.9.2/instragent_2.9.2-0.1.jar`

- Now you can run `test` from this instrumented sbt, and the `demo.InstrDemoSuite` tests should pass.

How it works
------------

We specify what to monitor in the `ASMTransformer`. For now, I am just
instrumenting everything in the `demo` package.

