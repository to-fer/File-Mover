package move

import java.nio.file.{Paths, Path, Files}
import file_mover.pathutil.RichPath._

class FileMover(val destParent: Path) {

  def move(pathToMove: Path) = {
    val destPath = destParent resolve pathToMove.getFileName

    if (Files.exists(destPath)) {
      val destDigest = destPath.digest
      val srcDigest = pathToMove.digest

      if (destDigest sameElements srcDigest)
        Files.delete(pathToMove)
      else {

        def insertEndingNum(num: Int): Path = {
          val fileNameWithoutExt = destPath.getFileName.toString().dropRight(destPath.extension.length)
          val fileNameWithNum = fileNameWithoutExt + "_" + num + destPath.extension
          val newPath = Paths.get(destPath.getParent.toString, fileNameWithNum)

          if (!Files.exists(newPath))
            newPath
          else
            insertEndingNum(num + 1)
        }

        val newDestPath = insertEndingNum(0)
        Files.move(pathToMove, newDestPath)
      }
    }
    else Files.move(pathToMove, destPath)
  }

}
