import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import java.io.File
import java.net.InetAddress
import java.nio.file.Paths

sealed interface Platform {
    fun isDarkModeEnabled(): Boolean
    val dataDir: File
}

fun getUserHome() = System.getProperty("user.home")!!
fun getHostName() = InetAddress.getLocalHost().hostName!!

class MacOS : Platform {
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
    override fun isDarkModeEnabled() =
        Dconf.HAS_DCONF
                && Dconf.getDconfEntry("/org/gnome/desktop/interface/gtk-theme").lowercase().contains("dark")

    override val dataDir: File by lazy { File(getUserHome(), ".libcheck") }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

class OtherOS : Platform {
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