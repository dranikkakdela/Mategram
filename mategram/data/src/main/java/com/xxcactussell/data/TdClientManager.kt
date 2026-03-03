package com.xxcactussell.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

interface TdClientManager {
    fun initialize(context: Context)

    fun initCred(context: Context)
    val client: Client
    val updatesFlow: SharedFlow<TdApi.Object>
    val filesUpdatesFlow: SharedFlow<TdApi.UpdateFile>
    val authUpdatesFlow: StateFlow<TdApi.AuthorizationState>
    val chatsUpdatesFlow: SharedFlow<TdApi.Update>
    val newMessagesFlow: SharedFlow<TdApi.Update>
    fun send(query: TdApi.Function<*>, handler: Client.ResultHandler? = null)
    val chatFoldersUpdatesFlow: SharedFlow<TdApi.Update>
    suspend fun <T : TdApi.Object> sendSuspend(query: TdApi.Function<*>): T
}