package com.ssg.itemevolution.handlers

import com.ssg.itemevolution.enchantments.mining.AreaMiningEnchantment
import com.ssg.itemevolution.enchantments.mining.DesabarEnchantment
import com.ssg.itemevolution.enchantments.mining.VeinMiningEnchantment
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment
import org.bukkit.block.BlockFace

/**
 * Agrupa os handlers de encantamentos para facilitar o registro.
 * Recebe as dependências diretamente no construtor.
 */
class EnchantmentEventHandlers(
    private val desabarEnchantment: DesabarEnchantment,
    private val veinMiningEnchantment: VeinMiningEnchantment,
    private val eternaEnchantment: EternaEnchantment,
    private val areaMiningEnchantment: AreaMiningEnchantment,
    private val plugin: com.ssg.itemevolution.ItemEvolutionPlugin
) {
    val desabarHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            if (event.type != EnchantmentEventType.BLOCK_BREAK) return EnchantmentEventResult.IGNORED

            val player = event.player
            val breakEvent = event.context["breakEvent"] as? org.bukkit.event.block.BlockBreakEvent
            if (breakEvent == null) {
                plugin.logger.warning("BreakEvent não encontrado no context")
                return EnchantmentEventResult.IGNORED
            }

            val block = breakEvent.block
            if (player.isSneaking) {
                val originalType = block.type
                val originalData = block.blockData.clone()
                desabarEnchantment.executeDesabar(player, block, event.item, event.level, originalType, originalData)
                return EnchantmentEventResult.HANDLED
            }

            return EnchantmentEventResult.IGNORED
        }
    }

    val veinMiningHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            if (event.type != EnchantmentEventType.BLOCK_BREAK) return EnchantmentEventResult.IGNORED

            val player = event.player
            val breakEvent = event.context["breakEvent"] as? org.bukkit.event.block.BlockBreakEvent
            if (breakEvent == null) {
                plugin.logger.warning("BreakEvent não encontrado no context")
                return EnchantmentEventResult.IGNORED
            }

            val block = breakEvent.block
            if (player.isSneaking) {
                val originalType = block.type
                val originalData = block.blockData.clone()
                veinMiningEnchantment.executeVeinMining(player, block, event.item, event.level, originalType, originalData)
                return EnchantmentEventResult.HANDLED
            }

            return EnchantmentEventResult.IGNORED
        }
    }


    val areaMiningHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            if (event.type != EnchantmentEventType.BLOCK_BREAK) return EnchantmentEventResult.IGNORED

            val player = event.player
            val breakEvent = event.context["breakEvent"] as? org.bukkit.event.block.BlockBreakEvent
            if (breakEvent == null) {
                plugin.logger.warning("BreakEvent não encontrado no context")
                return EnchantmentEventResult.IGNORED
            }

            val block = breakEvent.block
            val rayTraceResult = player.world.rayTraceBlocks(
                player.eyeLocation,
                player.location.direction,
                10.0 // Distância do raio, ajuste conforme necessário
            )
            val face = rayTraceResult?.hitBlockFace ?: BlockFace.SELF

            areaMiningEnchantment.executeAreaMining( block, event.item, event.level, face)
            return EnchantmentEventResult.HANDLED
        }
    }

    val eternaHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            return when (event.type) {
                EnchantmentEventType.BEFORE_USE -> handleBeforeUse(event)
                EnchantmentEventType.AFTER_USE -> handleAfterUse(event)
                else -> EnchantmentEventResult.IGNORED
            }
        }

        private fun handleBeforeUse(event: EnchantmentEvent): EnchantmentEventResult {
            if (eternaEnchantment.preventBrokenItemUse(event.player, event.item)) {
                return EnchantmentEventResult.CANCELLED
            }
            return EnchantmentEventResult.IGNORED
        }

        private fun handleAfterUse(event: EnchantmentEvent): EnchantmentEventResult {
            if (eternaEnchantment.checkAndPreserveItem(event.player, event.item)) {
                return EnchantmentEventResult.HANDLED
            }
            return EnchantmentEventResult.IGNORED
        }
    }
}