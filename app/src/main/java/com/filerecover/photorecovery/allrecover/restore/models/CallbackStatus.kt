package com.filerecover.photorecovery.allrecover.restore.models

sealed class CallbackStatus{
    object IDLE : CallbackStatus()
    object LOADING : CallbackStatus()
    object SUCCESS : CallbackStatus()
}