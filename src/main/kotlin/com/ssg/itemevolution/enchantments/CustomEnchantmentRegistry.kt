package com.ssg.itemevolution.enchantments

object EnchantmentRegistry {

    // Encantamentos de Mineração/Corte
    const val DESABAR = "supera:desabar"
    const val ESCAVAR_VEIA = "supera:escavar_veia"
    const val QUEBRA_AREA = "supera:quebra_area"

    fun isMiningEnchantment(enchantName: String): Boolean {
        return enchantName in listOf(DESABAR, ESCAVAR_VEIA, QUEBRA_AREA)
    }
}