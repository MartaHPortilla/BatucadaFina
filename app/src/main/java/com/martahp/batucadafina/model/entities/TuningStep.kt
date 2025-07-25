package com.martahp.batucadafina.model.entities

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.martahp.batucadafina.R

/**
 * Clase de datos que contiene los campos necesarios para representar un paso de la afinación.
 */
data class TuningStep(
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int,
    @DrawableRes val imageResId: Int
)
