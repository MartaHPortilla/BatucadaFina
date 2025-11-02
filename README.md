# BatukAfina 🥁
[![Kotlin Version](https://img.shields.io/badge/Kotlin-100%25-7F52FF.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-24%2B-A4C639.svg?style=for-the-badge&logo=android)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)
[![GitHub last commit](https://img.shields.io/github/last-commit/MartaHPortilla/BatucadaFina?style=for-the-badge&logo=github)](https://github.com/MartaHPortilla/BatucadaFina/commits/main)

---

## 🎯 Sobre el Proyecto

**Un afinador de percusión especializado y una herramienta educativa para Android, construido con Kotlin y un motor de procesamiento de audio avanzado.**

![Portada de BatukAfina](readme-assets/main.png)

BatukAfina nace de una necesidad real en la comunidad de músicos de percusión, especialmente en los ensambles de batucada: la falta de una herramienta de afinación precisa y fácil de usar para tambores. Los afinadores cromáticos estándar a menudo fallan al interpretar los complejos armónicos de la percusión, llevando a errores de octava y a una experiencia frustrante para el músico.

Esta aplicación aborda el problema desde dos frentes:

1.  **Herramienta de Precisión:** Ofrece un motor de análisis de audio robusto que identifica la frecuencia fundamental real, incluso en entornos ruidosos.
2.  **Plataforma Educativa:** Proporciona un entorno en desarrollo de guías y contexto para que los músicos sin formación teórica puedan entender el proceso de afinación y mejorar el sonido de su conjunto.

El proyecto fue desarrollado desde cero como proyecto final de DAM, abarcando todo el ciclo de vida del software, desde el análisis de requisitos y el diseño de la arquitectura hasta la implementación y las pruebas.

---

## 📸 Capturas de Pantalla

|                                                                Menú Principal                                                                | Afinador Específico | Lista de Instrumentos |
|:---:| :---: | :---: |
| <img src="https://github.com/MartaHPortilla/BatucadaFina/blob/main/readme-assets/app_menu.jpg?raw=true" alt="Pantalla Usuario" width="250"/> | <img src="https://github.com/MartaHPortilla/BatucadaFina/blob/main/readme-assets/test220.jpg?raw=true" alt="Afinador Específico" width="250"/> | <img src="https://github.com/MartaHPortilla/BatucadaFina/blob/main/readme-assets/all_instruments.png?raw=true" alt="Lista de Instrumentos" width="250"/> |

---

## ✨ Características

*   **Afinador de Precisión para Percusión:**
    *   Detecta la frecuencia fundamental real gracias a un motor de audio avanzado (FFT + HPS).
    *   Proporciona **feedback visual instantáneo** (colores e indicadores "Apretar/Aflojar") para guiar al músico hacia la nota objetivo.

*   **Biblioteca de Instrumentos Personalizada:**
    *   Incluye un catálogo de instrumentos de batucada predefinidos con sus afinaciones estándar.
    *   Soporte **CRUD** completo: crea, edita y elimina tus propios instrumentos.
    *   Marca instrumentos como **favoritos** para un acceso inmediato.

*   **Sistema de Cuentas de Usuario:**
    *   Guarda tu colección de instrumentos de forma segura en tu perfil personal.
    *   Acceso rápido y protegido mediante inicio de sesión.

*   **Contenido Educativo Integrado:**
    *   Una **guía de afinación** visual paso a paso, ideal para principiantes.
    *   Tabla de consulta de frecuencias recomendadas para diferentes tambores.

*   **Acceso Rápido sin Registro:**
    *   Incluye un **afinador cromático básico** de acceso inmediato para mediciones rápidas, sin necesidad de crear una cuenta.
 
---

## 🚀 Getting Started

Sigue estos pasos para compilar y ejecutar una copia del proyecto en tu máquina local.

### Prerrequisitos

Asegúrate de tener instalado el siguiente software:

*   **Android Studio:** [Jellyfish | 2023.3.1](https://developer.android.com/studio) o una versión superior.
*   **JDK:** Versión 17 o superior.
*   **Dispositivo Android:** Un dispositivo físico o emulador con Android 7.0 (API 24) o superior.

### Instalación

1.  **Clona el repositorio**
    ```sh
    git clone https://github.com/MartaHPortilla/BatucadaFina.git
    ```

2.  **Abre el proyecto en Android Studio**
    *   Ve a `File` -> `Open` y selecciona el directorio del proyecto que acabas de clonar.

3.  **Sincroniza las dependencias**
    *   Espera a que Android Studio termine el proceso de `Gradle Sync`. Esto descargará todas las librerías necesarias.

4.  **Ejecuta la aplicación**
    *   Selecciona un dispositivo (emulador o físico) y pulsa el botón `Run 'app'`.
    *   ¡Listo! No se requiere ninguna configuración adicional ni claves de API para ejecutar el proyecto.

---

## 🛠️ Stack Tecnológico y Arquitectura

La aplicación se ha construido siguiendo las mejores prácticas recomendadas por Google para el desarrollo nativo en Android.

* **Lenguaje:** **Kotlin 100%**, aprovechando sus características de seguridad y las **Corrutinas** para gestionar las operaciones en segundo plano.
* **Arquitectura:** **MVVM (Model-View-ViewModel)**, utilizando componentes de **Android Jetpack** como `ViewModel` y `LiveData` para crear una interfaz de usuario reactiva y desacoplada.
* **Base de Datos:** **SQLite** gestionada a través de una implementación manual de `SQLiteOpenHelper` y el **patrón DAO** (Data Access Object) para encapsular la lógica de acceso a datos.
* **Interfaz de Usuario (UI):** Diseñada con **XML Layouts** y la librería **Material Components (Material 3)** para una experiencia de usuario moderna y consistente.
* **Librerías Clave:**
    * **JTransforms:** Para la implementación de la **Transformada Rápida de Fourier (FFT)**, el núcleo matemático del análisis de espectro.
    * **Glide:** Para la carga y gestión eficiente de imágenes.
    * **JUnit 4 & Truth:** Para la realización de pruebas unitarias en la capa de datos.

### 🔬 El Motor de Afinación: Un Vistazo Técnico

El verdadero corazón de BatukAfina es su pipeline de procesamiento de audio, diseñado para ser robusto contra el ruido y los errores de octava:

1.  **Captura y Ventana:** El audio se captura en tiempo real y se le aplica una **Ventana de Hamming** para reducir la fuga espectral.
2.  **Análisis Espectral (FFT):** La señal se transforma al dominio de la frecuencia para obtener su espectro de energía.
3.  **Detección de Fundamental (HPS):** Se implementa el **Espectro de Producto Armónico** para discriminar los armónicos y aislar la frecuencia fundamental real, solucionando el principal problema de los afinadores genéricos.
4.  **Ponderación y Refinamiento:** Se aplica una **ponderación de frecuencia** para dar prioridad a los tonos graves y una **interpolación parabólica** para alcanzar una precisión sub-hertziana.

---

## 🚀 Vías Futuras

BatukAfina es un proyecto con un gran potencial de crecimiento. Algunas de las futuras mejoras planificadas incluyen:

* **Mejora del sistema de Autenticación** para proteger la privacidad de los usuarios.
* **Migración a Jetpack Compose** para modernizar completamente la capa de UI.
* **Implementación del patrón Repositorio** para centralizar aún más el acceso a los datos.
* **Investigación de algoritmos de afinación alternativos** como YIN para mejorar la precisión.
* **Aplicación de ponderación de frecuencias personalizadas** para mejorar la afinación en octavas altas.
* **Sincronización en la nube con Firebase** para permitir perfiles de usuario multiplataforma y la posibilidad de compartir afinaciones.
* **Nuevas herramientas para el músico**, como un metrónomo avanzado y una grabadora de ritmos.
* **Mejora del diseño de la interfaz de usuario** para una experiencia más fluida y accesible.

---

## 👨‍💻 Autora

**Marta H. Portilla**

*   GitHub: [@MartaHPortilla](https://github.com/MartaHPortilla)
*   LinkedIn: [Marta H. Portilla](https://www.linkedin.com/in/martahportilla/)

---

## 📄 Licencia

Este proyecto se distribuye bajo la Licencia MIT. Consulta el archivo `LICENSE.md` para más detalles.
