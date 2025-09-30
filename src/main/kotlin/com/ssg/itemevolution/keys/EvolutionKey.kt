package com.ssg.itemevolution.keys

import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

enum class EvolutionKey(val id: String) {
    /** Marca se a ferramenta possui alma */
    SOUL_TOOL("soul_tool"),

    /** Contador de uso da ferramenta */
    COUNTER("fplus_contador"),

    /** Nível atual da ferramenta */
    LEVEL("fplus_level"),

    /** Pontos acumulados para evolução */
    POINTS("fplus_pontos"),

    /** Número total de usos */
    USES("fplus_usos");

    fun key(plugin: JavaPlugin): NamespacedKey = NamespacedKey(plugin, id)
}
