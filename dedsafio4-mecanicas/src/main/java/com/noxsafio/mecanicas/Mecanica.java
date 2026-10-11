package com.noxsafio.mecanicas;

import java.util.Arrays;
import java.util.List;

/** Cada ruleta con su id (el del comando), el tipo de /notificar y sus textos. */
public enum Mecanica {
    ROJA("roja", "rojo",
            "Los mobs hostiles son más fuertes y rápidos. No te alejes de tu refugio.",
            "La sangre se calma. Los mobs vuelven a la normalidad."),
    NARANJA("naranja", "naranja",
            "El fuego no se apaga. Solo el agua te salvará.",
            "Las llamas se apagan. Por ahora estás a salvo."),
    AMARILLA("amarilla", "amarillo",
            "El sol te quema. Cúbrete la cabeza con un casco o busca sombra.",
            "El sol vuelve a ser amable."),
    VERDE("verde", "verde",
            "Los zombis te infectan al golpearte. Cuidado con los contagios.",
            "La infección remite."),
    AZUL("azul", "azul",
            "El agua te hace daño. Aléjate de ríos, lagos y mares.",
            "El agua vuelve a ser segura."),
    MORADA("morada", "morado",
            "La oscuridad lo cubre todo. Quédate cerca de tus compañeros.",
            "La luz regresa."),
    CELESTE("celeste", "celeste",
            "El frío te congela. Busca luz y calor cerca: antorchas, hogueras...",
            "El frío se aleja."),
    ROSA("rosa", "rosa",
            "¿Suerte o trampa? Cada cierto tiempo te tocará un regalo... o una sorpresa.",
            "La sorpresa termina."),
    NUTRIA("nutria", "nutria",
            "Momento Nutria: las nutrias te bendicen. Respiras bajo el agua y nadas como nunca.",
            "La bendición nutria termina."),
    REVIIL("reviil", "reviil",
            "Momento Reviil: las reglas cambian. Cada pocos segundos te pasará algo inesperado.",
            "El momento Reviil ha terminado. Volverá cuando menos lo esperen.");

    public final String id;
    public final String tipoNotificar;
    public final String mensaje;
    public final String mensajeFin;

    Mecanica(String id, String tipoNotificar, String mensaje, String mensajeFin) {
        this.id = id;
        this.tipoNotificar = tipoNotificar;
        this.mensaje = mensaje;
        this.mensajeFin = mensajeFin;
    }

    public static final List<String> IDS = Arrays.stream(values()).map(m -> m.id).toList();

    public static Mecanica deId(String id) {
        for (Mecanica m : values()) {
            if (m.id.equalsIgnoreCase(id)) return m;
        }
        return null;
    }
}
