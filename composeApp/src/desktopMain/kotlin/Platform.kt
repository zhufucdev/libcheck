import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import java.io.File
import java.net.InetAddress
import java.nio.file.Paths
import java.util.*

sealed interface Platform {
    fun isDarkModeEnabled(): Boolean
    val dataDir: File
    val name: String
}

fun getUserHome() = System.getProperty("user.home")!!
fun getHostName() = InetAddress.getLocalHost().hostName!!

class MacOS : Platform {
    override val name: String
        get() = "macOS $version"
    private val version: String

    init {
        val process = ProcessBuilder("sw_vers", "-productVersion").start()
        process.inputStream.use {
            val scanner = Scanner(it).useDelimiter("\\A")
            version = scanner.next().trimEnd()
        }
    }

    override fun isDarkModeEnabled() = MacOSDefaults.getDefaultsEntry("AppleInterfaceStyle") == "Dark"
    override val dataDir: File by lazy {
        Paths.get(getUserHome(), "Library", "Application Support", "libcheck").toFile()
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

class Windows : Platform {
    override val name: String
        get() = System.getProperty("os.name")!!

    override fun isDarkModeEnabled() = WindowsRegistry.getWindowsRegistryEntry(
        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
        "AppsUseLightTheme"
    ) == 0x0

    override val dataDir: File by lazy { Paths.get(System.getenv("APPDATA"), "libcheck").toFile() }
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

class Linux : Platform {
    override val name: String
        get() = "Linux ${System.getProperty("os.version")!!}"

    override fun isDarkModeEnabled() =
        Dconf.HAS_DCONF
                && Dconf.getDconfEntry("/org/gnome/desktop/interface/color-scheme").lowercase().contains("dark")

    override val dataDir: File by lazy { File(getUserHome(), ".libcheck") }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

class OtherOS : Platform {
    override val name: String
        get() = "${System.getProperty("os.name")!!} ${System.getProperty("os.version")!!}"

    override fun isDarkModeEnabled() = false
    override val dataDir: File by lazy { File(getUserHome(), "libcheck") }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

val currentPlatform: Platform by lazy {
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.startsWith("windows") -> Windows()
        osName.startsWith("mac") -> MacOS()
        osName.startsWith("linux") -> Linux()
        else -> OtherOS()
    }
}