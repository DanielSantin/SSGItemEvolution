package com.ssg.itemevolution.services

import com.ssg.itemevolution.core.InitializableService
import com.ssg.itemevolution.core.ReloadableService
import com.ssg.itemevolution.keys.ToolCategory
import com.ssg.itemevolution.utils.ConfigManager
import org.bukkit.Material
import org.bukkit.Bukkit

/**
 * Serviço responsável por gerenciar e verificar o cache de blocos que
 * podem ser minerados para evoluir uma ferramenta específica.
 * Usa o padrão ReloadableService para recarregar as configurações.
 */
class ToolBlockService(
    private val configManager: ConfigManager
) : InitializableService, ReloadableService {

    // Cache para a lista de blocos válidos por categoria de ferramenta
    private lateinit var toolMiningBlocks: Map<ToolCategory, Set<Material>>

    override fun initialize() {
        loadToolMiningBlocksCache()
    }

    override fun onConfigReload() {
        loadToolMiningBlocksCache()
    }

    /**
     * Carrega a configuração 'tool-mining-blocks' e popula o cache.
     */
    private fun loadToolMiningBlocksCache() {
        Bukkit.getLogger().info("[SSG] Carregando cache de blocos de mineração de ferramenta...")

        val evolutionConfig = configManager.getCustomConfig("evolution.yml")
        val section = evolutionConfig?.getConfigurationSection("tool-mining-blocks")

        val map = mutableMapOf<ToolCategory, MutableSet<Material>>()
        if (section == null) {
            toolMiningBlocks = map
            Bukkit.getLogger().warning("[SSG] Seção 'tool-mining-blocks' não encontrada em evolution.yml. Nenhuma ferramenta evoluirá por quebra de bloco.")
            return
        }

        ToolCategory.entries.forEach { category ->
            if (ToolCategory.isArmor(category) || category == ToolCategory.UNKNOWN) return@forEach

            // Lê a lista, seja no formato YAML padrão ou inline ([STONE, COBBLESTONE, ...])
            val rawList = section.getString(category.name)
            val materialList = if (rawList != null && rawList.startsWith("[")) {
                // Remove colchetes e divide por vírgula
                rawList.removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            } else {
                section.getStringList(category.name)
            }

            if (materialList.isNotEmpty()) {
                val materials = materialList.mapNotNull { materialName ->
                    try {
                        Material.valueOf(materialName.uppercase())
                    } catch (_: IllegalArgumentException) {
                        Bukkit.getLogger().warning("Material inválido '$materialName' na categoria ${category.name} do 'tool-mining-blocks'.")
                        null
                    }
                }.toSet()
                map[category] = materials.toMutableSet()
            }
        }

        toolMiningBlocks = map
        Bukkit.getLogger().info("[SSG] Cache de blocos de mineração de ferramenta carregado com ${toolMiningBlocks.size} categorias.")
    }

    /**
     * Verifica se um bloco é válido para evolução de uma categoria de ferramenta.
     */
    fun isValidBlockForToolEvolution(toolMaterial: Material, blockMaterial: Material): Boolean {
        val toolCategory = ToolCategory.fromMaterial(toolMaterial)

        if (ToolCategory.isArmor(toolCategory) ||
            toolCategory == ToolCategory.BOW ||
            toolCategory == ToolCategory.CROSSBOW ||
            toolCategory == ToolCategory.SHIELD ||
            toolCategory == ToolCategory.UNKNOWN
        ) return false

        val allowedBlocks = toolMiningBlocks[toolCategory] ?: return false
        return blockMaterial in allowedBlocks
    }
}
