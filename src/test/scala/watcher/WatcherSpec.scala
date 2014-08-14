
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit

import org.specs2.mutable._
import watch.Watcher._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, future}

class WatcherSpec extends Specification {

  // Directory used for each test should be different, as these tests will be executed in parallel.
  "Watcher" should {
    "watch" in {
      val (watchDir, watchDirContents) = prepareWatchDirectory("watcher_test_dir_2")

      var eventPaths = List[Path]()

      val watchFuture = future {
        watch(watchDir, eventPaths.length == watchDirContents.length) {
          case Created(p) => eventPaths = p :: eventPaths
        }
      }

      // static watch method takes some time to start watching, so we have to wait a bit before making files.
      Thread.sleep(1000)
      watchDirContents foreach { p => Files.createFile(p) }

      // wait for the watch method to finish handling all events.
      Await.ready(watchFuture, Duration(5, TimeUnit.SECONDS))

      // cleanup
      deleteWatchDirectory(watchDir, watchDirContents)

      eventPaths.reverse mustEqual watchDirContents
    }
  }

  def prepareWatchDirectory(watchDirStr: String): (Path, List[Path]) = {
    val watchDir = Paths.get(watchDirStr)
    val watchDirContents = List(
      watchDir resolve "test1.txt",
      watchDir resolve "test2.txt",
      watchDir resolve "test3.txt",
      watchDir resolve "test4.txt"
    )

    if (Files exists watchDir) deleteWatchDirectory(watchDir, watchDirContents)
    Files.createDirectory(watchDir)

    (watchDir, watchDirContents)
  }

  def deleteWatchDirectory(watchDir: Path, contents: List[Path]): Unit = {
    contents foreach { Files.deleteIfExists }
    Files.deleteIfExists(watchDir)
  }
}
