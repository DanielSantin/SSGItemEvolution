package com.ssg.itemevolution.handlers

import EnchantmentEventResult
import com.ssg.itemevolution.enchantments.mining.AreaMiningEnchantment
import com.ssg.itemevolution.enchantments.mining.DesabarEnchantment
import com.ssg.itemevolution.enchantments.mining.VeinMiningEnchantment
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.event.block.BlockBreakEvent

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

    /**
     * Dados extraídos de um evento de quebra de bloco
     */
    private data class BlockBreakData(
        val block: Block,
        val originalType: org.bukkit.Material,
        val originalData: BlockData
    )

    /**
     * Extrai e valida o BlockBreakEvent do contexto
     */
    private fun getBreakEvent(event: EnchantmentEvent): BlockBreakEvent? {
        val breakEvent = event.context["breakEvent"] as? BlockBreakEvent
        if (breakEvent == null) {
            plugin.logger.warning("BreakEvent não encontrado no context")
        }
        return breakEvent
    }

    /**
     * Valida e extrai dados necessários para handlers de mineração com agachamento
     */
    private fun validateSneakingMining(event: EnchantmentEvent): BlockBreakData? {
        val breakEvent = getBreakEvent(event) ?: return null

        if (!event.player.isSneaking) return null

        val block = breakEvent.block
        return BlockBreakData(
            block = block,
            originalType = block.type,
            originalData = block.blockData.clone()
        )
    }

    val desabarHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            val data = validateSneakingMining(event) ?: return EnchantmentEventResult.IGNORED

            desabarEnchantment.executeDesabar(
                event.player,
                data.block,
                event.item,
                event.level,
                data.originalType,
                data.originalData
            )
            return EnchantmentEventResult.HANDLED
        }
    }

    val veinMiningHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            val data = validateSneakingMining(event) ?: return EnchantmentEventResult.IGNORED

            veinMiningEnchantment.executeVeinMining(
                event.player,
                data.block,
                event.item,
                event.level,
                data.originalType,
                data.originalData
            )
            return EnchantmentEventResult.HANDLED
        }
    }

    val areaMiningHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            val breakEvent = getBreakEvent(event) ?: return EnchantmentEventResult.IGNORED

            val block = breakEvent.block
            val player = event.player
            val rayTraceResult = player.world.rayTraceBlocks(
                player.eyeLocation,
                player.location.direction,
                10.0
            )
            val face = rayTraceResult?.hitBlockFace ?: BlockFace.SELF

            areaMiningEnchantment.executeAreaMining(block, event.item, event.level, face)
            return EnchantmentEventResult.HANDLED
        }
    }

    val eternaBeforeUseHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            return if (eternaEnchantment.preventBrokenItemUse(event.player, event.item)) {
                EnchantmentEventResult.CANCELLED
            } else {
                EnchantmentEventResult.IGNORED
            }
        }
    }

    val eternaAfterUseHandler = object : EnchantmentEventHandler {
        override fun handle(event: EnchantmentEvent): EnchantmentEventResult {
            return if (eternaEnchantment.checkAndPreserveItem(event.player, event.item)) {
                EnchantmentEventResult.HANDLED
            } else {
                EnchantmentEventResult.IGNORED
            }
        }
    }
}