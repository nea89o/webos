import io.FileService
import io.Path
import io.PrimitiveFileService
import io.PrimitiveINode
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

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

class PrimitiveShell(override val console: Console) : Shell {

	var currentInput = ""

	override fun start() {
		console.print("${console.currentUser!!.name}@${console.os.hostname} ${console.workingDirectory} > ")
	}

	override fun handleKeyPress(key: Key) = when (key) {
		is Key.Printable -> { currentInput += key.char }
		else -> Unit
	}

}

sealed class Key {
	object Alt : Key()
	object AltGraph : Key()
	object CapsLock : Key()
	object Control : Key()
	object Function : Key()
	object FunctionLock : Key()
	object Hyper : Key()
	object Meta : Key()
	object NumLock : Key()
	object Shift : Key()
	object Super : Key()
	object Symbol : Key()
	object SymbolLock : Key()
	object Enter : Key()
	object Tab : Key()
	sealed class Arrow : Key() {
		object Down : Key()
		object Left : Key()
		object Right : Key()
		object Up : Key()
	}
	object End : Key()
	object Home : Key()
	object PageUp : Key()
	object PageDown : Key()
	object Backspace : Key()
	object Clear : Key()
	object Copy : Key()
	object CrSel : Key()
	object Cut : Key()
	object Delete : Key()
	object EraseEof : Key()
	object ExSel : Key()
	object Insert : Key()
	object Paste : Key()
	object Redo : Key()
	object Undo : Key()
	object Accept : Key()
	object Again : Key()
	object Attn : Key()
	object Cancel : Key()
	object ContextMenu : Key()
	object Escape : Key()
	object Execute : Key()
	object Find : Key()
	object Finish : Key()
	object Help : Key()
	class Printable(val char: Char) : Key()
	class FunctionN(val n: Int) : Key()
	companion object {
		fun from(string: String) = when (string) {
			"Alt" -> Alt
			"AltGraph" -> AltGraph
			"CapsLock" -> CapsLock
			"Control" -> Control
			"Fn" -> Function
			"FnLock" -> FunctionLock
			"Hyper" -> Hyper
			"Meta" -> Meta
			"NumLock" -> NumLock
			"Shift" -> Shift
			"Super" -> Super
			"Symbol" -> Symbol
			"SymbolLock" -> SymbolLock
			"Enter" -> Enter
			"Tab" -> Tab
			"Down" -> Arrow.Down
			"Left" -> Arrow.Left
			"Right" -> Arrow.Right
			"Up" -> Arrow.Up
			"End" -> End
			"Home" -> Home
			"PageUp" -> PageUp
			"PageDown" -> PageDown
			"Backspace" -> Backspace
			"Clear" -> Clear
			"Copy" -> Copy
			"CrSel" -> CrSel
			"Cut" -> Cut
			"Delete" -> Delete
			"EraseEof" -> EraseEof
			"ExSel" -> ExSel
			"Insert" -> Insert
			"Paste" -> Paste
			"Redo" -> Redo
			"Undo" -> Undo
			"Accept" -> Accept
			"Again" -> Again
			"Attn" -> Attn
			"Cancel" -> Cancel
			"ContextMenu" -> ContextMenu
			"Escape" -> Escape
			"Execute" -> Execute
			"Find" -> Find
			"Finish" -> Finish
			"Help" -> Help
			else -> if (string.length == 1)
				Printable(string.first())
			else if (string.first() == 'F')
				FunctionN(string.substring(1).toInt())
			else throw TODO()
		}
	}
}

interface Shell {
	val console: Console
	fun start()
	fun handleKeyPress(key: Key)
}

typealias KeypressHandler = (Key) -> Unit

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

	init {
	    renderElement?.addEventListener("keypress", ::computeKeypressEvent)
	}

	var keypressHandler: KeypressHandler? = null

	fun computeKeypressEvent(event: Event) {
		if (!isInFocus()) return
		if (event !is KeyboardEvent) return
		keypressHandler?.invoke(Key.from(event.key))
	}

	private fun isInFocus(): Boolean = renderElement.containsOrIs(document.activeElement)

	private fun Node?.containsOrIs(node: Node?) = this == node || this?.contains(node) ?: false

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

	fun print(text: String): Unit = TODO()

	fun loginAndThen(block: () -> Unit) {
		print("Username: ")
		var enteredUsername: Boolean = false
		var username = ""
		var password = ""
		keypressHandler = handler@{
			if (it is Key.Enter)
				if (enteredUsername) {
					//TODO: Login
					block()
					return@handler
				} else {
					enteredUsername = true
					print("Password: ")
				}
			if (it !is Key.Printable) return@handler
			if (!enteredUsername) username += it.char else password += it.char
			print(it.char)
		}
	}

	fun start() = loginAndThen {
		val shell = PrimitiveShell(this) //TODO: choose shell properly
		keypressHandler = shell::handleKeyPress
		shell.start()
	}


}

class WebOS {
	val hostname: String = "host"
	private val _consoles = mutableListOf<Console>()
	val consoles get() = _consoles.toList()
	val files: FileService<PrimitiveINode> = PrimitiveFileService()
	fun registerConsole(element: Element) {
		_consoles.add(Console(this, element))
	}
}

data class User(
	val name: String,
	val homeDirectory: Path.Absolute,
	val isRoot: Boolean = false,
)
