class IOHandler {
	val mounts = mutableListOf<Mount>()
	fun mount(absolutePath: Path.Absolute, fileSystem: FileSystem) {
		if (mounts.any { it.mountPoint == absolutePath })
			return // TODO sensible error message handling
		mounts += Mount(absolutePath, fileSystem)
	}

	fun unmount(mountPoint: Path.Absolute) {
		mounts.removeAll { it.mountPoint == mountPoint }
	}

	fun <T> findMountFor(
		workingDirectory: Path.Absolute,
		path: Path,
		operation: FileSystem.(relativePath: Path) -> T
	): T {
		val absolutPath = path.toAbsolutePath(workingDirectory)
		val mount = mounts.filter {
			it.mountPoint.parts.zip(absolutPath.parts).all { (a, b) -> a == b }
		}.maxByOrNull { it.mountPoint.parts.size } ?: throw IllegalStateException("No mount present")
		return mount.fileSystem.operation(
			Path.Absolute(
				absolutPath.parts.subList(
					mount.mountPoint.parts.size,
					absolutPath.parts.size
				)
			) // TODO: unangenehm
		)
	}

	fun findINode(absolutePath: Path.Absolute): INode {
		val mount = mounts.filter {
			it.mountPoint.parts.zip(absolutePath.parts).all { (a, b) -> a == b }
		}.maxByOrNull { it.mountPoint.parts.size } ?: throw IllegalStateException("No mount present")
		val iNode = mount.fileSystem.getINode(absolutePath.relativize(mount.mountPoint))
		return when (iNode) {
			is INodeResult.File -> iNode.op
			is INodeResult.ResolveAgain -> findINode(absolutePath.resolve(iNode.relativeToOriginal))
		}
	}

	fun read(workingDirectory: Path.Absolute, path: Path): ReadResult =
		findMountFor(workingDirectory, path) { read(it) }

	fun write(workingDirectory: Path.Absolute, path: Path, data: ByteArray): Unit =
		findMountFor(workingDirectory, path) { write(it, data) }

	fun stat(workingDirectory: Path.Absolute, path: Path): Unit =
		findMountFor(workingDirectory, path) { stat(it) }
}

interface INode {
	val fs: FileSystem
}

sealed interface INodeResult {
	class File(val op: INode) : INodeResult
	class ResolveAgain(val relativeToOriginal: Path): INodeResult
}

interface FileSystem {
	fun getINode(relativePath: Path.Relative): INodeResult
	fun read(relativePath: Path): ReadResult
	fun write(path: Path, data: ByteArray): Unit // Write result
	fun stat(path: Path): Unit // TODO Stat result
}

sealed class ReadResult {
	class Success(val text: String) : ReadResult()
	object NotFound : ReadResult()
	object NoAccess : ReadResult()
}

data class Mount(
	val mountPoint: Path.Absolute,
	val fileSystem: FileSystem
)

data class Stat(
	val exists: Boolean,
	var owner: User,
	val created: Long,
	val edited: Long,
	val size: Long
)
