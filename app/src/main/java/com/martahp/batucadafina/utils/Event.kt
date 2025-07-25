package com.martahp.batucadafina.utils


/**
 * Clase para eventos LiveData
 * Envuelve datos tipo out que representan eventos que solo deben ser observados una vez.
 * Útil para los viewmodels. Estos usan Event para envolver datos que ponen en LiveData.
 *
 * Contenedor para datos que representan eventos de un solo uso.
 * Usado con LiveData para evitar que acciones como navegación o Snackbars
 * se repitan tras cambios de configuración.
 * @param content Contenido del evento (puede ser de cualquier tipo) que se envuelve.
 * @param beenProcessed Bandera que indica si el evento ya ha sido procesado.
 * @constructor Crea un nuevo evento con el contenido proporcionado.
 * Contiene las funciones getContentIfNotProcessed() y peekContent() para obtener el contenido.
 */
open class Event <out T>(private val content: T){
    //out modificador de varianza (covarianza) hace la clase Event solo "produce" los eventos,
    // no los consume. Eso depende de las funciones más abajo

    var beenProcessed = false //por defecto el evento no ha sido procesado
        private set // Permite lectura externa pero no escritura. Solo podrá ser modificado por la clase

    /**
     * Función principal a la que se llamará desde el ViewModel.
     * Devuelve el contenido del evento al que llama
     * y limita su uso a una sola vez, marcándolo como procesado.
     * Devuelve null si el evento ya ha sido procesado.
     */
    fun getContentIfNotProcessed(): T? { // importante! null (?) si el evento ya ha sido procesado
        return if (beenProcessed) {
            null
        } else {
            beenProcessed = true
            content //devuelve el contenido
        }
    }

    /**
     * Devuelve el contenido del evento sin procesarlo.
     * Útil para eventos que no necesitan ser procesados.
     */
    fun peekContent(): T = content

}