package com.ssg.itemevolution.services

import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.keys.EvolutionKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/**
 * Serviço responsável por acessar e modificar dados persistentes dos itens.
 * Centraliza toda a interação com PersistentDataContainer.
 */
class ItemDataService(
    private val plugin: ItemEvolutionPlugin
) {
    // ===== SOUL TOOL =====

    fun setSoulTool(item: ItemStack, value: Boolean) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(
            EvolutionKey.SOUL_TOOL.key(plugin),
            PersistentDataType.INTEGER,
            if (value) 1 else 0
        )
        item.itemMeta = meta
    }

    // ===== COUNTER =====

    fun getCounter(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 1
        return meta.persistentDataContainer.get(
            EvolutionKey.COUNTER.key(plugin),
            PersistentDataType.INTEGER
        ) ?: 1
    }

    fun setCounter(item: ItemStack, counter: Int) {
        require(counter >= 1) { "Counter deve ser no mínimo 1" }

        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(
            EvolutionKey.COUNTER.key(plugin),
            PersistentDataType.INTEGER,
            counter
        )
        item.itemMeta = meta
    }


    // ===== USES =====

    fun getUses(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        return meta.persistentDataContainer.get(
            EvolutionKey.USES.key(plugin),
            PersistentDataType.INTEGER
        ) ?: 0
    }

    fun setUses(item: ItemStack, uses: Int) {
        require(uses >= 0) { "Uses não pode ser negativo" }

        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(
            EvolutionKey.USES.key(plugin),
            PersistentDataType.INTEGER,
            uses
        )
        item.itemMeta = meta
    }

    // ===== POINTS =====

    fun getPoints(item: ItemStack): Int {
        val meta = item.itemMeta ?: return 0
        return meta.persistentDataContainer.get(
            EvolutionKey.POINTS.key(plugin),
            PersistentDataType.INTEGER
        ) ?: 0
    }

    fun setPoints(item: ItemStack, points: Int) {
        require(points >= 0) { "Points não pode ser negativo" }

        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(
            EvolutionKey.POINTS.key(plugin),
            PersistentDataType.INTEGER,
            points
        )
        item.itemMeta = meta
    }

    fun addPoints(item: ItemStack, points: Int) {
        setPoints(item, getPoints(item) + points)
    }

    /**
     * Copia os dados de evolução entre containers
     */
    fun copyDataContainer(source: PersistentDataContainer, target: PersistentDataContainer) {
        for (evolutionKey in EvolutionKey.entries) {
            val key = evolutionKey.key(plugin)
            val value = source.get(key, PersistentDataType.INTEGER)
            if (value != null) {
                target.set(key, PersistentDataType.INTEGER, value)
            }
        }
    }
}