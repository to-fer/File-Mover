package file_mover.pathutil

import java.nio.file.{Files, Path}
import java.security.{DigestInputStream, MessageDigest}
import scala.concurrent.{Future, future, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.collection.convert._
import java.io.FileInputStream

object RichPath {
  implicit def path2RichPath(p: Path) = new RichPath(p)
}

class RichPath(p: Path) {

  def extension = {
    val name = p.getFileName.toString
    name.substring(name.indexOf(".") + 1)
  }

  def digest: List[Byte] = {
    val digest = MessageDigest.getInstance("MD5")
    val is = Files.newInputStream(p)
    val digestStream = new DigestInputStream(is, digest)

    while(digestStream.read(new Array[Byte](1 << 16)) != -1){}

    val md = digestStream.getMessageDigest()
    digestStream.close()
    md.digest.toList
  }

  def downloadFinish: Future[Path] = future {

    def blockTillDownloadFinish(prevDigest: List[Byte]): Unit = {

      /* Sleep for a short time to make sure to give the file time to download a bit more before reading
         from it again. Otherwise we might read so fast that it will appear to not be downloading when that isn't the case.*/
      Thread.sleep(50) // DO NOT REMOVE THIS. I'M WARNING YOU!
      val curDigest = digest

      /* If the digests differ then the file is still downloading. If the file's size is 0 then it has not yet started to
       * download, so we must wait longer.
       */
      if (!(curDigest sameElements prevDigest) || Files.size(p) == 0) blockTillDownloadFinish(curDigest)
    }

    blockTillDownloadFinish(List[Byte](0))

    p
  }

}
