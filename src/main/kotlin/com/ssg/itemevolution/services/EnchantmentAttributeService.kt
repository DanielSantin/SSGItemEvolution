package com.ssg.itemevolution.services

import com.ssg.itemevolution.core.InitializableService
import com.ssg.itemevolution.core.ReloadableService
import com.ssg.itemevolution.utils.ConfigManager
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.attribute.AttributeModifier.Operation
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey

// Data class para armazenar os detalhes do modificador de atributo
private data class AttributeModifierDetails(
    val value: Double,
    val operation: Operation,
    val slot: EquipmentSlotGroup
)

class EnchantmentAttributeService(
    private val configManager: ConfigManager
) : InitializableService, ReloadableService {

    // Novo mapa para armazenar os detalhes completos: Encantamento -> Atributo -> Nível -> Detalhes
    private val enchantmentAttributeMap = mutableMapOf<String, MutableMap<String, Map<Int, AttributeModifierDetails>>>()

    override fun initialize() {
        loadAttributeEffects()
    }

    override fun onConfigReload() {
        loadAttributeEffects()
    }

    private fun loadAttributeEffects() {
        enchantmentAttributeMap.clear()
        val config = configManager.getCustomConfig("enchantments.yml") ?: return
        val effectsSection = config.getConfigurationSection("attribute-effects") ?: return

        for (enchKey in effectsSection.getKeys(false)) {
            val attrSection = effectsSection.getConfigurationSection(enchKey) ?: continue
            val attrMap = mutableMapOf<String, Map<Int, AttributeModifierDetails>>()

            for (attrName in attrSection.getKeys(false)) {
                val levelSection = attrSection.getConfigurationSection(attrName) ?: continue
                val levelMap = mutableMapOf<Int, AttributeModifierDetails>()

                for (levelKey in levelSection.getKeys(false)) {
                    val level = levelKey.toIntOrNull() ?: continue
                    val levelDetailsSection = levelSection.getConfigurationSection(levelKey) ?: continue

                    // Extrai e mapeia os novos campos
                    val value = levelDetailsSection.getDouble("value")
                    val operationName = levelDetailsSection.getString("operation") ?: continue
                    val slotName = levelDetailsSection.getString("slot") ?: continue

                    val operation = getOperationByName(operationName) ?: continue
                    val slot = getSlotGroupByName(slotName) ?: continue

                    levelMap[level] = AttributeModifierDetails(value, operation, slot)
                }

                attrMap[attrName] = levelMap
            }
            enchantmentAttributeMap[enchKey] = attrMap
        }
    }

    fun applyCustomEnchantmentAttributes(item: ItemStack) {
        val meta = item.itemMeta ?: return

        val modifiersToRemove = meta.attributeModifiers?.entries()?.filter {
            it.value.key.namespace.equals("supera", ignoreCase = true)
        }?.toList() ?: emptyList()

        for ((attribute, modifier) in modifiersToRemove) {
            meta.removeAttributeModifier(attribute, modifier)
        }

        val enchantments = item.enchantments
        for ((enchantment, level) in enchantments) {
            val enchKey = enchantment.key.toString()
            val attrMap = enchantmentAttributeMap[enchKey] ?: continue

            for ((attrName, levelMap) in attrMap) {
                val details = levelMap[level] ?: continue
                if (details.value == 0.0 && details.operation != Operation.ADD_NUMBER) continue

                val attribute = getAttributeByName(attrName) ?: continue

                // Cria o NamespacedKey usando o namespace "supera" e o nome do encantamento e atributo
                val safeEnchKey = enchantment.key.key
                val namespacedKey = NamespacedKey("supera", "${safeEnchKey}_$attrName")

                // Cria o modificador usando os detalhes carregados
                val modifier = AttributeModifier(
                    namespacedKey,
                    details.value,
                    details.operation,
                    details.slot
                )

                meta.addAttributeModifier(attribute, modifier)
            }
        }

        item.itemMeta = meta
    }

    private fun getAttributeByName(name: String): Attribute? {
        val key = NamespacedKey.minecraft(name.lowercase())
        return Registry.ATTRIBUTE.get(key)
    }

    /**
     * Mapeia uma string para o enum Operation.
     */
    private fun getOperationByName(name: String): Operation? {
        return when (name.uppercase()) {
            "ADD_NUMBER" -> Operation.ADD_NUMBER
            "MULTIPLY_SCALAR_1" -> Operation.MULTIPLY_SCALAR_1
            "ADD_SCALAR" -> Operation.ADD_SCALAR
            else -> null
        }
    }

    /**
     * Mapeia uma string para o enum EquipmentSlotGroup.
     */
    private fun getSlotGroupByName(name: String): EquipmentSlotGroup? {
        return when (name.uppercase()) {
            "ANY" -> EquipmentSlotGroup.ANY
            "HAND" -> EquipmentSlotGroup.HAND
            "ARMOR" -> EquipmentSlotGroup.ARMOR
            "HEAD" -> EquipmentSlotGroup.HEAD
            "BODY" -> EquipmentSlotGroup.BODY
            "CHEST" -> EquipmentSlotGroup.CHEST
            "LEGS" -> EquipmentSlotGroup.LEGS
            "FEET" -> EquipmentSlotGroup.FEET
            "MAINHAND" -> EquipmentSlotGroup.MAINHAND
            "OFFHAND" -> EquipmentSlotGroup.OFFHAND
            "SADDLE" -> EquipmentSlotGroup.SADDLE // Adicionado SADDLE conforme sua lista
            else -> null
        }
    }
}