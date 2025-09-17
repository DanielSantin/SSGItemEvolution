package com.ssg.itemevolution.enchantments

object CustomEnchantmentRegistry {

    // Encantamentos de Mineração/Corte
    const val DESABAR = "Desabar"
    const val ESCAVAR_VEIA = "escavar_veia"
    const val QUEBRA_AREA = "quebra_area"

    // Encantamentos de Combate
    const val VAMPIRISMO = "vampirismo"
    const val EXECUÇÃO = "execução"
    const val RAIO = "raio"

    // Encantamentos de Utilidade
    const val TELETRANSPORTE = "teletransporte"
    const val REPARAÇÃO_AUTO = "reparação_auto"
    const val XP_BOOST = "xp_boost"

    /**
     * Verifica se um encantamento é de mineração/corte
     */
    fun isMiningEnchantment(enchantName: String): Boolean {
        return enchantName in listOf(DESABAR, ESCAVAR_VEIA, QUEBRA_AREA)
    }
}