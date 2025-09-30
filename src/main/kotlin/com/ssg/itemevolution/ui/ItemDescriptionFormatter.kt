package com.ssg.itemevolution.ui

import com.ssg.itemevolution.services.ItemDataService
import com.ssg.itemevolution.services.ItemEvolutionService
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import kotlin.collections.iterator

/**
 * Serviço responsável por formatar a descrição/lore dos itens para exibição
 */
class ItemDescriptionFormatter(
    private val itemDataService: ItemDataService,
    private val evolutionService: ItemEvolutionService
) {

    companion object {
        private const val COLOR_GRAY = "§7"
        private const val COLOR_GOLD = "§6"
        private const val COLOR_GREEN = "§a"
        private const val COLOR_AQUA = "§b"
        private const val SEPARATOR = "§7----------------------"
    }

    /**
     * Gera a descrição completa do item para exibição
     */
    fun getDescription(item: ItemStack): List<Component> {
        val description = mutableListOf<Component>()

        // Encantamentos
        addEnchantments(item, description)

        // Estatísticas de evolução
        addEvolutionStats(item, description)

        return description
    }

    /**
     * Gera uma descrição resumida do item
     */
    fun getShortDescription(item: ItemStack): List<Component> {
        val description = mutableListOf<Component>()

        val level = itemDataService.getLevel(item)
        val points = itemDataService.getPoints(item)

        description.add(Component.text("${COLOR_AQUA}Nível: $level"))
        description.add(Component.text("${COLOR_GOLD}Pontos: $points"))

        return description
    }

    /**
     * Adiciona os encantamentos à descrição
     */
    private fun addEnchantments(item: ItemStack, description: MutableList<Component>) {
        val enchantments = item.enchantments
        if (enchantments.isEmpty()) return

        for ((enchantment, level) in enchantments) {
            val enchantName = formatEnchantmentName(enchantment.key.key)
            val romanLevel = toRoman(level)
            description.add(Component.text("$COLOR_GRAY$enchantName $romanLevel"))
        }

        description.add(Component.text(SEPARATOR))
    }

    /**
     * Adiciona as estatísticas de evolução à descrição
     */
    private fun addEvolutionStats(item: ItemStack, description: MutableList<Component>) {
        val uses = itemDataService.getUses(item)
        val level = itemDataService.getLevel(item)
        val points = itemDataService.getPoints(item)
        val progress = evolutionService.getLevelProgress(item)

        // Calcular progresso para próximo nível
        val currentCounter = itemDataService.getCounter(item)
        val currentLevelCounter = evolutionService.calculateCounterForLevel(level, item)
        val nextLevelCounter = evolutionService.calculateCounterForLevel(level + 1, item)

        val toNext = currentCounter - currentLevelCounter
        val nextLvl = nextLevelCounter - currentLevelCounter

        description.add(Component.text("${COLOR_GRAY}Usos: ${COLOR_GREEN}$uses"))
        description.add(Component.text("${COLOR_GRAY}Nível: ${COLOR_AQUA}$level"))
        description.add(Component.text("${COLOR_GRAY}Pontos: ${COLOR_GOLD}$points"))
        description.add(Component.text("${COLOR_GRAY}Progresso: ${COLOR_GREEN}$toNext${COLOR_GRAY}/${COLOR_GREEN}$nextLvl ${COLOR_GRAY}(${formatPercentage(progress)})"))
        description.add(Component.text(SEPARATOR))
    }

    /**
     * Formata o nome do encantamento
     */
    private fun formatEnchantmentName(key: String): String {
        return key.split("_")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Formata porcentagem
     */
    private fun formatPercentage(value: Double): String {
        return String.format("%.1f%%", value * 100)
    }

    /**
     * Converte números para algarismos romanos
     */
    private fun toRoman(number: Int): String {
        if (number > 10) return number.toString()

        return when (number) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            else -> number.toString()
        }
    }

    /**
     * Gera barra de progresso visual
     */
    fun getProgressBar(progress: Double, length: Int = 20): String {
        val filled = (progress * length).toInt()
        val empty = length - filled

        val bar = StringBuilder(COLOR_GREEN)
        repeat(filled) { bar.append("█") }
        bar.append(COLOR_GRAY)
        repeat(empty) { bar.append("█") }

        return bar.toString()
    }
}