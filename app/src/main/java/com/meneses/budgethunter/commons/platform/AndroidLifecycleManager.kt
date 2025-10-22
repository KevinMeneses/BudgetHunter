package com.meneses.budgethunter.commons.platform

class AndroidLifecycleManager : LifecycleManager {

    private var lifecycleDelegate: LifecycleDelegate? = null

    override fun setLifecycleDelegate(delegate: LifecycleDelegate) {
        this.lifecycleDelegate = delegate
    }

    override fun onStart() {
        lifecycleDelegate?.onStart()
    }
}
