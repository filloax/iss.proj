package it.unibo.comm22.bluetooth.main

import it.unibo.comm22.bluetooth.BluetoothClientSupport
import it.unibo.comm22.bluetooth.BluetoothConfig
import unibo.comm22.utils.ColorsOut
import unibo.comm22.utils.CommSystemConfig
import unibo.comm22.utils.CommUtils

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
        val msg = CommUtils.buildDispatch("clnt", "test", line, "srvr")
        ColorsOut.out("Forwarding msg '$msg'...", ColorsOut.ANSI_PURPLE)
        clientConnection.forward(msg.toString())
        line = inReader.readLine()
    }
    ColorsOut.out("Input finished, closing", ColorsOut.ANSI_PURPLE)
}