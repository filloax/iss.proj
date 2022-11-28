package it.unibo.comm22.bluetooth.main

import it.unibo.comm22.bluetooth.BluetoothConfig
import it.unibo.comm22.bluetooth.BluetoothMsgHandler
import it.unibo.comm22.bluetooth.BluetoothServer
import it.unibo.kactor.IApplMessage
import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.CommSystemConfig
import unibo.comm22.utils.CommUtils

fun main() {
    BluetoothConfig.setConfiguration()
    BluetoothConfig.verbose = true
    CommSystemConfig.tracing = true

    val inReader = System.`in`.bufferedReader()

//    print("Insert device BD address: ")

    val address = "00:10:60:d1:4b:88" //inReader.readLine()

    val server = BluetoothServer("SimpleBTServer", address, object : BluetoothMsgHandler("SimpleMsgHandler") {
        override fun elaborate(msg: IApplMessage, conn: Interaction2021) {
            println("Msg arrived: $msg")
            val reply = CommUtils.buildReply(
                "srvr",
                msg.msgId(),
                msg.msgContent() + " reply",
                "clnt"
            )
            conn.reply(reply.toString())
        }
    })
    server.activate()

    println("Started server on ${server.address}/${server.port}")
}