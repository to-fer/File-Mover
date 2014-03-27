package pathutil

import java.nio.file.{Files, Path}
import java.security.{DigestInputStream, MessageDigest}
import scala.concurrent.{Future, future, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.util.concurrent.{TimeoutException, TimeUnit}
import scala.util.parsing.combinator.Parsers
import scala.util.Failure

object RichPath {
  implicit def path2RichPath(p: Path) = new RichPath(p)
}

class RichPath(delegatePath: Path) {

  def extension = {
    val fileName = delegatePath.getFileName.toString
    fileName.substring(fileName.indexOf(".") + 1)
  }

  def digest: List[Byte] = {
    val md5Digest = MessageDigest.getInstance("MD5")
    val inputStream = Files.newInputStream(delegatePath)
    val digestStream = new DigestInputStream(inputStream, md5Digest)

    while(digestStream.read(new Array[Byte](1 << 16)) != -1){}

    val messageDigest = digestStream.getMessageDigest()
    digestStream.close()
    messageDigest.digest.toList
  }

  def downloadFinish(timeout: Duration = Duration.create(15, TimeUnit.MINUTES)): Future[Path] = future {

    val blockDuration = Duration.create(50, TimeUnit.MILLISECONDS)

    def blockTillDownloadFinish(prevDigest: List[Byte], timeLeft: Duration): Unit = {
      if (timeLeft > Duration.Zero) {
        /*
       * Sleep for a short time to make sure to give the file time to download a bit more before reading
       * from it again. Otherwise we might read so fast that it will appear to not be downloading when that isn't the case.
       */
        Thread.sleep(blockDuration.length) // DO NOT REMOVE THIS. I'M WARNING YOU!
        val curDigest = digest

        /*
         * If the digests differ then the file is still downloading. If the file's size is 0 then it has not yet started to
         * download, so we must wait longer.
         */
        if (!(curDigest sameElements prevDigest) || Files.size(delegatePath) == 0) blockTillDownloadFinish(curDigest, timeout - blockDuration)
      }
      else throw new TimeoutException("File took too long to download.")
    }

    blockTillDownloadFinish(List[Byte](0), timeout)
    delegatePath
  }

}
