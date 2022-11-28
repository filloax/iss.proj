package it.unibo.comm22.bluetooth

import unibo.comm22.utils.CommSystemConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

fun main() {
    BluetoothConfig.setConfiguration()
    BluetoothConfig.verbose = true
    CommSystemConfig.tracing = true

    val inReader = System.`in`.bufferedReader()

    print("Insert server BD address: ")

    val address = inReader.readLine()
    val clientConnection = BluetoothClientSupport.connect(address)

    println("Started client, send data on std in:")

    var line = inReader.readLine()
    while (line != null) {
        clientConnection.forward(line)
        line = inReader.readLine()
    }
}