package com.ssg.itemevolution.enchantments.mining

import com.ssg.itemevolution.ItemEvolutionPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.Sound
import org.bukkit.block.data.BlockData
import java.util.*

class DesabarEnchantment {

    companion object {
        private const val MAX_TREE_SIZE = 200
        private const val MAX_SEARCH_RADIUS = 30
        private const val MAX_LEAF_BRIDGE_DISTANCE = 3

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

        val treeBlocks = getTreeWithLeafBridges(brokenBlock)
        if (treeBlocks.isEmpty()) return

        // Ordenar de cima para baixo
        val sortedBlocks = treeBlocks.sortedByDescending { it.y }

        // Configuração das probabilidades por nível
        val probabilityConfig = mapOf(
            1 to mapOf(0 to 100.0),
            2 to mapOf(
                0 to 25.0,
                1 to 50.0,
                2 to 25.0
            ),
            3 to mapOf(
                0 to 10.0,
                1 to 40.0,
                2 to 30.0,
                3 to 20.0
            )
        )

        val extraBlocks = calculateExtraBlocks(level, probabilityConfig)
        val blocksToBreak = (extraBlocks + 1).coerceAtMost(sortedBlocks.size)

        var brokenCount = 0
        var originalBlockWasBroken = false

        for (i in 0 until blocksToBreak) {
            val block = sortedBlocks[i]
            if (WOOD_MATERIALS.contains(block.type)) {
                if (block.location == brokenBlock.location) {
                    originalBlockWasBroken = true
                }

                block.breakNaturally(tool)
                block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)
                brokenCount++
            }
        }

        player.playSound(player.location, Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f)

        if (!originalBlockWasBroken) {
            Bukkit.getScheduler().runTaskLater(ItemEvolutionPlugin.instance, Runnable {
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
            if (random <= cumulativeProbability) {
                return extraBlocks
            }
        }

        return levelConfig.keys.minOrNull() ?: 0
    }

    private fun getTreeWithLeafBridges(startBlock: Block): List<Block> {
        val troncos = mutableListOf<Block>()
        val visitedTroncos = mutableSetOf<Block>()
        val visitedFolhas = mutableSetOf<Block>()
        val queueTroncos: Queue<Block> = LinkedList()

        queueTroncos.add(startBlock)
        visitedTroncos.add(startBlock)

        while (queueTroncos.isNotEmpty() && troncos.size < MAX_TREE_SIZE) {
            val currentTronco = queueTroncos.poll()
            troncos.add(currentTronco)

            // Buscar troncos adjacentes diretos
            getBlocksAround(currentTronco.location).forEach { nearbyBlock ->
                if (nearbyBlock !in visitedTroncos &&
                    WOOD_MATERIALS.contains(nearbyBlock.type) &&
                    isWithinSearchRadius(startBlock.location, nearbyBlock.location)) {

                    visitedTroncos.add(nearbyBlock)
                    queueTroncos.add(nearbyBlock)
                }
            }

            // Buscar troncos conectados por folhas
            findTrunksThroughLeaves(currentTronco, startBlock.location, visitedTroncos, visitedFolhas)
                .forEach { distantTrunk ->
                    if (distantTrunk !in visitedTroncos) {
                        visitedTroncos.add(distantTrunk)
                        queueTroncos.add(distantTrunk)
                    }
                }
        }

        return if (troncos.size >= MAX_TREE_SIZE) emptyList() else troncos
    }

    private fun findTrunksThroughLeaves(
        startTrunk: Block,
        originalStart: Location,
        visitedTroncos: MutableSet<Block>,
        visitedFolhas: MutableSet<Block>
    ): List<Block> {
        val foundTrunks = mutableListOf<Block>()
        val leafQueue: Queue<Block> = LinkedList()
        val leafVisited = mutableSetOf<Block>()

        // Começar das folhas ao redor do tronco atual (raio 2)
        getBlocksInRange(startTrunk.location, 2).forEach { block ->
            if (LEAF_MATERIALS.contains(block.type) && block !in visitedFolhas) {
                leafQueue.add(block)
                leafVisited.add(block)
                visitedFolhas.add(block)
            }
        }

        var depth = 0
        val maxDepth = MAX_LEAF_BRIDGE_DISTANCE

        while (leafQueue.isNotEmpty() && depth < maxDepth) {
            val currentSize = leafQueue.size

            repeat(currentSize) {
                val currentLeaf = leafQueue.poll()

                // Procurar troncos ao redor desta folha
                getBlocksAround(currentLeaf.location).forEach { nearbyBlock ->
                    if (WOOD_MATERIALS.contains(nearbyBlock.type) &&
                        nearbyBlock !in visitedTroncos &&
                        isWithinSearchRadius(originalStart, nearbyBlock.location)) {

                        foundTrunks.add(nearbyBlock)
                    }
                }

                // Expandir busca através de folhas conectadas
                if (depth < maxDepth - 1) {
                    getBlocksAround(currentLeaf.location).forEach { nearbyBlock ->
                        if (LEAF_MATERIALS.contains(nearbyBlock.type) &&
                            nearbyBlock !in leafVisited &&
                            isWithinSearchRadius(originalStart, nearbyBlock.location)) {

                            leafQueue.add(nearbyBlock)
                            leafVisited.add(nearbyBlock)
                            visitedFolhas.add(nearbyBlock)
                        }
                    }
                }
            }
            depth++
        }

        return foundTrunks
    }

    private fun getBlocksInRange(location: Location, range: Int): List<Block> {
        val blocks = mutableListOf<Block>()
        val world = location.world ?: return blocks

        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    if (x == 0 && y == 0 && z == 0) continue

                    val block = world.getBlockAt(
                        location.blockX + x,
                        location.blockY + y,
                        location.blockZ + z
                    )
                    blocks.add(block)
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