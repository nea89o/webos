import io.IOHandler
import io.Path
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

	var currentUser: User? = null

	private var _workingDirectory: Path.Absolute? = null

	var workingDirectory: Path.Absolute
		get() = _workingDirectory ?: currentUser?.homeDirectory ?: Path.root
		set(value) {
			_workingDirectory = value
		}

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
	val files = IOHandler()
	fun registerConsole(element: Element) {
		_consoles.add(Console(this, element))
	}
}

data class User(
	val name: String,
	val homeDirectory: Path.Absolute
)

