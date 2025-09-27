package com.ssg.itemevolution

import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

object EnchantmentHandler {

    /**
     * Verifica se um item possui um encantamento específico
     * Funciona tanto para encantamentos vanilla quanto data-driven
     */
    fun hasEnchantment(item: ItemStack, enchantmentKey: String): Boolean {
        val enchantment = getEnchantment(enchantmentKey) ?: return false
        return item.containsEnchantment(enchantment)
    }

    /**
     * Obtém o nível de um encantamento específico
     * Funciona tanto para encantamentos vanilla quanto data-driven
     */
    fun getEnchantmentLevel(item: ItemStack, enchantmentKey: String): Int {
        val enchantment = getEnchantment(enchantmentKey) ?: return 0
        return item.getEnchantmentLevel(enchantment)
    }

    /**
     * Adiciona um encantamento ao item
     * Funciona tanto para encantamentos vanilla quanto data-driven
     */
    fun enchantItem(item: ItemStack, enchantmentKey: String, level: Int): ItemStack {
        val newItem = item.clone()
        val enchantment = getEnchantment(enchantmentKey) ?: return newItem
        newItem.addUnsafeEnchantment(enchantment, level)
        return newItem
    }

    /**
     * Remove um encantamento do item
     */
    fun removeEnchantment(item: ItemStack, enchantmentKey: String): ItemStack {
        val newItem = item.clone()
        val enchantment = getEnchantment(enchantmentKey) ?: return newItem
        newItem.removeEnchantment(enchantment)
        return newItem
    }

    /**
     * Obtém o objeto Enchantment a partir de uma string
     * Suporta tanto nomes simples quanto namespaced keys
     */
    private fun getEnchantment(enchantmentKey: String): Enchantment? {
        // Se já contém ":", trata como namespaced key
        val key = if (enchantmentKey.contains(":")) {
            NamespacedKey.fromString(enchantmentKey)
        } else {
            // Primeiro tenta como vanilla (minecraft:)
            NamespacedKey.minecraft(enchantmentKey.lowercase())
        }

        if (key != null) {
            val enchantment = Enchantment.getByKey(key)
            if (enchantment != null) {
                return enchantment
            }
        }

        // Se não encontrou como minecraft:, tenta com outros namespaces comuns
        if (!enchantmentKey.contains(":")) {
            // Tenta com alguns namespaces comuns para encantamentos customizados
            val commonNamespaces = listOf("supera", "ssg", "custom")

            for (namespace in commonNamespaces) {
                val customKey = NamespacedKey.fromString("$namespace:${enchantmentKey.lowercase()}")
                if (customKey != null) {
                    val enchantment = Enchantment.getByKey(customKey)
                    if (enchantment != null) {
                        return enchantment
                    }
                }
            }
        }

        return null
    }

    /**
     * Lista todos os encantamentos presentes em um item
     */
    fun getItemEnchantments(item: ItemStack): Map<Enchantment, Int> {
        return item.enchantments
    }

    /**
     * Verifica se um encantamento está registrado no servidor
     */
    fun isEnchantmentRegistered(enchantmentKey: String): Boolean {
        return getEnchantment(enchantmentKey) != null
    }
}