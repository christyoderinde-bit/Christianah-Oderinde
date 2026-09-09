package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatMessageDao: ChatMessageDao) {

    val allMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    fun getMessagesForPeer(peerName: String): Flow<List<ChatMessageEntity>> {
        return chatMessageDao.getMessagesForPeer(peerName)
    }

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        return chatMessageDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessageEntity) {
        chatMessageDao.updateMessage(message)
    }

    suspend fun deleteMessage(id: Long) {
        chatMessageDao.deleteMessage(id)
    }

    suspend fun clearAllMessages() {
        chatMessageDao.clearAllMessages()
    }
}
