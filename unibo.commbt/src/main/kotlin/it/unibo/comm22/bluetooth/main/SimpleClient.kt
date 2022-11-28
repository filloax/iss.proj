package it.unibo.comm22.bluetooth.main

import it.unibo.comm22.bluetooth.BluetoothClientSupport
import it.unibo.comm22.bluetooth.BluetoothConfig
import unibo.comm22.utils.CommSystemConfig

fun main() {
    BluetoothConfig.setConfiguration()
    BluetoothConfig.verbose = true
    CommSystemConfig.tracing = true

    val inReader = System.`in`.bufferedReader()

    print("Insert server BD address: ")

    var address = inReader.readLine()

    while (address == null || address.trim() == "") {
        print("Address wrong, insert again:")
        address = inReader.readLine()
    }

    val clientConnection = BluetoothClientSupport.connect(address)

    println("Started client, send data on std in:")

    var line = inReader.readLine()
    while (line != null) {
        clientConnection.forward(line)
        line = inReader.readLine()
    }
}