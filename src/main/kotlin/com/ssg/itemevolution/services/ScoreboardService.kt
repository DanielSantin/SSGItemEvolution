package com.ssg.itemevolution.services

import com.ssg.itemevolution.ui.ItemDescriptionFormatter
import me.neznamy.tab.api.TabAPI
import me.neznamy.tab.api.TabPlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ScoreboardService(
    private val soulToolService: SoulToolService,
    private val descriptionFormatter: ItemDescriptionFormatter
) {

    private val scoreboardId = "soultool_board"

    fun showFor(player: Player, item: ItemStack) {
        if (!soulToolService.hasSoul(item)) {
            hideFor(player)
            return
        }

        val tabPlayer: TabPlayer = TabAPI.getInstance().getPlayer(player.uniqueId) ?: return
        val sbManager = TabAPI.getInstance().scoreboardManager ?: return

        // Usa o formatter para gerar as linhas já prontas
        val (title, lines) = descriptionFormatter.getScoreboardContent(item)

        // Cria (ou sobrescreve) o scoreboard com os valores
        val scoreboard = sbManager.createScoreboard(
            scoreboardId,
            title,
            lines
        )

        // Mostra para o jogador
        sbManager.showScoreboard(tabPlayer, scoreboard)
    }


    fun updateLines(player: Player, item: ItemStack) {
        if (!soulToolService.hasSoul(item)){
            hideFor(player)
            return
        }
        showFor(player, item)  // para simplicidade, re-chama showFor para reconstruir as linhas
    }

    fun hideFor(player: Player) {
        val tabPlayer = TabAPI.getInstance().getPlayer(player.uniqueId) ?: return
        val sbManager = TabAPI.getInstance().scoreboardManager ?: return
        sbManager.resetScoreboard(tabPlayer)
    }
}
