package com.ssg.itemevolution.ui

import com.ssg.itemevolution.core.InitializableService
import com.ssg.itemevolution.core.ReloadableService
import com.ssg.itemevolution.services.ItemDataService
import com.ssg.itemevolution.services.ItemEvolutionService
import com.ssg.itemevolution.utils.ConfigManager
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
) : InitializableService, ReloadableService {

    private lateinit var scoreboardDefaultTitle: String
    private lateinit var scoreboardDefaultLines: List<String>
    private lateinit var scoreboardMaxTitle: String
    private lateinit var scoreboardMaxLines: List<String>

    override fun initialize() { loadScoreboardConfig() }
    override fun onConfigReload() { loadScoreboardConfig() }

    fun loadScoreboardConfig() { // Mantenha público/internal para reloads
        val config = configManager.getCustomConfig("scoreboard.yml") ?: return

        val defaultSection = config.getConfigurationSection("scoreboard.default")
        scoreboardDefaultTitle = defaultSection?.getString("title") ?: ""
        scoreboardDefaultLines = defaultSection?.getStringList("lines") ?: emptyList()

        val maxSection = config.getConfigurationSection("scoreboard.max-level")
        scoreboardMaxTitle = maxSection?.getString("title") ?: ""
        scoreboardMaxLines = maxSection?.getStringList("lines") ?: emptyList()
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

    fun getScoreboardContent(item: ItemStack): Pair<String, List<String>> {
        val stats = extractStats(item)
        val isMax = stats.level >= configManager.getEvolutionSettings().maxLevel
        val title = getScoreboardTitle( isMax)
        val lines = getScoreboardLines(stats, isMax)
        return title to lines
    }

    fun getScoreboardTitle(isMaxLvl: Boolean): String {
        return if (isMaxLvl) scoreboardMaxTitle else scoreboardDefaultTitle
    }

    fun getScoreboardLines(stats: EvolutionStats, isMaxLvl: Boolean): List<String> {
        val lines = if (isMaxLvl) scoreboardMaxLines else scoreboardDefaultLines
        return lines.map { line ->
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

//    fun getProgressBar(progress: Double, length: Int = 20): String {
//        val filled = (progress * length).toInt()
//        val empty = length - filled
//        val bar = StringBuilder("§a")
//        repeat(filled) { bar.append("█") }
//        bar.append("§7")
//        repeat(empty) { bar.append("█") }
//        return bar.toString()
//    }
}
