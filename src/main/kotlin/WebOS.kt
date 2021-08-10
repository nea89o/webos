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
	val fileSystem = FileSystem()
	fun registerConsole(element: Element) {
		_consoles.add(Console(this, element))
	}
}

interface User {
	val name: String
	val homeDirectory: Path
}

data class Path private constructor(val parts: List<String>) {
	companion object {
		fun of(text: String) =
			Path(text.split("/"))
		fun of(vararg parts: String): Path {
			if(parts.any { it.contains("/") })
				throw IllegalArgumentException("Path parts cannot contain forward slashes")
			return Path(parts.toList())
		}
	}
}

class FileSystem {
	val mounts = mutableMapOf<Path, Mount>()
	fun mount(path: Path, mount:Mount) {
		mounts[path] = mount
	}
	fun <T>findMountFor(path: Path, operation: Mount.(relativePath: Path) -> T) :T{
		val (mountPath, mount) = mounts.filterKeys {
			// path.startsWith(it)
			true
		}.maxByOrNull { it.key.parts.size} ?: throw IllegalStateException("No mount present")
		return mount.operation(mountPath.relativize(path))
	}

	fun read(path: Path): ReadResult = findMountFor(path) { read(it) }
	fun write(path: Path, data: ByteArray): Unit = TODO()
	fun stat(path: Path): Unit = TODO()
}

sealed class ReadResult {
	class Success(val text: String) : ReadResult()
	object NotFound
	object NoAccess
}
interface Mount {
	fun read(relativePath: Path): ReadResult = TODO()
	fun write(path: Path, data: ByteArray): Unit = TODO()
	fun stat(path: Path): Unit = TODO()
}

data class Stat(
	val exists: Boolean,
	var owner: User,
	val created: Long,
	val edited: Long,
	val size: Long
)
