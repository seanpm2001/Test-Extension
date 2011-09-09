import org.nlogo.api.{ CommandTask, ReporterTask, Equality, LogoException, Context }

// main object
object Tester {
  val tests = collection.mutable.ListBuffer[Test]()
  var setup: Option[CommandTask] = None
  var results: Iterable[TestResult] = Nil
  def addTest(t: Test) { tests += t }
  def run(context: Context) {
    results = tests.map(_.run(context))
  }
  def clear() {
    tests.clear()
    results = Nil
    setup = None
  }
  def successes = results.collect{ case t: Pass => t }
  def failures = results.collect{ case t: Failure => t }
  def errors = results.collect{ case t: Error => t }
  def simpleSummary =
    "Test Run " +
    (if(failures.isEmpty && errors.isEmpty) "Passed"
     else "Failed") +
    " - Total " + results.size +
    ", Failed " + failures.size +
    ", Errors " + errors.size +
    ", Passed " + successes.size
  def summary =
    simpleSummary +
    (if(failures.nonEmpty) ("\n"+failures.map(_.report).mkString("\n")) else "") +
    (if(errors.nonEmpty) ("\n"+errors.map(_.report).mkString("\n")) else "")
  def details =
    simpleSummary + "\n" + results.map(_.report).mkString("\n")
}

// helpers
case class Test(name: String, command: CommandTask, reporter: ReporterTask, expected: AnyRef) {
  import org.nlogo.nvm
  def run(c: Context): TestResult = {
    val context = c.asInstanceOf[nvm.ExtensionContext].nvmContext
    val workspace = c.asInstanceOf[nvm.ExtensionContext].workspace
    try {
      workspace.clearAll()
      for(setup <- Tester.setup)
        setup.asInstanceOf[nvm.CommandTask].perform(context, Array())
      command.asInstanceOf[nvm.CommandTask].perform(context, Array())
      val actual = reporter.asInstanceOf[nvm.ReporterTask].report(context, Array())
      if(Equality.equals(actual, expected))
        Pass(this)
      else
        Failure(this, "expected: " + expected + " but got: " + actual)
    }
    catch {
      case e: LogoException =>
        Error(this, e)
    }
  }
}

sealed trait TestResult {
  val test: Test
  def report: String
}
case class Pass(test: Test) extends TestResult {
  def report: String = "PASS '" + test.name + "'"
}
case class Failure(test: Test, reason: String) extends TestResult {
  def report: String = "FAIL '" + test.name + "' " + reason
}
case class Error(test: Test, e: LogoException) extends TestResult {
  def report: String = "ERROR '" + test.name + "' " + e.getMessage
}
