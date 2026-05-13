package com.itsur.credito.data

import com.itsur.credito.db.AppDatabase

class DatabaseHelper(driverFactory: DatabaseDriverFactory) {
    // Creamos el driver usando la fábrica que ya configuraste (Android o Desktop)
    private val driver = driverFactory.createDriver()

    // Esta es la instancia que usaremos para hacer consultas
    val database = AppDatabase(driver)
}