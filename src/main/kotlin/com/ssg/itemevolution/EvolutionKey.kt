package com.ssg.itemevolution

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
    USES("fplus_usos"),

    /** Lista de encantamentos customizados */
    CUSTOM_ENCHANTS("custom_enc"),

    /** Níveis dos encantamentos customizados */
    CUSTOM_ENCHANT_LEVELS("custom_enc_lvl"),

    /** Display names dos encantamentos customizados */
    CUSTOM_ENCHANT_DISPLAY("custom_enc_display"),

    BREAK_SAVE("break_save");


    fun key(plugin: JavaPlugin): NamespacedKey = NamespacedKey(plugin, id)
}
