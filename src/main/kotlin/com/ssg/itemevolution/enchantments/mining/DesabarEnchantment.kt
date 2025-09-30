package com.ssg.itemevolution.enchantments.mining

import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.services.ItemRepairService
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.Sound
import java.util.*

class DesabarEnchantment (
    private val plugin: ItemEvolutionPlugin,
    private val itemRepairService: ItemRepairService
) {
    companion object {
        private const val MAX_TREE_SIZE = 200
        private const val MAX_SEARCH_RADIUS = 30

        private fun mat(name: String) = Material.getMaterial(name)
            ?: throw IllegalArgumentException("Material $name não encontrado. Verifique a versão do servidor.")

        // Materiais de madeira
        private val WOOD_MATERIALS = setOf(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_WOOD, Material.BIRCH_WOOD, Material.SPRUCE_WOOD,
            Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
            Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
            mat("PALE_OAK_LOG"),
            mat("STRIPPED_PALE_OAK_LOG")
        )

        // Folhas
        private val LEAF_MATERIALS = setOf(
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
            Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES
        )
    }

    fun executeDesabar(player: Player, brokenBlock: Block, tool: ItemStack, level: Int, originalType: Material, originalData: BlockData) {
        if (!WOOD_MATERIALS.contains(brokenBlock.type)) return
        plugin.logger.info("DesabarEnchantment - executeDesabar")

        val treeBlocks = getTree(brokenBlock)
        if (treeBlocks.isEmpty()) return

        val sortedBlocks = treeBlocks.sortedByDescending { it.y }

        val probabilityConfig = mapOf(
            1 to mapOf(0 to 100.0),
            2 to mapOf(0 to 25.0, 1 to 50.0, 2 to 25.0),
            3 to mapOf(0 to 10.0, 1 to 40.0, 2 to 30.0, 3 to 20.0)
        )

        val extraBlocks = calculateExtraBlocks(level, probabilityConfig)
        val blocksToBreak = (extraBlocks + 1).coerceAtMost(sortedBlocks.size)

        var originalBlockWasBroken = false

        for (i in 0 until blocksToBreak) {
            val block = sortedBlocks[i]
            if (WOOD_MATERIALS.contains(block.type)) {
                if (block.location == brokenBlock.location) {
                    originalBlockWasBroken = true
                }

                itemRepairService.applyDurabilityDamage(tool)
                block.breakNaturally(tool)
                block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)
            }
        }

        player.playSound(player.location, Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f)

        if (!originalBlockWasBroken) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                brokenBlock.type = originalType
                brokenBlock.blockData = originalData
            }, 1L)
        }
    }

    private fun calculateExtraBlocks(level: Int, config: Map<Int, Map<Int, Double>>): Int {
        val levelConfig = config[level] ?: return 0
        val random = kotlin.random.Random.nextDouble(0.0, 100.0)

        var cumulativeProbability = 0.0
        for ((extraBlocks, probability) in levelConfig.toList().sortedBy { it.first }) {
            cumulativeProbability += probability
            if (random <= cumulativeProbability) return extraBlocks
        }
        return levelConfig.keys.minOrNull() ?: 0
    }

    private fun getTree(startBlock: Block): List<Block> {
        val world = startBlock.world ?: return emptyList()

        // 1. Localizar a "raiz" descendo até o primeiro tronco conectado ao chão
        var root = startBlock
        while (root.y > world.minHeight) {
            val below = world.getBlockAt(root.x, root.y - 1, root.z)
            if (WOOD_MATERIALS.contains(below.type)) {
                root = below
            } else {
                break
            }
        }

        val troncos = mutableListOf<Block>()
        val visited = mutableSetOf<Block>()
        val queue: Queue<Block> = LinkedList()

        troncos.add(root)
        visited.add(root)
        queue.add(root)

        while (queue.isNotEmpty() && troncos.size < MAX_TREE_SIZE) {
            val current = queue.poll()

            // 2. Buscar troncos adjacentes
            getBlocksAround(current.location).forEach { nearby ->
                if (nearby !in visited &&
                    WOOD_MATERIALS.contains(nearby.type) &&
                    horizontalDistance(root.location, nearby.location) <= 3 &&
                    isWithinSearchRadius(root.location, nearby.location)
                ) {
                    visited.add(nearby)
                    troncos.add(nearby)
                    queue.add(nearby)
                }
            }

            // 3. Buscar via ponte de folhas (restrita)
            getBlocksAround(current.location).forEach { leaf ->
                if (LEAF_MATERIALS.contains(leaf.type)) {
                    getBlocksAround(leaf.location).forEach { maybeTrunk ->
                        if (maybeTrunk !in visited &&
                            WOOD_MATERIALS.contains(maybeTrunk.type) &&
                            maybeTrunk.y >= current.y && // só igual ou acima
                            horizontalDistance(root.location, maybeTrunk.location) <= 3 &&
                            isWithinSearchRadius(root.location, maybeTrunk.location)
                        ) {
                            visited.add(maybeTrunk)
                            troncos.add(maybeTrunk)
                            queue.add(maybeTrunk)
                        }
                    }
                }
            }
        }

        return if (troncos.size >= MAX_TREE_SIZE) emptyList() else troncos
    }

    private fun horizontalDistance(a: Location, b: Location): Int {
        return maxOf(kotlin.math.abs(a.blockX - b.blockX), kotlin.math.abs(a.blockZ - b.blockZ))
    }



    private fun getBlocksInRange(location: Location, range: Int): List<Block> {
        val blocks = mutableListOf<Block>()
        val world = location.world ?: return blocks

        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    if (x == 0 && y == 0 && z == 0) continue
                    blocks.add(world.getBlockAt(location.blockX + x, location.blockY + y, location.blockZ + z))
                }
            }
        }
        return blocks
    }

    private fun getBlocksAround(location: Location): List<Block> {
        return getBlocksInRange(location, 1)
    }

    private fun isWithinSearchRadius(start: Location, target: Location): Boolean {
        return start.distance(target) <= MAX_SEARCH_RADIUS
    }
}
