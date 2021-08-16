package com.kieronquinn.app.discoverkiller.components.intentforwarder

import android.content.Intent
import android.util.Log

/**
 *  Wrapper to handle sending Intents from the Xposed hooks to the overlay, without static
 *  fields all over the place.
 */
class IntentForwarder {

    companion object {

        private var INSTANCE: IntentForwarder? = null

        @Synchronized
        fun getOrCreateInstance(): IntentForwarder {
            if(INSTANCE == null) INSTANCE = IntentForwarder()
            return INSTANCE!!
        }

        //Used when a hook's actions depend on whether the overlay is waiting for an activity
        @Synchronized
        fun getInstance(): IntentForwarder? {
            return INSTANCE
        }

        @Synchronized
        fun clearInstance(){
            INSTANCE?.setIntentForwarderCallback(null)
            INSTANCE = null
        }

    }

    private var intentForwarderCallback: IntentForwarderCallback? = null

    fun setIntentForwarderCallback(callback: IntentForwarderCallback?){
        intentForwarderCallback = callback
    }

    fun postIntent(intent: Intent) {
        intentForwarderCallback?.onIntentForwarded(intent)
    }

}

interface IntentForwarderCallback {

    fun onIntentForwarded(intent: Intent)

}