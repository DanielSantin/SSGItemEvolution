package com.ssg.itemevolution

import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey

object EnchantmentHandler {

    private val customEnchantmentsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc")
    private val customEnchantmentLevelsKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc_lvl")
    private val customEnchantmentDisplayKey = NamespacedKey(ItemEvolutionPlugin.instance, "custom_enc_display")

    /**
     * Adiciona encantamento à lista customizada (para lore)
     */
    fun addToCustomEnchantmentList(item: ItemStack, enchantName: String, level: Int, displayEnchantment: Boolean = true) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",")?.toMutableList() ?: mutableListOf()
        val levels = container.get(customEnchantmentLevelsKey, PersistentDataType.STRING)?.split(",")?.toMutableList() ?: mutableListOf()
        val displayFlags = container.get(customEnchantmentDisplayKey, PersistentDataType.STRING)?.split(",")?.toMutableList() ?: mutableListOf()

        val index = enchantments.indexOf(enchantName)
        if (index >= 0) {
            // Atualizar nível e flag de display existentes
            levels[index] = level.toString()
            displayFlags[index] = displayEnchantment.toString()
        } else {
            // Adicionar novo encantamento
            enchantments.add(enchantName)
            levels.add(level.toString())
            displayFlags.add(displayEnchantment.toString())
        }

        container.set(customEnchantmentsKey, PersistentDataType.STRING, enchantments.joinToString(","))
        container.set(customEnchantmentLevelsKey, PersistentDataType.STRING, levels.joinToString(","))
        container.set(customEnchantmentDisplayKey, PersistentDataType.STRING, displayFlags.joinToString(","))

        item.itemMeta = meta
    }

    /**
     * Converte nome de encantamento para encantamento vanilla do Bukkit
     */
    fun getVanillaEnchantment(enchantName: String): Enchantment? {
        return when (enchantName.lowercase()) {
            "efficiency", "eficiência" -> Enchantment.EFFICIENCY
            "fortune", "fortuna" -> Enchantment.FORTUNE
            "silk_touch", "toque_suave" -> Enchantment.SILK_TOUCH
            "sharpness", "afiação" -> Enchantment.SHARPNESS
            "smite", "punição" -> Enchantment.SMITE
            "bane_of_arthropods", "flagelo_dos_artrópodes" -> Enchantment.BANE_OF_ARTHROPODS
            "looting", "pilhagem" -> Enchantment.LOOTING
            "SWEEPING_EDGE", "ataque_em_área" -> Enchantment.SWEEPING_EDGE
            "fire_aspect", "aspecto_flamejante" -> Enchantment.FIRE_ASPECT
            "knockback", "repulsão" -> Enchantment.KNOCKBACK
            "protection", "proteção" -> Enchantment.PROTECTION
            "fire_protection", "proteção_contra_fogo" -> Enchantment.FIRE_PROTECTION
            "blast_protection", "proteção_contra_explosões" -> Enchantment.BLAST_PROTECTION
            "projectile_protection", "proteção_contra_projéteis" -> Enchantment.PROJECTILE_PROTECTION
            "respiration", "respiração" -> Enchantment.RESPIRATION
            "aqua_affinity", "afinidade_aquática" -> Enchantment.AQUA_AFFINITY
            "thorns", "espinhos" -> Enchantment.THORNS
            "depth_strider", "passos_profundos" -> Enchantment.DEPTH_STRIDER
            "frost_walker", "andarilho_do_gelo" -> Enchantment.FROST_WALKER
            "feather_falling", "queda_suave" -> Enchantment.FEATHER_FALLING
            "power", "força" -> Enchantment.POWER
            "punch", "impacto" -> Enchantment.PUNCH
            "flame", "chama" -> Enchantment.FLAME
            "infinity", "infinidade" -> Enchantment.INFINITY
            "unbreaking", "inquebrável" -> Enchantment.UNBREAKING
            "mending", "reparação" -> Enchantment.MENDING
            "loyalty", "lealdade" -> Enchantment.LOYALTY
            "impaling", "perfuração" -> Enchantment.IMPALING
            "riptide", "correnteza" -> Enchantment.RIPTIDE
            "channeling", "canalização" -> Enchantment.CHANNELING
            else -> null // Encantamentos customizados/fake
        }
    }



//    /**
//     * Obtém todos os encantamentos customizados de um item
//     */
//    fun getCustomEnchantments(item: ItemStack): Map<String, Int> {
//        val meta = item.itemMeta ?: return emptyMap()
//        val container = meta.persistentDataContainer
//
//        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",") ?: return emptyMap()
//        val levels = container.get(customEnchantmentLevelsKey, PersistentDataType.STRING)?.split(",") ?: return emptyMap()
//
//        val result = mutableMapOf<String, Int>()
//        for (i in enchantments.indices) {
//            if (i < levels.size) {
//                val level = levels[i].toIntOrNull() ?: 1
//                result[enchantments[i]] = level
//            }
//        }
//
//        return result
//    }

    fun quickHasCustomEnchantment(item: ItemStack, enchantName: String): Boolean {
        val meta = item.itemMeta ?: return false
        val enchantments = meta.persistentDataContainer.get(customEnchantmentsKey, PersistentDataType.STRING)
        return enchantments?.contains(enchantName) == true
    }

    fun getCustomEnchantmentLevel(item: ItemStack, enchantName: String): Int {
        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer

        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING) ?: return 0
        val levels = container.get(customEnchantmentLevelsKey, PersistentDataType.STRING) ?: return 0

        // Split apenas uma vez para eficiência
        val enchantmentList = enchantments.split(",")
        val levelList = levels.split(",")

        // Procurar o índice do encantamento
        val index = enchantmentList.indexOf(enchantName)

        return if (index >= 0 && index < levelList.size) {
            levelList[index].toIntOrNull() ?: 0
        } else {
            0
        }
    }

    fun getEnchantmentLevel(item: ItemStack, enchantName: String): Int {
        val vanillaEnchant = getVanillaEnchantment(enchantName)
        if (vanillaEnchant != null) {
            return item.getEnchantmentLevel(vanillaEnchant)
        } else {
            return getCustomEnchantmentLevel(item, enchantName)
        }
    }

    fun enchantItem(item: ItemStack, enchantName: String, level: Int): ItemStack {
        val newItem = item.clone()
        val vanillaEnchant = getVanillaEnchantment(enchantName)
        if (vanillaEnchant != null) {
            newItem.addUnsafeEnchantment(vanillaEnchant, level)
            //addToCustomEnchantmentList(newItem, enchantName, level, displayEnchantment = false)
        } else {
            addToCustomEnchantmentList(newItem, enchantName, level)
        }
        return newItem
    }
}