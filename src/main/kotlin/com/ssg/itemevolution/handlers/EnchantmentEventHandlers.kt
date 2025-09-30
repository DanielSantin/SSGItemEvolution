package com.ssg.itemevolution.handlers

import com.ssg.itemevolution.enchantments.mining.DesabarEnchantment
import com.ssg.itemevolution.enchantments.mining.VeinMiningEnchantment
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment

/**
 * Agrupa os handlers de encantamentos para facilitar o registro.
 * Recebe as dependências diretamente no construtor.
 */
class EnchantmentEventHandlers(
    private val desabarEnchantment: DesabarEnchantment,
    private val veinMiningEnchantment: VeinMiningEnchantment,
    private val eternaEnchantment: EternaEnchantment
) {

    val desabarHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            if (event.type != EnchantmentEventType.BLOCK_BREAK) return EnchantmentEventResult.IGNORED

            val player = event.player
            val block = event.context["block"] as? org.bukkit.block.Block ?: return EnchantmentEventResult.IGNORED

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
            val block = event.context["block"] as? org.bukkit.block.Block ?: return EnchantmentEventResult.IGNORED

            if (player.isSneaking) {
                val originalType = block.type
                val originalData = block.blockData.clone()
                veinMiningEnchantment.executeVeinMining(player, block, event.item, event.level, originalType, originalData)
                return EnchantmentEventResult.HANDLED
            }

            return EnchantmentEventResult.IGNORED
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