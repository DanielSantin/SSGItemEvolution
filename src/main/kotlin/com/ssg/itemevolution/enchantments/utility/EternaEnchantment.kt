package com.ssg.itemevolution.enchantments.utility

import com.ssg.itemevolution.ItemEvolutionPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

class EternaEnchantment() {
    companion object {
        // Chave para marcar itens como "quebrados" mas preservados
        private lateinit var BROKEN_KEY: NamespacedKey

        fun initializeKeys(plugin: ItemEvolutionPlugin) {
            BROKEN_KEY = NamespacedKey(plugin, "eterna_broken")
        }

        /**
         * Verifica se um item está marcado como quebrado
         */
        fun isItemBroken(item: ItemStack): Boolean {
            val meta = item.itemMeta ?: return false
            val dataContainer = meta.persistentDataContainer
            return dataContainer.has(BROKEN_KEY, PersistentDataType.INTEGER)
        }

        /**
         * Repara um item quebrado (para usar em bigornas ou outros métodos)
         */
        fun removeBrokenItemMark(item: ItemStack): Boolean {
            if (!isItemBroken(item)) return false
            val meta = item.itemMeta ?: return false
            val dataContainer = meta.persistentDataContainer

            dataContainer.remove(BROKEN_KEY)
            item.itemMeta = meta
            return true
        }
    }

    /**
     * Verifica se o item deveria quebrar e aplica a preservação se necessário
     */
    fun checkAndPreserveItem(player: Player, item: ItemStack): Boolean {
        val meta = item.itemMeta as? Damageable ?: return false
        val maxDurability = item.type.maxDurability.toInt()

        // Se a durabilidade está quase no limite e não está marcada como quebrada
        if (meta.damage >= maxDurability - 2 && !isItemBroken(item)) {
            meta.damage = maxDurability - 2
            item.itemMeta = meta
            markItemAsBroken(item, player)
            return true
        }

        return false
    }


    /**
     * Marca um item como quebrado mas preservado
     */
    private fun markItemAsBroken(item: ItemStack, player: Player) {
        val meta = item.itemMeta ?: return
        val dataContainer = meta.persistentDataContainer

        dataContainer.set(BROKEN_KEY, PersistentDataType.INTEGER, 1)
        item.itemMeta = meta

        // Efeitos visuais e sonoros
        val materialName = item.type.name.lowercase()
        player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f)
        player.sendMessage(Component.text("Sua $materialName quebrou mas foi preservada pelo encantamento!", NamedTextColor.RED))
        player.sendMessage(Component.text("Repare-a em uma bancada para usar novamente.", NamedTextColor.YELLOW))
    }

    /**
     * Previne o uso de ferramentas quebradas
     */
    fun preventBrokenItemUse(player: Player, item: ItemStack): Boolean {
        if (isItemBroken(item)) {
            player.sendMessage(Component.text("Esta ferramenta está danificada e não pode ser usada!", NamedTextColor.RED))
            player.sendMessage(Component.text("Repare-a em uma bancada primeiro.", NamedTextColor.YELLOW))
            player.playSound(player.location, Sound.BLOCK_COPPER_HIT, 1.0f, 2f)
            return true // Bloqueia o uso
        }
        return false // Permite o uso
    }
}

