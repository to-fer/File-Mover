package path

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable.Specification
import path.RichPath.path2RichPath

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RichPathSpec extends Specification {
  "RichPath" should {

    "extension" in {
      val extention = "txt"
      val extensionPath = Paths.get(s"test.$extention")
      extensionPath.extension mustEqual extention
    }

    "digests of two files with differing contents are different" in {
      val firstTestPath = Paths.get("test.txt")
      if (!Files.exists(firstTestPath))
        Files.createFile(firstTestPath)
      val firstTestFileContents = "test"
      Files.write(firstTestPath, firstTestFileContents.getBytes)

      val secondTestPath = Paths.get("different-test.txt")
      if (!Files.exists(secondTestPath))
        Files.createFile(secondTestPath)
      Files.write(secondTestPath, (firstTestFileContents + " I'm different!").getBytes)

      val firstTestFileDigest = firstTestPath.digest
      val secondTestFileDigest = secondTestPath.digest

      Files delete firstTestPath
      Files delete secondTestPath

      firstTestFileDigest mustNotEqual secondTestFileDigest
    }

    "digests of two identical files are equal" in {
      val firstTestPath = Paths.get("identical-test-1.txt")
      if (!Files.exists(firstTestPath))
        Files.createFile(firstTestPath)
      val firstTestFileContents = "test"
      Files.write(firstTestPath, firstTestFileContents.getBytes)

      val secondTestPath = Paths.get("identical-test-2.txt")
      if (!Files.exists(secondTestPath))
        Files.createFile(secondTestPath)
      Files.write(secondTestPath, firstTestFileContents.getBytes)

      val firstTestFileDigest = firstTestPath.digest
      val secondTestFileDigest = secondTestPath.digest

      Files delete firstTestPath
      Files delete secondTestPath

      firstTestFileDigest mustEqual secondTestFileDigest
    }

    "downloadFinish" in {
      val googleFile = new File("google-file.html")
      val googleUrl = url("http://www.google.com")
      val googleHtml = Http(googleUrl > as.File(googleFile))
      val waitDuration = Duration.create(10, TimeUnit.SECONDS)
      val downloadFinishFuture = Future { googleFile.toPath.downloadFinish(waitDuration) }
      Await.ready(downloadFinishFuture, waitDuration)

      val fileExists = googleFile.exists
      googleFile.delete() // Cleanup

      fileExists mustEqual true
    }

  }
}
