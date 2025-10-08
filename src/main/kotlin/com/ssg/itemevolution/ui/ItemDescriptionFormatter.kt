package com.ssg.itemevolution.ui

import com.ssg.itemevolution.services.ItemDataService
import com.ssg.itemevolution.services.ItemEvolutionService
import com.ssg.itemevolution.utils.ConfigManager
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
    private val evolutionService: ItemEvolutionService,
    private val configManager: ConfigManager
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

    fun getScoreboardContent(item: ItemStack): Pair<String, List<String>> {
        val stats = extractStats(item)
        val isMax = stats.level >= configManager.getEvolutionSettings().maxLevel
        val title = getScoreboardTitle( isMax)
        val lines = getScoreboardLines(stats, isMax)
        return title to lines
    }

    fun getScoreboardTitle(isMaxLvl: Boolean): String {
        return configManager.getScoreboardTitle(isMaxLvl)
    }

    fun getScoreboardLines(stats: EvolutionStats, isMaxLvl: Boolean): List<String> {
        return configManager.getScoreboardLines(isMaxLvl).map { line ->
            applyPlaceholders(line, stats)
        }
    }



    private fun applyPlaceholders(text: String, stats: EvolutionStats): String {
        return text
            .replace("{level}", stats.level.toString())
            .replace("{points}", stats.points.toString())
            .replace("{uses}", stats.uses.toString())
            .replace("{toNext}", stats.toNext.toString())
            .replace("{nextLvl}", stats.nextLvl.toString())
    }


    private fun addEvolutionStats(item: ItemStack, description: MutableList<Component>) {
        val stats = extractStats(item)
        description.add(Component.text("${COLOR_GRAY}Usos: ${COLOR_GREEN}${stats.uses}"))
        description.add(Component.text("${COLOR_GRAY}Pontos: ${COLOR_GOLD}${stats.points}"))
        description.add(Component.text("${COLOR_GRAY}Progresso: ${COLOR_GREEN}${stats.toNext}${COLOR_GRAY}/${COLOR_GREEN}${stats.nextLvl} ${COLOR_GRAY}(${formatPercentage(stats.progress)})"))
        description.add(Component.text(SEPARATOR))
    }

    private fun formatPercentage(value: Double): String {
        return String.format("%.1f%%", value * 100)
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
