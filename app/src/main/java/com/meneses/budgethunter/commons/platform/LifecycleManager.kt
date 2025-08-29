package com.meneses.budgethunter.commons.platform

interface LifecycleManager {
    fun onStart()
    fun setLifecycleDelegate(delegate: LifecycleDelegate)
}

interface LifecycleDelegate {
    fun onStart()
}
