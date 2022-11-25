package it.unibo.comm22.bluetooth

import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.ColorsOut
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.file.FileSystems
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.io.path.bufferedWriter
import kotlin.io.path.outputStream
import kotlin.io.path.writer
import kotlin.math.log

/**
 * Handles a bluetooth connection based on Python scripts
 * @param process Process of the python script that implements the connection,
 *  its stdout should be the connection received data while its stdin should be
 *  data sent to the connection
 */
class BluetoothConnectionPython(val process: Process)
        : Interaction2021 {

    private val procStdOut = process.inputStream.bufferedReader()
    private val procStdIn = DataOutputStream(process.outputStream)

    val isOpen get() = process.isAlive

    init {
        tryRedirectProcessStderr(process)
    }

    companion object {
        fun createClient(address: String, port: Int = 5, nattempts: Int = 5): Interaction2021 {
            val sep = FileSystems.getDefault().separator
            val scriptPath = Path.of(BluetoothConfig.scriptFolder, BluetoothConfig.clientScript).toAbsolutePath()
            val executable = "${BluetoothConfig.pythonCmd} $scriptPath"
            var verbosePart = if (BluetoothConfig.verbose) " -v" else ""
            val cmd = "$executable -a $address -p $port -r $nattempts$verbosePart"
            val process = Runtime.getRuntime().exec(cmd)

            ColorsOut.out("createClient | Started process with command '$cmd', " +
                    "status: ${if (process.isAlive) "running" else "dead"}" +
                    "/${if (!process.isAlive) process.exitValue() else "-"}")

            return checkProcessAndReturn(process)
        }

        fun createServer(address: String, port: Int = 5): Interaction2021 {
            val sep = FileSystems.getDefault().separator
            val scriptPath = Path.of(BluetoothConfig.scriptFolder, BluetoothConfig.serverScript).toAbsolutePath()
            val executable = "${BluetoothConfig.pythonCmd} $scriptPath"
            var verbosePart = if (BluetoothConfig.verbose) " -v" else ""
            val cmd = "$executable -a $address -p $port$verbosePart"
            val process = Runtime.getRuntime().exec(cmd)

            ColorsOut.out("createServer | Started process with command '$cmd', " +
                    "status: ${if (process.isAlive) "running" else "dead"}" +
                    "/${if (!process.isAlive) process.exitValue() else "-"}")

            return checkProcessAndReturn(process)
        }

        private fun checkProcessAndReturn(process: Process): BluetoothConnectionPython {
            val startLine = process.inputStream.bufferedReader().readLine()

            if (startLine == "CONNECTED") {
                return BluetoothConnectionPython(process)
            } else {
                tryRedirectProcessStderr(process)
                throw IOException("Didn't start BT script properly, start line is <$startLine>; check log if logging")//, err line is <$errLine>")
            }
        }

        private fun tryRedirectProcessStderr(process: Process) {
            if (BluetoothConfig.logScripts || BluetoothConfig.printStdErr) {
                thread {
                    val streams = ArrayList<PrintStream>()
                    if (BluetoothConfig.logScripts) {
                        val logFolderPath = Path.of(BluetoothConfig.scriptFolder, BluetoothConfig.logScriptsFolder)
                        if (!logFolderPath.toFile().isDirectory) {
                            logFolderPath.toFile().mkdir()
                        }
                        val timestamp = DateTimeFormatter.ofPattern("MM-dd_HH-mm").format(LocalDateTime.now())
                        val filename = "btscript_${timestamp}_${process.pid()}.log"
                        val filepath = logFolderPath.resolve(filename)
                        val logStream = PrintStream(filepath.outputStream())
                        streams.add(logStream)
                    }
                    if (BluetoothConfig.printStdErr) {
                        streams.add(System.err)
                    }

                    val reader = process.errorStream.bufferedReader()
                    try {
                        while (process.isAlive || reader.ready()) {
                            val line = reader.readLine()

                            // Python non installato
                            if (line.lowercase().matches(Regex(""".*python.*(non trovato|not found).*"""))) {
                                System.err.println(
                                    "Python command '${BluetoothConfig.pythonCmd}' not found; maybe not installed " +
                                            "or should use change command in config?\nOS error is: $line"
                                )
                            }

                            streams.forEach{
                                it.println(line)
                                it.flush()
                            }
                        }
                    } finally {
                        streams.forEach{
                            it.flush()
                        }
                    }
                }
            }
        }
    }

    override fun forward(msg: String) {
        try {
            procStdIn.writeBytes("$msg\n")
            procStdIn.flush()
        } catch (e : IOException) {
            e.printStackTrace()
        }
    }

    override fun request(msg: String): String? {
        forward(msg)
        return receiveMsg()
    }

    override fun reply(msg: String) {
        forward(msg)
    }

    override fun receiveMsg(): String? {
        return try {
            procStdOut.readLine()
        } catch (e : IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun close() {
        process.destroy()
    }
}