package com.sigmaflux.market

import android.app.Application
import com.sigmaflux.market.data.Graph

class SigmaFluxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
