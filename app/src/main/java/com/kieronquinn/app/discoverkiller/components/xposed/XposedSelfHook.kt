package com.kieronquinn.app.discoverkiller.components.xposed

object XposedSelfHook {

    val XPOSED_APPS = arrayOf("org.meowcat.edxposed.manager", "org.lsposed.manager")

    @JvmStatic
    fun isXposedHooked(): Boolean {
        //Self hook
        return false
    }

}