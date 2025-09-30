package com.ssg.itemevolution.enchantments.mining

import com.ssg.itemevolution.services.ItemRepairService
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AreaMiningEnchantment(
    private val itemRepairService: ItemRepairService
) {
    fun executeAreaMining(player: Player, brokenBlock: Block, tool: ItemStack, level: Int, face: BlockFace) {
        val blocksToBreak = getBlocksToBreak(brokenBlock, face, level)

        for (block in blocksToBreak) {
            if (block.type.isAir) continue
            itemRepairService.applyDurabilityDamage(tool)
            block.breakNaturally(tool)
            block.world.playSound(block.location, Sound.BLOCK_STONE_BREAK, 0.5f, 1.0f)
        }
    }

    private fun getBlocksToBreak(origin: Block, face: BlockFace, level: Int): List<Block> {
        val blocks = mutableListOf<Block>()

        when (level) {
            1 -> {
                // 1x3 (linha de 3 blocos na direção da face)
                for (i in -1..1) {
                    val target = when (face) {
                        BlockFace.UP, BlockFace.DOWN -> origin.location.add(i.toDouble(), 0.0, 0.0).block
                        BlockFace.NORTH, BlockFace.SOUTH -> origin.location.add(i.toDouble(), 0.0, 0.0).block
                        BlockFace.EAST, BlockFace.WEST -> origin.location.add(0.0, 0.0, i.toDouble()).block
                        else -> origin
                    }
                    blocks.add(target)
                }
            }
            2 -> {
                // 3x3 (plano perpendicular à face clicada)
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val target = when (face) {
                            BlockFace.UP, BlockFace.DOWN -> origin.location.add(dx.toDouble(), 0.0, dy.toDouble()).block
                            BlockFace.NORTH, BlockFace.SOUTH -> origin.location.add(dx.toDouble(), dy.toDouble(), 0.0).block
                            BlockFace.EAST, BlockFace.WEST -> origin.location.add(0.0, dy.toDouble(), dx.toDouble()).block
                            else -> origin
                        }
                        blocks.add(target)
                    }
                }
            }
        }
        return blocks
    }
}
