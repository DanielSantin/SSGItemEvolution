package com.ssg.itemevolution.enchantments.listeners

import com.ssg.itemevolution.EnchantmentHandler
import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.enchantments.CustomEnchantmentRegistry
import com.ssg.itemevolution.enchantments.mining.DesabarEnchantment
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class MiningEnchantmentListener : Listener {

    @EventHandler
    fun onBlockBreakWithActionBar(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val tool = player.inventory.itemInMainHand

        // Verificar se tem o encantamento Desabar
        if (EnchantmentHandler.quickHasCustomEnchantment(tool, CustomEnchantmentRegistry.DESABAR)) {
            val level = EnchantmentHandler.getCustomEnchantmentLevel(tool, CustomEnchantmentRegistry.DESABAR)

            if (level > 0) {
                if (player.isSneaking) {
                    // Cancelar a quebra original para controlarmos o processo
                    event.isDropItems = false
                    val originalType = block.type
                    val originalData = block.blockData.clone()

                    // Executar o Desabar
                    DesabarEnchantment().executeDesabar(player, block, tool, level, originalType, originalData)
                } else {
                    // Mostrar dica na action bar ocasionalmente
                    if (kotlin.random.Random.nextInt(0, 15) == 0) { // ~6.7% de chance
                        player.sendActionBar("§7Agache para ativar o §6§lDesabar")
                    }
                }
            }
        }
    }

}