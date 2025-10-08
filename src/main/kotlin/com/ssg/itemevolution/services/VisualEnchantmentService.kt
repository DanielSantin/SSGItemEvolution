package com.ssg.itemevolution.services

import com.ssg.itemevolution.core.InitializableService
import com.ssg.itemevolution.core.ReloadableService
import com.ssg.itemevolution.utils.ConfigManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

/**
 * Serviço responsável por gerenciar encantamentos visuais
 * que alteram o modelo 3D dos itens.
 *
 * Estrutura: Map<EnchantmentKey, Map<Material, ModelPath>>
 * Exemplo: {"supera:area_mining" -> {DIAMOND_PICKAXE -> "supera:diamond_hammer"}}
 */
class VisualEnchantmentService(
    private val configManager: ConfigManager,
    private val plugin: com.ssg.itemevolution.ItemEvolutionPlugin
) : InitializableService, ReloadableService {

    // Mapa: enchantmentKey -> (material -> modelPath)
    private var visualEnchantments: Map<String, Map<Material, String>> = emptyMap()

    override fun initialize() {
        loadVisualConfigurations()
    }

    override fun onConfigReload() {
        loadVisualConfigurations()
    }

    /**
     * Carrega as configurações de encantamentos visuais do YAML
     */
    private fun loadVisualConfigurations() {
        val visualConfig = configManager.getCustomConfig("visual_enchantments.yml")
        if (visualConfig == null) {
            plugin.logger.warning("⚠️ Arquivo visual_enchantments.yml não encontrado!")
            return
        }

        val enchantSection = visualConfig.getConfigurationSection("visual_enchantments")
        if (enchantSection == null) {
            plugin.logger.warning("⚠️ Seção 'visual_enchantments' não encontrada!")
            return
        }

        val loadedEnchantments = mutableMapOf<String, Map<Material, String>>()

        // Para cada encantamento (ex: "supera:area_mining")
        for (enchantKey in enchantSection.getKeys(false)) {
            val materialSection = enchantSection.getConfigurationSection(enchantKey)
            if (materialSection == null) {
                plugin.logger.warning("⚠️ Encantamento '$enchantKey' não tem materiais configurados!")
                continue
            }

            val materialMap = mutableMapOf<Material, String>()

            // Para cada material (ex: "DIAMOND_PICKAXE")
            for (materialName in materialSection.getKeys(false)) {
                val material = Material.getMaterial(materialName)
                val modelPath = materialSection.getString(materialName)

                if (material != null && modelPath != null) {
                    materialMap[material] = modelPath
                    plugin.logger.info("  ✓ $enchantKey -> $materialName = $modelPath")
                } else {
                    plugin.logger.warning("  ✗ Material inválido ou modelo nulo: $materialName")
                }
            }

            if (materialMap.isNotEmpty()) {
                loadedEnchantments[enchantKey] = materialMap
            }
        }

        visualEnchantments = loadedEnchantments

        plugin.logger.info("✅ Visual Enchantments carregados!")
        plugin.logger.info("   Total de encantamentos: ${visualEnchantments.size}")
        plugin.logger.info("   Total de modelos: ${visualEnchantments.values.sumOf { it.size }}")
    }

    /**
     * Aplica o modelo visual apropriado ao item baseado no encantamento
     *
     * @param item O item que receberá o modelo visual
     * @param enchantmentKey A chave do encantamento (ex: "supera:area_mining")
     * @return true se o modelo foi aplicado, false caso contrário
     */
    fun applyVisualModel(item: ItemStack, enchantmentKey: String): Boolean {
        val meta = item.itemMeta ?: return false

        // Busca o mapa de materiais para este encantamento
        val materialMap = visualEnchantments[enchantmentKey]
        if (materialMap == null) {
            plugin.logger.warning("⚠️ Encantamento '$enchantmentKey' não possui modelos visuais configurados")
            return false
        }

        // Busca o modelo para o material específico
        val modelPath = materialMap[item.type]
        if (modelPath == null) {
            plugin.logger.warning("⚠️ Material ${item.type} não possui modelo visual para '$enchantmentKey'")
            return false
        }

        // Aplica o modelo
        val modelKey = NamespacedKey.fromString(modelPath)
        if (modelKey != null) {
            meta.itemModel = modelKey
            item.itemMeta = meta
            plugin.logger.info("✓ Modelo visual aplicado: $modelPath para ${item.type}")
            return true
        } else {
            plugin.logger.warning("✗ Chave de modelo inválida: $modelPath")
            return false
        }
    }

    /**
     * Atualiza o modelo visual quando o item é melhorado (upgrade)
     * Procura o modelo do novo material mantendo o mesmo encantamento
     *
     * @param oldItem Item antes do upgrade
     * @param newItem Item após o upgrade
     * @return true se o modelo foi atualizado, false caso contrário
     */
    fun updateVisualModelOnUpgrade(oldItem: ItemStack, newItem: ItemStack): Boolean {
        val oldMeta = oldItem.itemMeta ?: return false

        // Verifica se o item antigo tinha um modelo visual
        if (!oldMeta.hasItemModel()) return false

        val oldModelKey = oldMeta.itemModel ?: return false
        val oldModelPath = "${oldModelKey.namespace}:${oldModelKey.key}"

        // Procura qual encantamento tinha esse modelo no material antigo
        val enchantmentKey = findEnchantmentByModel(oldItem.type, oldModelPath)
        if (enchantmentKey == null) {
            plugin.logger.warning("⚠️ Não foi possível identificar o encantamento do modelo: $oldModelPath")
            return false
        }

        // Aplica o modelo do mesmo encantamento para o novo material
        return applyVisualModel(newItem, enchantmentKey)
    }

    /**
     * Busca qual encantamento está usando um modelo específico para um material
     */
    private fun findEnchantmentByModel(material: Material, modelPath: String): String? {
        for ((enchantKey, materialMap) in visualEnchantments) {
            if (materialMap[material] == modelPath) {
                return enchantKey
            }
        }
        return null
    }

    /**
     * Verifica se um encantamento específico possui modelo visual para algum material
     */
    fun hasVisualModel(enchantmentKey: String): Boolean {
        return visualEnchantments.containsKey(enchantmentKey)
    }

}