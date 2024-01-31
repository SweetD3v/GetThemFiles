package com.filerecover.photorecovery.allrecover.restore.models

data class StatsModel(
    var stateName: String,
    var statColor: Int
) {
    var result: Result = Result(CallbackStatus.IDLE, FileTypes.OTHER, 0L)

    constructor(
        stateName: String,
        statColor: Int,
        result: Result
    ) : this(
        stateName,
        statColor
    ) {
        this.result = result
    }
}