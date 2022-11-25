package it.unibo.comm22.bluetooth

import it.unibo.comm22.utils.StaticConfig

object BluetoothConfig {
    var scriptFolder: String = ""
    var clientScript: String = "client.py"
    var serverScript: String = "server.py"
    var pythonCmd: String = "python"
    var maxBtVersion: String = "2.0"

    var verbose = false
    var printStdErr = true
    // Redirect script stderr to a log folder
    var logScripts: Boolean = false
    var logScriptsFolder: String = "log"

    fun checkBtVersion(targetVersion: String): Boolean {
        return targetVersion.toFloat() <= maxBtVersion.toFloat()
    }

    private var setConf = false
    private var noWrite = false
    private var noRead = false

    fun setConfiguration(cfgPath: String = "BluetoothConfig.json", force: Boolean = false) {
        if ((!setConf || force) && !noRead) {
            StaticConfig.setConfiguration(this::class, this, cfgPath, noWrite)
            setConf = true
        }
    }

    fun disableWrite() {
        noWrite = true
    }
    fun enableWrite() {
        noWrite = false
    }
    fun disableRead() {
        noRead = true
    }
    fun enableRead() {
        noRead = false
    }
}