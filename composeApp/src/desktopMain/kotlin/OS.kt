import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import java.io.File
import java.nio.file.Paths

interface OS {
    fun isDarkModeEnabled(): Boolean
    val dataDir: File
}

private fun getUserHome() = System.getProperty("user.home")

class MacOS : OS {
    override fun isDarkModeEnabled() = MacOSDefaults.getDefaultsEntry("AppleInterfaceStyle") == "Dark"
    override val dataDir: File by lazy {
        Paths.get(getUserHome(), "Library", "Application Support", "NUISTin").toFile()
    }
}

class Windows : OS {
    override fun isDarkModeEnabled() = WindowsRegistry.getWindowsRegistryEntry(
        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
        "AppsUseLightTheme"
    ) == 0x0

    override val dataDir: File by lazy { Paths.get(System.getenv("APPDATA"), "NUISTin").toFile() }
}

class Linux : OS {
    override fun isDarkModeEnabled() =
        Dconf.HAS_DCONF
                && Dconf.getDconfEntry("/org/gnome/desktop/interface/gtk-theme").lowercase().contains("dark")

    override val dataDir: File by lazy { File(getUserHome(), ".nuistin") }
}

class OtherOS : OS {
    override fun isDarkModeEnabled() = false
    override val dataDir: File by lazy { File(getUserHome(), "nuistin") }
}

val currentOS: OS by lazy {
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.startsWith("windows") -> Windows()
        osName.startsWith("mac") -> MacOS()
        osName.startsWith("linux") -> Linux()
        else -> OtherOS()
    }
}