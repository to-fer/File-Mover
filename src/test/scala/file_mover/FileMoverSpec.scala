package file_mover

import file.FileUtil
import org.specs2.mutable._
import java.nio.file.{Path, Files, Paths}
import org.specs2.matcher.MatchResult

// THESE TESTS ARE EXECUTED IN PARALLEL, BEWARE!
// Use different paths in each test to avoid problems.
class FileMoverSpec extends Specification {

  def deleteDirectory(dir: Path) = {
    for(files <- Option(dir.toFile.listFiles);
        file <- files) file.delete()
    Files deleteIfExists dir
  }

  def createFileWithContents(filePath: Path, fileContents: String) = {
    if (Files notExists filePath) Files createFile filePath
    Files.write(filePath, fileContents.getBytes)
  }

  "File Mover" should {
    "file" in {
      fileMoverTest("move-dest-1", "move-source-1.txt") {
        (destDir, pathToMove) => {
          val mover = new FileMover(destDir)
          val movedPath = mover.move(pathToMove)

          movedPath mustEqual(destDir resolve pathToMove.getFileName)
          Files.exists(movedPath) mustEqual true
        }
      }
    }

    "move('path to file that exists in destination and the already existing file's contents aren't identical to its')" in {
      fileMoverTest("move-dest-2", "move-source-2.txt") {
        (destDir, pathToMove) => {
          val destDirContents = destDir.resolve(pathToMove.getFileName)
          createFileWithContents(destDirContents, "test")

          val mover = new FileMover(destDir)
          val movedPath = mover.move(pathToMove)

          val destFileNameWithNumberAdded = Paths.get((destDir resolve pathToMove.getFileName).toString.replace(".", "_0."))
          movedPath mustEqual(destFileNameWithNumberAdded)
          val fileName = movedPath.getFileName.toString
          fileName.endsWith("_0.txt") mustEqual true
          Files.exists(movedPath) mustEqual true
        }
      }
    }

    "move('path to file that exists in destination and the already existing file's contents are identical to its')" in {
      fileMoverTest("duplicate-move-dest", "duplicate-move-source.txt") {
        (destDir, pathToMove) => {
          val fileContents = "test file contents"
          Files.write(pathToMove, fileContents.getBytes)

          val destDirContents = destDir.resolve(pathToMove.getFileName)
          createFileWithContents(destDirContents, fileContents)

          val mover = new FileMover(destDir)
          val movedPath = mover.move(pathToMove)

          movedPath.toString.contains("_") mustEqual false
          Files.exists(movedPath) mustEqual true
        }
      }
    }

    /*
     * Used to guarantee that the paths involved in a test are deleted before and after each test.
     * Also acts as a convenience method for initializing the paths to be used in the test.
     */
    def fileMoverTest(dirPath: String, filePath: String)(testBlock: (Path, Path) => MatchResult[_]): MatchResult[_] = {
      val testDestDir = Paths.get(dirPath)
      deleteDirectory(testDestDir)
      Files createDirectory testDestDir

      val testFile = Paths.get(filePath)
      Files deleteIfExists testFile
      Files createFile testFile

      val matchResult = testBlock(testDestDir, testFile)

      deleteDirectory(testDestDir)

      matchResult
    }
  }
}
