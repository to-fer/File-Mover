package util

import java.io.File
import java.nio.file.{Path, Paths}

object TestFileUtil {
  def delete(dir: String): Unit = {
    delete(Paths.get(dir))
  }

  def delete(dir: Path): Unit = {
    delete(dir.toFile)
  }

  def delete(dir: File): Unit = {
    val dirContents = dir.listFiles
    if (dirContents != null && dirContents.length > 0) {
      val (files, directories) = dirContents.partition(_.isFile)
      files.foreach(_.delete())
      directories.foreach(delete)
    }
    dir.delete()
  }
}
