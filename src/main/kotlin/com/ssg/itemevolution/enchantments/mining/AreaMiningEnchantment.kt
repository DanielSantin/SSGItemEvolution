package com.ssg.itemevolution.enchantments.mining

import com.ssg.itemevolution.services.ItemRepairService
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

class AreaMiningEnchantment(
    private val itemRepairService: ItemRepairService
) {
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
                            val target = origin.getRelative(0, i, 0)
                            if (!target.type.isAir && isBreakable(target, originHardness)) blocks.add(target)
                        }
                    }
                    BlockFace.UP, BlockFace.DOWN -> {
                        for (i in -1..1) {
                            val target = origin.getRelative(i, 0, 0)
                            if (!target.type.isAir && isBreakable(target, originHardness)) blocks.add(target)
                        }
                    }
                    else -> if (!origin.type.isAir && isBreakable(origin, originHardness)) blocks.add(origin)
                }
            }
            2 -> {
                for (dx in -1..1) {
                    for (dy in -1..1) {
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

}
