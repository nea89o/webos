package io

import User
import io.kotest.core.spec.style.FunSpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileServiceTest : FunSpec({
	generateTests("Primitive", ::PrimitiveFileService)
})

fun <INode> FunSpec.generateTests(name: String, provider: () -> FileService<INode>) {
	val aPath = Path.of("/a") as Path.Absolute
	val homePath = Path.of("/roothome") as Path.Absolute
	val dataA = "a".encodeToByteArray()
	val rootUser = User("root", homePath, true)
	test("$name: root inode exists") {
		val fileService = provider()
		val rootInode = fileService.getINode(Path.root)
		assertTrue(fileService.exists(rootInode))
	}
	test("$name: CRUD a file") {
		val fileService = provider()
		val aInode = fileService.getINode(aPath)
		assertFalse(fileService.exists(aInode))
		assertEquals(CreateFileResult.Created, CreateFileResult.Created)
		assertEquals(fileService.createFile(aInode, rootUser), CreateFileResult.Created)
		assertTrue(fileService.exists(aInode))
		assertTrue(fileService.isFile(aInode))
		assertFalse(fileService.isSymlink(aInode))
		assertFalse(fileService.isDirectory(aInode))
		assertEquals(fileService.readFromFile(aInode, rootUser), ReadFileResult.Read(ByteArray(0)))
		assertEquals(fileService.writeToFile(aInode, rootUser, dataA), WriteFileResult.Written)
		assertEquals(fileService.readFromFile(aInode, rootUser), ReadFileResult.Read(dataA))
		assertTrue(fileService.isFile(aInode))
		assertEquals(fileService.deleteFile(aInode, rootUser), DeleteFileResult.Deleted)
		assertFalse(fileService.isFile(aInode))
		assertFalse(fileService.exists(aInode))
	}

}
