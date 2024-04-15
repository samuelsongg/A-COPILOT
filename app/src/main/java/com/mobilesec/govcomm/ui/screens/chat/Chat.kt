package com.mobilesec.govcomm.ui.screens.chat

data class Chat(
    var senderEmail: String = "",
    var receiverEmail: String = "",
    var message: String = "",
    var timestamp: Long = System.currentTimeMillis() // Assuming timestamp is a Long
) {
}