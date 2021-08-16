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
import kotlin.properties.Delegates

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

	open fun handleKeyPress(key: Key) {}
	open fun update() {}
	abstract fun render(columns: Int, rows: Int): List<List<CharacterRun>>
}

class ShellRunner(console: Console) : Activity(console) {

	val history = mutableListOf<String>()

	val shellProgramStack = ArrayDeque<ShellProgram>()

	var lastLine = ""

	fun newLine() {
		history.add(lastLine)
		lastLine = ""
		console.invalidateRender()
	}

	fun println(it: String = "") {
		print(it)
		newLine()
	}

	fun print(it: String) {
		lastLine += it
		console.invalidateRender()
	}

	override fun update() {
		shellProgramStack.lastOrNull()?.update()
	}

	fun getInput(): String? {
		if (inputBuffer.contains("\n")) {
			val r = inputBuffer.substringBefore("\n")
			inputBuffer = inputBuffer.substringAfter("\n")
			return r
		}
		return null
	}

	override fun render(columns: Int, rows: Int): List<List<CharacterRun>> =
		history.map { listOf(CharacterRun(it.take(columns), "white")) }.takeLast(rows - 1) + listOf(
			listOf(
				CharacterRun(
					lastLine + inputBuffer,
					"white"
				)
			)
		)

	var inputBuffer = ""

	override fun handleKeyPress(key: Key) = when (key) {
		is Key.Printable -> {
			inputBuffer += key.char
		}
		is Key.Enter     -> {
			inputBuffer += "\n"
		}
		else             -> Unit
	}

	fun <T : ShellProgram> openShellProgram(program: (ShellRunner) -> T): ShellRunner = openShellProgram(program(this))

	fun <T : ShellProgram> openShellProgram(program: T): ShellRunner {
		shellProgramStack.addLast(program)
		return this
	}

}

abstract class ShellProgram(val shellRunner: ShellRunner) {

	abstract fun update(): Unit
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
			"Alt"         -> Alt
			"AltGraph"    -> AltGraph
			"CapsLock"    -> CapsLock
			"Control"     -> Control
			"Fn"          -> Function
			"FnLock"      -> FunctionLock
			"Hyper"       -> Hyper
			"Meta"        -> Meta
			"NumLock"     -> NumLock
			"Shift"       -> Shift
			"Super"       -> Super
			"Symbol"      -> Symbol
			"SymbolLock"  -> SymbolLock
			"Enter"       -> Enter
			"Tab"         -> Tab
			"Down"        -> Arrow.Down
			"Left"        -> Arrow.Left
			"Right"       -> Arrow.Right
			"Up"          -> Arrow.Up
			"End"         -> End
			"Home"        -> Home
			"PageUp"      -> PageUp
			"PageDown"    -> PageDown
			"Backspace"   -> Backspace
			"Clear"       -> Clear
			"Copy"        -> Copy
			"CrSel"       -> CrSel
			"Cut"         -> Cut
			"Delete"      -> Delete
			"EraseEof"    -> EraseEof
			"ExSel"       -> ExSel
			"Insert"      -> Insert
			"Paste"       -> Paste
			"Redo"        -> Redo
			"Undo"        -> Undo
			"Accept"      -> Accept
			"Again"       -> Again
			"Attn"        -> Attn
			"Cancel"      -> Cancel
			"ContextMenu" -> ContextMenu
			"Escape"      -> Escape
			"Execute"     -> Execute
			"Find"        -> Find
			"Finish"      -> Finish
			"Help"        -> Help
			else          -> if (string.length == 1)
				Printable(string.first())
			else if (string.first() == 'F')
				FunctionN(string.substring(1).toInt())
			else throw TODO()
		}
	}
}

class Login(shellRunner: ShellRunner) : ShellProgram(shellRunner) {

	override fun update() {
		while (true)
			when (state) {
				0 -> {
					shellRunner.print("Username: ")
					state = 1
				}
				1 -> {
					val inp = shellRunner.getInput() ?: return
					shellRunner.println(inp)
					shellRunner.print("Password: ")
					username = inp
					state = 2
				}
				2 -> {
					val inp = shellRunner.getInput() ?: return
					shellRunner.println(inp)
					shellRunner.println("Login complete)")
					password = inp
					// TODO: check password and set user
					state = 3
				}
				3 -> {
					TODO()
				}
			}

	}

	var state = 0
	var username = ""
	var password = ""

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

	init {
		renderElement?.addEventListener("keydown", ::computeKeypressEvent)
	}

	fun computeKeypressEvent(event: Event) {
		if (!isInFocus()) return
		if (event !is KeyboardEvent) return
		activityStack.lastOrNull()?.handleKeyPress(Key.from(event.key))
	}

	private fun isInFocus(): Boolean = renderElement.containsOrIs(document.activeElement)

	private fun Node?.containsOrIs(node: Node?) = this == node || this?.contains(node) ?: false

	fun <T : Activity> openActivity(activity: (Console) -> T): Console = openActivity(activity(this))

	fun <T : Activity> openActivity(activity: T): Console {
		activityStack.addLast(activity)
		invalidateRender()
		return this
	}

	fun render() {
		if (renderElement == null) return
		if (!shouldRerender) return
		shouldRerender = false
		val x = activityStack.lastOrNull()?.render(columns, rows)
		console.log(x)
	}

	fun invalidateRender() {
		shouldRerender = true
		window.requestAnimationFrame { render() }
	}

	fun resize(newColumns: Int, newRows: Int) {
		invalidateRender()
	}

	// TODO: Handle resizes of the renderElement

	fun update(): Unit {
		activityStack.lastOrNull()?.update()
	}

	var updateInterval by Delegates.notNull<Int>()

	fun start(): Console {
		updateInterval = window.setInterval(::update, 10)
		return openActivity(ShellRunner(this).also { it.openShellProgram(Login(it)) })
	}

	fun destroy() {
		window.clearInterval(updateInterval)
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
