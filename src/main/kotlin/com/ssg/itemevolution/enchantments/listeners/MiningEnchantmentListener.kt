package com.ssg.itemevolution.enchantments.listeners

import com.ssg.itemevolution.EnchantmentHandler
import com.ssg.itemevolution.enchantments.EnchantmentRegistry
import com.ssg.itemevolution.enchantments.mining.DesabarEnchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class MiningEnchantmentListener : Listener {

    @EventHandler
    fun onBlockBreakWithActionBar(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val tool = player.inventory.itemInMainHand

        // Verificar se tem o encantamento Desabar (usando o novo sistema)
        if (EnchantmentHandler.hasEnchantment(tool, EnchantmentRegistry.DESABAR)) {
            val level = EnchantmentHandler.getEnchantmentLevel(tool, EnchantmentRegistry.DESABAR)

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

        // Verificar outros encantamentos de mineração
        checkEscavarVeia(event, tool)
        checkQuebraArea(event, tool)
    }

    private fun checkEscavarVeia(event: BlockBreakEvent, tool: org.bukkit.inventory.ItemStack) {
        if (EnchantmentHandler.hasEnchantment(tool, EnchantmentRegistry.ESCAVAR_VEIA)) {
            EnchantmentHandler.getEnchantmentLevel(tool, EnchantmentRegistry.ESCAVAR_VEIA)
            // Implementar lógica do Escavar Veia aqui
            // TODO: Implementar ação do encantamento
        }
    }

    private fun checkQuebraArea(event: BlockBreakEvent, tool: org.bukkit.inventory.ItemStack) {
        if (EnchantmentHandler.hasEnchantment(tool, EnchantmentRegistry.QUEBRA_AREA)) {
            EnchantmentHandler.getEnchantmentLevel(tool, EnchantmentRegistry.QUEBRA_AREA)
            // Implementar lógica do Quebra Área aqui
            // TODO: Implementar ação do encantamento
        }
    }
}