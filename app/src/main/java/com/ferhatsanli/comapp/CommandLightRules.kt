package com.ferhatsanli.comapp

import com.ferhatsanli.comapp.ui.glass.CommandLight

/** When the status light under the bottom bar changes (the timing lives in MainActivity). */
internal object CommandLightRules {
    /**
     * Orange while a command is [pending]; when it stops being pending, green for SUCCESS and
     * red for anything else (FAILED, or a send that never reached Firebase). Otherwise unchanged.
     */
    fun next(current: CommandLight, pending: Boolean, status: String?): CommandLight = when {
        pending -> CommandLight.PENDING
        current == CommandLight.PENDING -> if (status == "SUCCESS") CommandLight.SUCCESS else CommandLight.FAILED
        else -> current
    }
}
