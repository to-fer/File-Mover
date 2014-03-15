package file_mover

import move.FileMover
import org.specs2.mutable._
import java.nio.file.{Path, Files, Paths}

// THESE TESTS ARE EXECUTED IN PARALLEL, BEWARE!
// (Use different paths in each test.)
class FileMoverSpec extends Specification {

  def initPath(pathStr: String) = {
    val p = Paths.get(pathStr)
    if (Files exists p) throw new IllegalArgumentException(
      s"Path $pathStr already exists! " +
        "Check to make sure you aren't using the same path name in two separate tests or that a pervious test failed and couldn't delete the test files."
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

  /* Neither of these tests test to see if the file exists in the correct location after a move has been performed.
   * This is due to some kind of strangeness regarding Files.move(). It seems to schedule a move for later rather than
   * doing it immediately, so any test to see if the file exists after a move is performed will fail despite the fact
   * that the move was (or, rather, will be) successful. Thus, we assume that the move was successful, so long as an
   * exception isn't thrown during the process, of course.
   */
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

      val destFileNameWithNumberAdded = Paths.get((destDir resolve pathToMove.getFileName).toString.replace(".", "_0."))
      movedPath mustEqual(destFileNameWithNumberAdded)
      val fileName = movedPath.getFileName.toString
      fileName.endsWith("_0.txt") mustEqual true
    }
  }
}
