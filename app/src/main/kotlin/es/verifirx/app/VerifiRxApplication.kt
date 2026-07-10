package es.verifirx.app

import android.app.Application
import es.verifirx.app.di.ServiceLocator

class VerifiRxApplication : Application() {
    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)
    }
}
