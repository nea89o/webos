package io

import User

sealed interface CreateFileResult {
	object Created : CreateFileResult
	sealed interface Failure : CreateFileResult {
		object NoPermission : Failure
		object NoParent : Failure
		object AlreadyExists : Failure
	}
}

sealed interface WriteFileResult {
	object Written : WriteFileResult
	sealed interface Failure : WriteFileResult {
		object NoPermission : Failure
		object NotAFile : Failure
		data class NotEnoughSpace(
			val dataSize: Long,
			val spaceLeft: Long
		) : Failure
	}
}

sealed interface ReadFileResult {
	data class Read(val data: ByteArray) : ReadFileResult {
		override fun equals(other: Any?): Boolean = (other as? Read)?.let { it.data.contentEquals(this.data) } ?: false

		override fun hashCode(): Int {
			return data.contentHashCode()
		}
	}

	sealed interface Failure : ReadFileResult {
		object NoPermission : Failure
		object NotAFile : Failure
	}
}

sealed interface DeleteFileResult {
	sealed interface Failure : DeleteFileResult {
		object NoPermission : Failure
		object NotAFile : Failure
	}

	object Deleted : DeleteFileResult
}

interface FileService<INode> {
	fun getPath(iNode: INode): Path.Absolute
	fun getINode(path: Path.Absolute): INode
	fun createFile(iNode: INode, user: User): CreateFileResult
	fun createSymlink(iNode: INode, user: User, path: Path): CreateFileResult
	fun createDirectory(iNode: INode, user: User): CreateFileResult
	fun writeToFile(iNode: INode, user: User, data: ByteArray): WriteFileResult
	fun readFromFile(iNode: INode, user: User): ReadFileResult
	fun deleteFile(iNode: INode, user: User): DeleteFileResult
	fun exists(iNode: INode): Boolean
	fun isFile(iNode: INode): Boolean
	fun isDirectory(iNode: INode): Boolean
	fun isSymlink(iNode: INode): Boolean
	fun resolve(iNode: INode, fragment: String): INode
	fun resolve(iNode: INode, path: Path.Relative): INode
	fun changePermissions(iNode: INode, user: User, permissionUpdate: Map<User, Permission>)
}

data class Permission(
	val read: Boolean,
	val write: Boolean,
	val execute: Boolean
) {
	companion object {
		val default get() = Permission(read = true, write = true, execute = false)
	}
}

sealed interface PrimitiveStorageBlob {
	val permissions: MutableMap<User, Permission>

	class File(var data: ByteArray, override val permissions: MutableMap<User, Permission>) : PrimitiveStorageBlob
	class Symlink(val path: Path, override val permissions: MutableMap<User, Permission>) : PrimitiveStorageBlob
	class Directory(override val permissions: MutableMap<User, Permission>) : PrimitiveStorageBlob
}

data class PrimitiveINode internal constructor(internal val internalPath: String)
class PrimitiveFileService : FileService<PrimitiveINode> {
	private val storageBlobs = mutableMapOf<String, PrimitiveStorageBlob>(
		"/" to PrimitiveStorageBlob.Directory(mutableMapOf())
	)

	override fun getPath(iNode: PrimitiveINode): Path.Absolute = Path.of(iNode.internalPath) as Path.Absolute

	override fun getINode(path: Path.Absolute): PrimitiveINode {
		return resolve(PrimitiveINode("/"), Path.Relative(path.parts))
	}

	override fun resolve(iNode: PrimitiveINode, fragment: String): PrimitiveINode {
		if (fragment == "..") {
			val up = iNode.internalPath.substringBeforeLast('/')
			if (up.isEmpty()) return PrimitiveINode("/")
			return PrimitiveINode(up)
		}
		if (fragment.isEmpty() || fragment == ".")
			return iNode
		val blob = storageBlobs[iNode.internalPath]
		return when (blob) {
			is PrimitiveStorageBlob.Symlink -> {
				when (blob.path) {
					is Path.Absolute -> getINode(blob.path)
					is Path.Relative -> resolve(resolve(iNode, ".."), blob.path)
				}
			}
			else -> {
				PrimitiveINode(iNode.internalPath + "/" + fragment)
			}
		}
	}

	override fun resolve(iNode: PrimitiveINode, path: Path.Relative): PrimitiveINode =
		path.parts.fold(iNode) { node, fragment -> resolve(node, fragment) }

	private fun getStorageBlob(iNode: PrimitiveINode): PrimitiveStorageBlob? = storageBlobs[iNode.internalPath]

	override fun writeToFile(iNode: PrimitiveINode, user: User, data: ByteArray): WriteFileResult {
		val file = getStorageBlob(iNode) as? PrimitiveStorageBlob.File ?: return WriteFileResult.Failure.NotAFile
		if (!hasPermission(user, file) { read })
			return WriteFileResult.Failure.NoPermission
		file.data = data
		return WriteFileResult.Written
	}

	override fun readFromFile(iNode: PrimitiveINode, user: User): ReadFileResult {
		val file = getStorageBlob(iNode) as? PrimitiveStorageBlob.File ?: return ReadFileResult.Failure.NotAFile
		if (!hasPermission(user, file) { read })
			return ReadFileResult.Failure.NoPermission
		return ReadFileResult.Read(file.data)
	}

	override fun exists(iNode: PrimitiveINode): Boolean =
		getStorageBlob(iNode) != null

	override fun isFile(iNode: PrimitiveINode): Boolean =
		getStorageBlob(iNode) is PrimitiveStorageBlob.File

	override fun isDirectory(iNode: PrimitiveINode): Boolean =
		getStorageBlob(iNode) is PrimitiveStorageBlob.Directory

	override fun isSymlink(iNode: PrimitiveINode): Boolean =
		getStorageBlob(iNode) is PrimitiveStorageBlob.Symlink

	override fun changePermissions(iNode: PrimitiveINode, user: User, permissionUpdate: Map<User, Permission>) {
		val file = getStorageBlob(iNode) ?: return // TODO Results
		if (!hasPermission(user, file) { write })
			return  // TODO Results
		file.permissions.putAll(permissionUpdate)
		return // TODO Results
	}

	override fun deleteFile(iNode: PrimitiveINode, user: User): DeleteFileResult {
		val file = getStorageBlob(iNode) ?: return DeleteFileResult.Failure.NotAFile
		if (!hasPermission(user, file) { write })
			return DeleteFileResult.Failure.NoPermission
		(storageBlobs.keys.filter { it.startsWith(iNode.internalPath + "/") } + listOf(iNode.internalPath)).forEach {
			storageBlobs.remove(it)
		}
		return DeleteFileResult.Deleted
	}

	private fun hasPermission(user: User, blob: PrimitiveStorageBlob, check: Permission.() -> Boolean): Boolean {
		return user.isRoot || blob.permissions[user]?.let(check) ?: false
	}

	private fun checkCreationPreconditions(iNode: PrimitiveINode, user: User): CreateFileResult? {
		if (storageBlobs.containsKey(iNode.internalPath)) return CreateFileResult.Failure.AlreadyExists
		val parent = getStorageBlob(resolve(iNode, ".."))
		if (parent !is PrimitiveStorageBlob.Directory) return CreateFileResult.Failure.NoParent
		if (!hasPermission(user, parent) { write }) return CreateFileResult.Failure.NoPermission
		return null
	}

	override fun createFile(iNode: PrimitiveINode, user: User): CreateFileResult {
		val preconditions = checkCreationPreconditions(iNode, user)
		if (preconditions != null) return preconditions
		storageBlobs[iNode.internalPath] = PrimitiveStorageBlob.File(byteArrayOf(), mutableMapOf(user to Permission.default))
		return CreateFileResult.Created
	}

	override fun createSymlink(iNode: PrimitiveINode, user: User, path: Path): CreateFileResult {
		val preconditions = checkCreationPreconditions(iNode, user)
		if (preconditions != null) return preconditions
		storageBlobs[iNode.internalPath] = PrimitiveStorageBlob.Symlink(path, mutableMapOf(user to Permission.default))
		return CreateFileResult.Created
	}

	override fun createDirectory(iNode: PrimitiveINode, user: User): CreateFileResult {
		val preconditions = checkCreationPreconditions(iNode, user)
		if (preconditions != null) return preconditions
		storageBlobs[iNode.internalPath] = PrimitiveStorageBlob.Directory(mutableMapOf(user to Permission.default))
		return CreateFileResult.Created
	}

}
