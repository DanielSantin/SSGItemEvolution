package com.ssg.itemevolution.services

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class EnchantmentService() {

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