package com.trickcal.crayon

import android.app.Application

class CrayonApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
