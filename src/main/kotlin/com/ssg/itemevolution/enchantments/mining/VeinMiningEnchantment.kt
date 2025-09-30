package com.ssg.itemevolution.enchantments.mining

import com.ssg.itemevolution.ItemEvolutionPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.Sound
import java.util.*
import kotlin.math.sqrt

class VeinMiningEnchantment(
    private val plugin: ItemEvolutionPlugin
) {
    companion object {
        private const val MAX_VEIN_SIZE = 200
        private const val MAX_SEARCH_RADIUS = 30

        // Materiais de minério
        private val ORE_MATERIALS = setOf(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
        )

        // Materiais de pedra
        private val STONE_MATERIALS = setOf(
            Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.DEEPSLATE, Material.TUFF, Material.CALCITE, Material.SMOOTH_BASALT,
            Material.COBBLESTONE, Material.COBBLED_DEEPSLATE,
            Material.NETHERRACK, Material.BLACKSTONE, Material.BASALT,
            Material.END_STONE
        )

        // Materiais de terra/areia
        private val EARTH_MATERIALS = setOf(
            Material.DIRT, Material.COARSE_DIRT, Material.PODZOL, Material.MYCELIUM,
            Material.GRASS_BLOCK, Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.CLAY, Material.TERRACOTTA, Material.SOUL_SAND, Material.SOUL_SOIL
        )

        // Todos os materiais válidos para vein mining
        private val VEIN_MATERIALS = ORE_MATERIALS
    }

    fun executeVeinMining(player: Player, brokenBlock: Block, tool: ItemStack, level: Int, originalType: Material, originalData: BlockData) {
        if (!VEIN_MATERIALS.contains(brokenBlock.type)) return
        plugin.logger.info("VeinMiningEnchantment - executeVeinMining")

        val veinBlocksWithDistance = getVeinBlocks(brokenBlock)
        if (veinBlocksWithDistance.isEmpty()) return

        // Ordena os blocos pela distância de caminho (mais distantes primeiro)
        // Em caso de empate na distância, ordena por distância euclidiana
        val sortedBlocks = veinBlocksWithDistance.entries.sortedWith(
            compareByDescending<Map.Entry<Block, Int>> { it.value } // Distância de caminho primeiro
                .thenByDescending { getDistance(brokenBlock.location, it.key.location) } // Distância euclidiana como desempate
        ).map { it.key }

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
            if (VEIN_MATERIALS.contains(block.type) && block.type == brokenBlock.type) {
                if (block.location == brokenBlock.location) {
                    originalBlockWasBroken = true
                }

                block.breakNaturally(tool)

                // Efeito visual baseado no tipo de material
                when {
                    ORE_MATERIALS.contains(block.type) -> {
                        block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)
                        player.playSound(block.location, Sound.BLOCK_STONE_BREAK, 0.5f, 1.2f)
                    }
                    STONE_MATERIALS.contains(block.type) -> {
                        block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)
                        player.playSound(block.location, Sound.BLOCK_STONE_BREAK, 0.3f, 0.8f)
                    }
                    EARTH_MATERIALS.contains(block.type) -> {
                        block.world.playEffect(block.location, org.bukkit.Effect.STEP_SOUND, block.type)
                        player.playSound(block.location, Sound.BLOCK_GRAVEL_BREAK, 0.3f, 0.9f)
                    }
                }
            }
        }

        // Som principal baseado no material quebrado
        val mainSound = when {
            ORE_MATERIALS.contains(originalType) -> Sound.BLOCK_STONE_BREAK
            STONE_MATERIALS.contains(originalType) -> Sound.BLOCK_STONE_BREAK
            EARTH_MATERIALS.contains(originalType) -> Sound.BLOCK_GRAVEL_BREAK
            else -> Sound.BLOCK_STONE_BREAK
        }

        player.playSound(player.location, mainSound, 1.0f, 0.8f)

        // Restaura o bloco original se não foi quebrado
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

    private fun getVeinBlocks(startBlock: Block): Map<Block, Int> {
        val veinBlocks = mutableMapOf<Block, Int>() // Block -> distância de caminho
        val visited = mutableSetOf<Block>()
        val queue: Queue<Pair<Block, Int>> = LinkedList() // Block, distância
        val targetMaterial = startBlock.type

        queue.add(Pair(startBlock, 0))
        visited.add(startBlock)
        veinBlocks[startBlock] = 0

        while (queue.isNotEmpty() && veinBlocks.size < MAX_VEIN_SIZE) {
            val (currentBlock, currentDistance) = queue.poll()

            // Busca blocos adjacentes do mesmo material
            getBlocksAround(currentBlock.location).forEach { nearbyBlock ->
                if (nearbyBlock !in visited &&
                    nearbyBlock.type == targetMaterial &&
                    isWithinSearchRadius(startBlock.location, nearbyBlock.location)
                ) {
                    val newDistance = currentDistance + 1
                    visited.add(nearbyBlock)
                    veinBlocks[nearbyBlock] = newDistance
                    queue.add(Pair(nearbyBlock, newDistance))
                }
            }
        }

        return if (veinBlocks.size >= MAX_VEIN_SIZE) emptyMap() else veinBlocks
    }

    private fun getBlocksAround(location: Location): List<Block> {
        val blocks = mutableListOf<Block>()
        val world = location.world ?: return blocks

        // Verifica blocos adjacentes (6 direções: cima, baixo, norte, sul, leste, oeste)
        val directions = listOf(
            intArrayOf(0, 1, 0),   // cima
            intArrayOf(0, -1, 0),  // baixo
            intArrayOf(1, 0, 0),   // leste
            intArrayOf(-1, 0, 0),  // oeste
            intArrayOf(0, 0, 1),   // sul
            intArrayOf(0, 0, -1),  // norte
            // Diagonais horizontais
            intArrayOf(1, 0, 1),
            intArrayOf(1, 0, -1),
            intArrayOf(-1, 0, 1),
            intArrayOf(-1, 0, -1),
            // Diagonais verticais
            intArrayOf(1, 1, 0),
            intArrayOf(-1, 1, 0),
            intArrayOf(0, 1, 1),
            intArrayOf(0, 1, -1),
            intArrayOf(1, -1, 0),
            intArrayOf(-1, -1, 0),
            intArrayOf(0, -1, 1),
            intArrayOf(0, -1, -1)
        )

        for (direction in directions) {
            val block = world.getBlockAt(
                location.blockX + direction[0],
                location.blockY + direction[1],
                location.blockZ + direction[2]
            )
            blocks.add(block)
        }

        return blocks
    }

    private fun isWithinSearchRadius(start: Location, target: Location): Boolean {
        return getDistance(start, target) <= MAX_SEARCH_RADIUS
    }

    private fun getDistance(loc1: Location, loc2: Location): Double {
        val dx = loc1.x - loc2.x
        val dy = loc1.y - loc2.y
        val dz = loc1.z - loc2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}