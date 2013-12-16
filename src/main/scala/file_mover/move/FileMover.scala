package move

import java.nio.file.{Paths, Path, Files}
import file_mover.pathutil.RichPath._

class FileMover(val destParent: Path) {

  def move(pathToMove: Path): Path = {
	if (Files notExists pathToMove) throw new IllegalArgumentException("That path doesn't exist!")
	
    val tentativeDestPath = destParent resolve pathToMove.getFileName
    val destPath =
    if (Files.exists(tentativeDestPath)) {
      val destDigest = tentativeDestPath.digest
      val srcDigest = pathToMove.digest

      if (destDigest sameElements srcDigest) {
        Files.delete(tentativeDestPath)
        tentativeDestPath
      }
      else {
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
