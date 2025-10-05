# Requisitos del Proyecto: Sistema de Registro Básico

Este documento contiene las Historias de Usuario (HU) y sus Criterios de Aceptación (Gherkin) para el desarrollo del módulo de autenticación. Es la Fuente Única de Verdad 

## HU 1: Registro Exitoso y Validaciones

**Historia de Usuario:** Como Nuevo Usuario, quiero registrar una cuenta nueva con mi email y una contraseña segura, para poder acceder al contenido de la plataforma por primera vez.

**Criterios de Aceptación (Gherkin)**
Escenario: Registro de nuevo usuario exitoso
  Dado que el usuario tiene acceso a la pantalla de "Registro"
  Cuando introduce un Nombre de Usuario, un Email ÚNICO y una Contraseña que cumple con los estándares de seguridad
  Entonces el sistema crea la cuenta del usuario y lo redirige a la aplicación.

Escenario: Prevención de registro con email duplicado
  Dado que el email 'usuario@ejemplo.com' ya existe en la base de datos
  Cuando un nuevo usuario intenta registrarse con 'usuario@ejemplo.com'
  Entonces el sistema NO crea la cuenta y muestra un mensaje indicando que el email ya está en uso.

Escenario: Campos obligatorios no proporcionados
  Dado que el usuario omite el campo 'Contraseña' durante el registro
  Cuando intenta enviar el formulario de registro
  Entonces el sistema muestra un mensaje de error y no permite la creación de la cuenta.