package com.martahp.batucadafina.model.repository

import com.martahp.batucadafina.model.entities.InstrumentConfiguration

/**
 * Clase que maneja la configuración de los instrumentos.
 *
 */

class ConfigurationManager {

    private val instrumentConfigurations = mutableListOf<InstrumentConfiguration>()

    fun addInstrumentConfiguration(configuration: InstrumentConfiguration) {
        instrumentConfigurations.add(configuration)
    }

    fun getInstrumentConfiguration(instrumentName: String): InstrumentConfiguration? {
        return instrumentConfigurations.find { it.name == instrumentName }

    }

    fun removeConfiguration(instrumentName: String) {
        instrumentConfigurations.removeAll { it.name == instrumentName }

    }

//TODO: esta clase no la usamos?


}