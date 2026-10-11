# DEDSAFIO4 Mecánicas (Fabric 1.21.1)

Mod extra que añade mecánicas a las ruletas del mod DEDSAFIO4. Va aparte del jar original.

## Compilar
1. Instala JDK 21 y Gradle 8.10 o superior.
2. En esta carpeta: `gradle wrapper --gradle-version 8.10.2`
3. `./gradlew build` (en Windows `gradlew.bat build`)
4. El mod queda en `build/libs/dedsafio4-mecanicas-1.0.0.jar` (no uses el `-sources`).
5. Ponlo en `mods/` del servidor junto a DEDSAFIO4.jar y Fabric API.

## Comandos (operadores)
- `/mecanica activar <color> [segundos]`
- `/mecanica desactivar <color>`
- `/mecanica apagar_todas`
- `/mecanica lista`
- `/mecanica notificar <true|false>`  (usar o no /notificar del otro mod para el aviso)

Colores: roja, naranja, amarilla, verde, azul, morada, celeste, rosa, nutria, reviil.

## Mecánicas
- roja: mobs hostiles cerca con Fuerza II y Velocidad
- naranja: fuego eterno (solo el agua lo apaga; Resistencia al fuego protege)
- amarilla: el sol quema de día a cielo abierto si no llevas casco
- verde: los zombis infectan (veneno, debilidad, hambre)
- azul: el agua hace daño
- morada: oscuridad
- celeste: lentitud y daño por frío salvo cerca de luz/calor (nivel de luz de bloque 8+)
- rosa: cada 30 s, regalo o trampa al azar
- nutria: respirar bajo el agua, gracia del delfín, regeneración en el agua
- reviil: cada 10 s un efecto al azar (editable en REVIIL_EFECTOS)

Jugadores en creativo o espectador no sufren las mecánicas.
