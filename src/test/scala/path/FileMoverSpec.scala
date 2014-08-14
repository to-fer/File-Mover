package path

import java.nio.file.{Files, Paths}

import com.twitter.logging.Logger
import org.specs2.mutable.Specification
import util.TestFileUtil.delete

class FileMoverSpec extends Specification {
  "FileMover" should {
    "onEvent('path that exists and matches move rules')" in {
      val dir = Paths.get("onEventTest1")
      delete(dir)
      Files.createDirectory(dir)

      val existingPath = dir resolve "existing-path.txt"
      Files.createFile(existingPath)
      Files.write(existingPath, "Arbitrary bytes!".getBytes)

      val destDir = dir resolve "dest"
      Files.createDirectory(destDir)

      val fileMover = new FileMover(Logger.get(getClass), List("txt"), destDir)
      fileMover.onEvent(existingPath)

      // Wait for move to occur.
      Thread.sleep(1000)
      val pathMoved = !Files.exists(dir.resolve("existing-path.txt")) && Files.exists(destDir.resolve("existing-path.txt"))
      delete(dir)

      pathMoved mustEqual true
    }

    "onEvent('path that exists and doesn't match move rules')" in {
      val dir = Paths.get("onEventTest2")
      delete(dir)
      Files.createDirectory(dir)

      val notMatchingPath = dir resolve "not-matching-path.png"
      Files.createFile(notMatchingPath)
      Files.write(notMatchingPath, "Arbitrary bytes!".getBytes)

      val destDir = dir resolve "dest"
      Files.createDirectory(destDir)

      val fileMover = new FileMover(Logger.get(getClass), List("txt", "gif"), destDir)
      fileMover.onEvent(notMatchingPath)

      // Give move time to occur
      Thread.sleep(1000)
      val pathMoved = !Files.exists(dir.resolve("not-matching-path.png")) && Files.exists(destDir.resolve("not-matching-path.png"))

      delete(dir)
      
      pathMoved mustEqual false
    }

    // TODO add tests for moving a file that's downloading when onEvent() is called
  }
}
