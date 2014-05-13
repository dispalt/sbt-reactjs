
import org.scalatest._

class FilterOutClassTest extends FunSuite with Matchers {

  test("filter out classpaths") {
    val unManagedDirs = Seq("src/foo", "src", "bar", "bar/foo", "bar/bar/foo").sortWith {
      case (lhs, rhs) => lhs.size < rhs.size
    }

    val filtered = unManagedDirs.foldLeft(Seq.empty[String]) {
      (files, currentFile) =>
        if (files.count {
          file =>
            currentFile.startsWith(file)
        } == 0) {
          files ++ Seq(currentFile)
        } else {
          files
        }
    }
    filtered.size should be (2)
  }

}
