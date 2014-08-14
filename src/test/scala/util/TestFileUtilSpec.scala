package util

import java.io.File

import org.specs2.mutable.Specification

class TestFileUtilSpec extends Specification {
  "TestFileUtil" should {
    "delete('not a directory')" in {
      val file = new File("empty-file.txt")
      file.createNewFile()
      TestFileUtil.delete(file)
      val fileExists = file.exists

      file.delete()

      fileExists mustEqual false
    }

    "delete('empty directory')" in {
      val emptyDir = new File("empty-dir")
      emptyDir.mkdir()
      TestFileUtil.delete(emptyDir)
      val dirExists = emptyDir.exists

      if (dirExists)
        emptyDir.delete()

      dirExists mustEqual false
    }

    "delete('directory containing other directories')" in {
      val topDir = new File("top-dir")
      val topDirFile = new File(topDir, "top-dir-file")
      val subDir = new File(topDir, "sub-dir")
      val subDirFile = new File(subDir, "sub-dir-file")

      topDir.mkdir()
      topDirFile.createNewFile()
      subDir.mkdir()
      subDirFile.createNewFile()

      TestFileUtil.delete(topDir)

      val anyExist = topDir.exists || topDirFile.exists || subDir.exists || subDirFile.exists

      if (anyExist) {
        topDir.delete()
        topDirFile.delete()
        subDir.delete()
        subDirFile.delete()
      }

      anyExist mustEqual false
    }
  }
}
