package com.luc4n3x.levyra.desktop.app

import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.system.exitProcess

internal object DesktopCrashHandler {
    private val handling = AtomicBoolean(false)

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (!handling.compareAndSet(false, true)) {
                throwable.printStackTrace()
                exitProcess(1)
            }

            val report = buildReport(thread, throwable)
            persistReport(report)
            runCatching {
                if (SwingUtilities.isEventDispatchThread()) {
                    showDialog(report)
                } else {
                    SwingUtilities.invokeAndWait { showDialog(report) }
                }
            }.onFailure {
                System.err.println(report)
            }
            exitProcess(1)
        }
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val trace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()
        return buildString {
            appendLine("Levyra Desktop ${AppInfo.version()}")
            appendLine("Timestamp: ${Instant.now()}")
            appendLine("Thread: ${thread.name}")
            appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
            appendLine("Java: ${System.getProperty("java.version")}")
            appendLine()
            append(trace)
        }
    }

    private fun persistReport(report: String) {
        runCatching {
            val directory = AppPaths.defaultRoot().resolve("crash-reports")
            Files.createDirectories(directory)
            val timestamp = DateTimeFormatter
                .ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
            Files.writeString(
                directory.resolve("levyra-crash-$timestamp.txt"),
                report,
                StandardCharsets.UTF_8
            )
        }
    }

    private fun showDialog(report: String) {
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
        val italian = Locale.getDefault().language.equals("it", ignoreCase = true)
        val dialog = JDialog().apply {
            title = if (italian) "Levyra - Errore imprevisto" else "Levyra - Unexpected error"
            isModal = true
            defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            preferredSize = Dimension(760, 520)
            minimumSize = Dimension(560, 380)
        }

        val title = if (italian) "Levyra si è chiusa in modo imprevisto" else "Levyra closed unexpectedly"
        val subtitle = if (italian) {
            "Il rapporto è stato salvato nella cartella dati dell'app. Puoi copiarlo per la diagnosi."
        } else {
            "The report was saved in the app data directory. You can copy it for diagnosis."
        }
        val copyLabel = if (italian) "Copia rapporto" else "Copy report"
        val copiedLabel = if (italian) "Copiato" else "Copied"
        val closeLabel = if (italian) "Chiudi" else "Close"

        val content = JPanel(BorderLayout(0, 12)).apply {
            border = BorderFactory.createEmptyBorder(18, 18, 18, 18)
        }
        val header = JPanel(BorderLayout(0, 5)).apply {
            add(JLabel(title).apply { font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.NORTH)
            add(JLabel("<html>$subtitle<br><font color='gray'>Levyra Desktop ${AppInfo.version()}</font></html>"), BorderLayout.CENTER)
        }
        val textArea = JTextArea(report).apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            caretPosition = 0
            background = Color(25, 27, 32)
            foreground = Color(220, 224, 230)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        val copy = JButton(copyLabel).apply {
            addActionListener {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(report), null)
                text = copiedLabel
                isEnabled = false
            }
        }
        val close = JButton(closeLabel).apply {
            addActionListener { dialog.dispose() }
        }
        buttons.add(copy)
        buttons.add(close)
        content.add(header, BorderLayout.NORTH)
        content.add(JScrollPane(textArea), BorderLayout.CENTER)
        content.add(buttons, BorderLayout.SOUTH)
        dialog.contentPane = content
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }
}
