package com.ssg.itemevolution.ui

import com.ssg.itemevolution.services.ItemDataService
import com.ssg.itemevolution.services.ItemEvolutionService
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

data class EvolutionStats(
    val level: Int,
    val points: Int,
    val uses: Int,
    val progress: Double,
    val toNext: Int,
    val nextLvl: Int
)

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
     * Extrai estatísticas de evolução do item
     */
    fun extractStats(item: ItemStack): EvolutionStats {
        val level = evolutionService.calculateLevelFromItem(item)
        val uses = itemDataService.getUses(item)
        val points = itemDataService.getPoints(item)
        val progress = evolutionService.getLevelProgress(item)

        val currentCounter = itemDataService.getCounter(item)
        val currentLevelCounter = evolutionService.calculateCounterForLevel(level, item)
        val nextLevelCounter = evolutionService.calculateCounterForLevel(level + 1, item)

        val toNext = currentCounter - currentLevelCounter
        val nextLvl = nextLevelCounter - currentLevelCounter

        return EvolutionStats(level, points, uses, progress, toNext, nextLvl)
    }

    /**
     * Lore completa (Adventure Component)
     */
    fun getDescription(item: ItemStack): List<Component> {
        val description = mutableListOf<Component>()

        addEnchantments(item, description)
        addEvolutionStats(item, description)

        return description
    }

    /**
     * Lore curta
     */
    fun getShortDescription(item: ItemStack): List<Component> {
        val stats = extractStats(item)
        return listOf(
            Component.text("${COLOR_AQUA}Nível: ${stats.level}"),
            Component.text("${COLOR_GOLD}Pontos: ${stats.points}")
        )
    }

    /**
     * Gera linhas para Scoreboard (strings coloridas)
     */
    fun getScoreboardLines(item: ItemStack): List<String> {
        val stats = extractStats(item)
        return listOf(
            "           ${COLOR_AQUA}Soul Tool",
            "    %animation:MyAnimation1%", // você pode trocar/omitir se quiser
            "   &6Info:",
            "    ${COLOR_GRAY}Usos: ${COLOR_GREEN}${stats.uses}",
            "    ${COLOR_AQUA}Nível: ${COLOR_GREEN}${stats.level}",
            "    ${COLOR_GOLD}Pontos: ${COLOR_GREEN}${stats.points}",
            "    ${COLOR_GRAY}NextLevel: ${COLOR_GREEN}${stats.toNext}${COLOR_GRAY}/${COLOR_GREEN}${stats.nextLvl})",
            "",
            ""
        )
    }


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

    private fun addEvolutionStats(item: ItemStack, description: MutableList<Component>) {
        val stats = extractStats(item)
        description.add(Component.text("${COLOR_GRAY}Usos: ${COLOR_GREEN}${stats.uses}"))
        description.add(Component.text("${COLOR_GRAY}Pontos: ${COLOR_GOLD}${stats.points}"))
        description.add(Component.text("${COLOR_GRAY}Progresso: ${COLOR_GREEN}${stats.toNext}${COLOR_GRAY}/${COLOR_GREEN}${stats.nextLvl} ${COLOR_GRAY}(${formatPercentage(stats.progress)})"))
        description.add(Component.text(SEPARATOR))
    }

    private fun formatEnchantmentName(key: String): String {
        return key.split("_")
            .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
    }

    private fun formatPercentage(value: Double): String {
        return String.format("%.1f%%", value * 100)
    }

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
