package move

import java.nio.file.{Paths, Path, Files}
import pathutil.RichPath._

class FileMover(val destParent: Path) {

  def move(pathToMove: Path): Path = {
	if (Files notExists pathToMove) throw new IllegalArgumentException("That path doesn't exist!")
	
    val tentativeDestPath = destParent resolve pathToMove.getFileName
    val destPath =
    if (Files.exists(tentativeDestPath)) {
      // Conflict! The destination file already exists.
      // Test to see if the two files have identical contents (and are thus the same file).
      val destDigest = tentativeDestPath.digest
      val srcDigest = pathToMove.digest
      if (destDigest sameElements srcDigest) {
        Files.delete(tentativeDestPath)
        tentativeDestPath
      }
      else {
        // The two files aren't the same file, so we can just append an underscore and number to the end of the file to
        // be moved and move it to eliminate the path-conflict and move on (;D).
        def insertEndingNum(num: Int): Path = {
          val fileName = pathToMove.getFileName.toString
          val fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."))
          val fileNameWithNum = fileNameWithoutExt + "_" + num + "." + tentativeDestPath.extension
          val newPath = Paths.get(tentativeDestPath.getParent.toString, fileNameWithNum)

          if (Files notExists newPath)
            newPath
          else
            insertEndingNum(num + 1)
        }

        val newDestPath = insertEndingNum(0)
        newDestPath
      }

    }
    else tentativeDestPath

    Files.move(pathToMove, destPath)
  }

}
