package com.ssg.itemevolution.services

import com.ssg.itemevolution.utils.ConfigManager
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class EnchantmentService(
    private val configManager: ConfigManager
    ) {

    // Cache dos grupos de encantamentos para melhor performance
    private val enchantmentGroups: Map<String, List<String>> by lazy {
        val groups = mutableMapOf<String, List<String>>()
        val config = configManager.getCustomConfig("enchantments.yml") ?: return@lazy groups

        val groupsSection = config.getConfigurationSection("enchantment-groups")
        groupsSection?.getKeys(false)?.forEach { groupName ->
            val groupSection = groupsSection.getConfigurationSection(groupName)
            val isMutuallyExclusive = groupSection?.getBoolean("mutually-exclusive", false) ?: false

            if (isMutuallyExclusive) {
                val enchantments = groupSection.getStringList("enchantments")
                groups[groupName] = enchantments
            }
        }

        groups
    }

    // Cache dos conflitos customizados
    private val customConflicts: Map<String, List<String>> by lazy {
        val conflicts = mutableMapOf<String, List<String>>()
        val config = configManager.getCustomConfig("enchantments.yml") ?: return@lazy conflicts

        val conflictsSection = config.getConfigurationSection("custom-conflicts")
        conflictsSection?.getKeys(false)?.forEach { enchantName ->
            val conflictList = conflictsSection.getStringList(enchantName)
            conflicts[enchantName] = conflictList
        }

        conflicts
    }

    fun enchantItem(item: ItemStack, enchantmentKey: String, level: Int): ItemStack {
        val newItem = item.clone()
        val enchantment = getEnchantment(enchantmentKey) ?: return newItem
        newItem.addUnsafeEnchantment(enchantment, level)
        return newItem
    }

    fun hasEnchantment(item: ItemStack, enchantmentKey: String): Boolean {
        val enchantment = getEnchantment(enchantmentKey) ?: return false
        return item.containsEnchantment(enchantment)
    }

    fun getEnchantmentLevel(item: ItemStack, enchantmentKey: String): Int {
        val enchantment = getEnchantment(enchantmentKey) ?: return 0
        return item.getEnchantmentLevel(enchantment)
    }

    fun listEnchantments(item: ItemStack): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for ((enchant, level) in item.enchantments) {
            val key = enchant.key.toString()
            result[key] = level
        }
        return result
    }

    /**
     * Verifica se um encantamento é compatível com os encantamentos já presentes no item
     * @param item ItemStack a verificar
     * @param newEnchantment Nome do encantamento que deseja adicionar
     * @return true se compatível, false se houver conflito
     */
    fun isEnchantmentCompatible(item: ItemStack, newEnchantment: String): Boolean {
        val currentEnchantments = getCurrentEnchantmentNames(item)

        // Verifica conflitos em grupos de exclusão mútua
        for ((_, groupEnchantments) in enchantmentGroups) {
            if (newEnchantment in groupEnchantments) {
                // Se o novo encantamento está neste grupo, verifica se já existe outro do mesmo grupo
                val hasConflict = currentEnchantments.any { it in groupEnchantments && it != newEnchantment }
                if (hasConflict) {
                    return false
                }
            }
        }

        // Verifica conflitos customizados
        val conflicts = customConflicts[newEnchantment] ?: emptyList()
        if (currentEnchantments.any { it in conflicts }) {
            return false
        }

        // Verifica conflitos reversos (se algum encantamento atual conflita com o novo)
        for (currentEnch in currentEnchantments) {
            val currentConflicts = customConflicts[currentEnch] ?: emptyList()
            if (newEnchantment in currentConflicts) {
                return false
            }
        }

        return true
    }

    /**
     * Obtém os nomes dos encantamentos presentes no item
     * @param item ItemStack a verificar
     * @return Lista com os nomes dos encantamentos (sem namespace)
     */
    fun getCurrentEnchantmentNames(item: ItemStack): List<String> {
        return item.enchantments.keys.map { it.key.toString() } // mantém o namespace
    }

    /**
     * Retorna lista de encantamentos conflitantes com o encantamento fornecido
     * @param enchantmentName Nome do encantamento
     * @return Lista de encantamentos que conflitam
     */
    fun getConflictingEnchantments(enchantmentName: String): List<String> {
        val conflicts = mutableListOf<String>()

        // Adiciona conflitos do mesmo grupo
        for ((_, groupEnchantments) in enchantmentGroups) {
            if (enchantmentName in groupEnchantments) {
                conflicts.addAll(groupEnchantments.filter { it != enchantmentName })
            }
        }

        // Adiciona conflitos customizados
        customConflicts[enchantmentName]?.let { conflicts.addAll(it) }

        // Adiciona conflitos reversos
        for ((enchant, conflictList) in customConflicts) {
            if (enchantmentName in conflictList && enchant !in conflicts) {
                conflicts.add(enchant)
            }
        }

        return conflicts.distinct()
    }

    /**
     * Obtém o objeto Enchantment a partir de uma string
     */
    fun getEnchantment(enchantmentKey: String): Enchantment? {
        // 1. Tenta encontrar a chave diretamente.
        val keyFromInput = NamespacedKey.fromString(enchantmentKey)

        if (keyFromInput != null) {
            val enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(keyFromInput)
            if (enchantment != null) {
                return enchantment
            }
        }

        return null
    }
}