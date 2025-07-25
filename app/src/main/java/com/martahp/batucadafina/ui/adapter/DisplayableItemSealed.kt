package com.martahp.batucadafina.ui.adapter

import com.martahp.batucadafina.model.entities.CreatedInstrument
import com.martahp.batucadafina.model.entities.PredefinedInstrument

sealed class DisplayableItemSealed {

    // data class que representa un instrumento predefinido
    data class PredefinedInstrumentItem(val instrument: PredefinedInstrument, val isFavorite: Boolean) : DisplayableItemSealed() {
        val id = instrument.id
    }

    // data class que representa un instrumento creado
    data class CreatedInstrumentItem(val instrument: CreatedInstrument, val isFavorite: Boolean) : DisplayableItemSealed() {
        val id = instrument.id
    }

    // data class que representa un item que es un header o título de sección
    data class HeaderItem(val title: String) : DisplayableItemSealed() {
        val diffIid = title.hashCode().toLong() // un identificador único para el header
    }


}