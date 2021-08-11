import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.asList

fun main() {
	console.log("Hello from Kotlin")
	val webos = WebOS()
	document.body?.addEventListener("load", {
		document.body?.querySelectorAll(".webosconsole")?.asList()?.forEach {
			if (it !is Element) return@forEach
			webos.registerConsole(it)
		}
	})
}

data class CharacterRun(val text: String, val color: String)

abstract class Activity(val console: Console) {
	abstract fun render(columns: Int, rows: Int): List<List<CharacterRun>>
}

class Console(val os: WebOS, val renderElement: Element?) {
	val isVirtual get() = renderElement == null
	val activityStack = ArrayDeque<Activity>()

	var columns: Int = 80
	var rows: Int = 46

	var shouldRerender = true

	lateinit var currentUser: User

	private var _workingDirectory: AbsolutPath? = null

	var workingDirectory: AbsolutPath
		get() {
			if (_workingDirectory == null) _workingDirectory = currentUser.homeDirectory; return _workingDirectory!!
		}
		set(value) { _workingDirectory = value }

	fun openActivity(activity: Activity) {
		activityStack.addLast(activity)
		invalidateRender()
	}

	fun render() {
		if (renderElement == null) return
		if (!shouldRerender) return
		shouldRerender = false
		activityStack.last()
	}

	fun invalidateRender() {
		shouldRerender = true
		window.requestAnimationFrame { render() }
	}

	fun resize(newColumns: Int, newRows: Int) {
		invalidateRender()
	}

	// TODO: Handle resizes of the renderElement

}

class WebOS {
	private val _consoles = mutableListOf<Console>()
	val consoles get() = _consoles.toList()
	val IOHandler = IOHandler()
	fun registerConsole(element: Element) {
		_consoles.add(Console(this, element))
	}
}

data class User(
	val name: String,
	val homeDirectory: AbsolutPath
)

data class RelativePath internal constructor(override val parts: List<String>) : Path {
	override fun toAbsolutPath(context: Console): AbsolutPath {
		val result = mutableListOf<String>()
		for (part in parts) {
			when (part) {
				"." -> result.addAll(context.workingDirectory.parts)
				"~" -> result.addAll(context.currentUser.homeDirectory.parts)
				".." -> { result.removeLast() }
				else -> result.add(part)
			}
		}
		return AbsolutPath(result)
	}
}

data class AbsolutPath internal constructor(override val parts: List<String>) : Path {
	override fun toAbsolutPath(context: Console): AbsolutPath = this
}

sealed interface Path {
	val parts: List<String>
	fun toAbsolutPath(context: Console): AbsolutPath
	companion object {
		fun of(string: String): Path {
			val isAbsolut = string.first() == '/'
			val parts = string.split("/")
			val aparts = parts.subList(1, parts.size)
			if (aparts.contains(".") || aparts.contains("~"))
				throw IllegalArgumentException("'.' and '~' are only allowed at the beginning of a relative path")
			if (!isAbsolut) return RelativePath(parts)
			if (aparts.contains("..")) throw IllegalArgumentException("'..' is only allowed in a relative path")
			return AbsolutPath(aparts)
		}
	}
}

class FileSystem {
	fun read(relativePath: Path): ReadResult = TODO()
	fun write(path: Path, data: ByteArray): Unit = TODO()
	fun stat(path: Path): Unit = TODO()
}

class IOHandler {
	val mounts = mutableListOf<Mount>()
	fun mount(absolutPath: AbsolutPath, fileSystem: FileSystem) {
		mounts += Mount(absolutPath, fileSystem)
	}
	fun unmount(mountPoint: AbsolutPath) {
		mounts.removeAll { it.mountPoint == mountPoint }
	}
	fun <T> findMountFor(context: Console, path: Path, operation: FileSystem.(relativePath: Path) -> T): T {
		val absolutPath = path.toAbsolutPath(context)
		val mount = mounts.filter {
			var result = true
			it.mountPoint.parts.forEachIndexed { index, part -> if (absolutPath.parts[index] != part) {
				result = false
			} }
			result
		}.maxByOrNull { it.mountPoint.parts.size} ?: throw IllegalStateException("No mount present")
		return mount.fileSystem.operation(
			AbsolutPath(absolutPath.parts.subList(mount.mountPoint.parts.size, absolutPath.parts.size))
		)
	}

	fun read(context: Console, path: Path): ReadResult = findMountFor(context, path) { read(it) }
	fun write(context: Console, path: Path, data: ByteArray): Unit = findMountFor(context, path) { write(it, data) }
	fun stat(context: Console, path: Path): Unit = findMountFor(context, path) { stat(it) }
}

sealed class ReadResult {
	class Success(val text: String) : ReadResult()
	object NotFound
	object NoAccess
}
data class Mount (
	val mountPoint: AbsolutPath,
	val fileSystem: FileSystem
)

data class Stat(
	val exists: Boolean,
	var owner: User,
	val created: Long,
	val edited: Long,
	val size: Long
)
