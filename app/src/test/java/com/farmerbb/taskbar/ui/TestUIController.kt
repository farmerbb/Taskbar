package com.farmerbb.taskbar.ui

import android.content.Context

class TestUIController(context: Context?) : UIController(context) {
    var onCreateHost: UIHost? = null
    var onRecreateHost: UIHost? = null
    var onDestroyHost: UIHost? = null
    public override fun onCreateHost(host: UIHost) {
        onCreateHost = host
    }

    public override fun onRecreateHost(host: UIHost) {
        onRecreateHost = host
    }

    public override fun onDestroyHost(host: UIHost) {
        onDestroyHost = host
    }
}
