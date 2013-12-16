package file_mover

import _root_.move.FileMover
import org.specs2.mutable.Specification
import java.nio.file.{Path, Files, Paths}

// THESE TESTS ARE EXECUTED IN PARALLEL, BEWARE!
class FileMoverSpec extends Specification {

  def initPath(pathStr: String) = {
    val p = Paths.get(pathStr)
    if (Files exists p) throw new IllegalArgumentException(
      s"Path $pathStr already exists! Check to make sure you aren't using the same path name in two separate tests."
    )
    p
  }

  def testPath(pathStr: String) = {
    val p = initPath(pathStr)
    Files createFile p
    p
  }

  def testDir(dirStr: String) = {
    val dir = initPath(dirStr)
    Files createDirectory dir
    dir
  }

  def deleteDirectory(dir: Path) = {
    for(files <- Option(dir.toFile.listFiles);
        file <- files) file.delete()
    Files delete dir
  }

  "File Mover" should {
    "move" in {
      val destDir = testDir("move-dest-1")
      val pathToMove = testPath("move-test-1.txt")

      val mover = new FileMover(destDir)
      val movedPath = mover.move(pathToMove)

      // Cleanup
      deleteDirectory(destDir)

      movedPath mustEqual(destDir resolve pathToMove.getFileName)
    }

    "move('path that already exists')" in {
      val destDir = testDir("move-dest-2")
      val pathToMove = testPath("move-test-2.txt")
      
      val destDirContents = destDir.resolve(pathToMove.getFileName)
      if (Files notExists destDirContents) Files createFile destDirContents
      Files.write(destDirContents, "test".getBytes)

	    val mover = new FileMover(destDir)
      val movedPath = mover.move(pathToMove)

      // Cleanup
      deleteDirectory(destDir)

      val fileName = movedPath.getFileName.toString
      fileName.endsWith("_0.txt") mustEqual true
    }
  }
}
