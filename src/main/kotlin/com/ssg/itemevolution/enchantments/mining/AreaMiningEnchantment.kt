package com.ssg.itemevolution.enchantments.mining

import com.ssg.itemevolution.services.EnchantmentService
import com.ssg.itemevolution.services.ItemRepairService
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AreaMiningEnchantment(
    private val itemRepairService: ItemRepairService,
    private val enchantmentService: EnchantmentService
) {
    private val AREA_MINING_MODIFIER_KEY = NamespacedKey("ssgitemevolution", "area_mining_speed")
    fun executeAreaMining(brokenBlock: Block, tool: ItemStack, level: Int, face: BlockFace) {
        val blocksToBreak = getBlocksToBreak(brokenBlock, face, level)

        for (block in blocksToBreak) {
            if (block.type.isAir) continue
            itemRepairService.applyDurabilityDamage(tool)
            block.breakNaturally(tool)
            block.world.playSound(block.location, Sound.BLOCK_STONE_BREAK, 0.5f, 1.0f)
        }
    }

    private fun isBreakable(target: Block, originHardness: Float): Boolean {
        val hardness = target.blockData.material.hardness
        return target.type != org.bukkit.Material.BEDROCK && hardness <= originHardness
    }

    private fun getBlocksToBreak(origin: Block, face: BlockFace, level: Int): List<Block> {
        val blocks = mutableListOf<Block>()
        val originHardness = origin.blockData.material.hardness

        when (level) {
            1 -> {
                when (face) {
                    BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST -> {
                        for (i in -1..1) {
                            // Pula o bloco de origem
                            if (i == 0) continue
                            val target = origin.getRelative(0, i, 0)
                            if (!target.type.isAir && isBreakable(target, originHardness)) blocks.add(target)
                        }
                    }
                    BlockFace.UP, BlockFace.DOWN -> {
                        for (i in -1..1) {
                            // Pula o bloco de origem
                            if (i == 0) continue
                            val target = origin.getRelative(i, 0, 0)
                            if (!target.type.isAir && isBreakable(target, originHardness)) blocks.add(target)
                        }
                    }
                    else -> {} // Nenhum bloco adicional a ser quebrado
                }
            }
            2 -> {
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        // Pula o bloco de origem
                        if (dx == 0 && dy == 0) continue
                        val target = when (face) {
                            BlockFace.UP, BlockFace.DOWN -> origin.getRelative(dx, 0, dy) // Plano X-Z
                            BlockFace.NORTH, BlockFace.SOUTH -> origin.getRelative(dx, dy, 0) // Plano X-Y
                            BlockFace.EAST, BlockFace.WEST -> origin.getRelative(0, dy, dx) // Plano Y-Z
                            else -> origin
                        }
                        if (!target.type.isAir && isBreakable(target, originHardness)) blocks.add(target)
                    }
                }
            }
        }
        return blocks
    }

    private fun getSpeedMultiplier(level: Int): Double {
        return when (level) {
            1 -> -0.5
            2 -> -0.7
            else -> 0.0
        }
    }


    fun applyAreaMiningModifier(player: Player, item: ItemStack?) {
        if (item == null || !enchantmentService.hasEnchantment(item, "area_mining")) return

        val level = enchantmentService.getEnchantmentLevel(item, "area_mining")
        val multiplier = getSpeedMultiplier(level) // implemente sua lógica de cálculo do multiplicador

        val digSpeedAttribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return

        // Remove o modificador antigo, caso já exista
        digSpeedAttribute.modifiers.firstOrNull { it.key == AREA_MINING_MODIFIER_KEY }?.let {
            digSpeedAttribute.removeModifier(it)
        }

        // Cria o novo modificador usando NamespacedKey
        val modifier = AttributeModifier(
            AREA_MINING_MODIFIER_KEY,
            multiplier,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1
        )

        // Aplica o modificador
        digSpeedAttribute.addModifier(modifier)
    }

    fun removeAreaMiningModifier(player: Player, item: ItemStack?) {
        if (item == null) return

        val digSpeedAttribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return

        // Remove o modificador usando o NamespacedKey
        digSpeedAttribute.modifiers
            .firstOrNull { it.key == AREA_MINING_MODIFIER_KEY }
            ?.let { digSpeedAttribute.removeModifier(it) }
    }


}
