package com.martahp.batucadafina.model.entities
import android.app.Activity
import androidx.annotation.DrawableRes

data class TipEntry(
    val title: String,
    val description: String?,
    @DrawableRes val iconResId: Int, //@DrawableRes para indicar que es un recurso de drawable en lugar de un valor primitivo
    val targetActivityClass: Class<out Activity> //Clase de actividad destino, como si fuera un Intent
)