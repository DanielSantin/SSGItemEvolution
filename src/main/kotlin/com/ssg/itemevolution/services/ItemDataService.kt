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

    fun isSoulTool(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.get(
            EvolutionKey.SOUL_TOOL.key(plugin),
            PersistentDataType.INTEGER
        ) == 1
    }

    fun setSoulTool(item: ItemStack, value: Boolean) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(
            EvolutionKey.SOUL_TOOL.key(plugin),
            PersistentDataType.INTEGER,
            if (value) 1 else 0
        )
        item.itemMeta = meta
    }

    // ===== LEVEL =====
    // REMOVIDO: A dependência circular foi quebrada
    // Agora ItemEvolutionService chama setCounter diretamente

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

    // REMOVIDO: setCounterBasedOnLevel (causava dependência circular)
    // Essa lógica foi movida para ItemEvolutionService

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

    fun incrementUses(item: ItemStack) {
        setUses(item, getUses(item) + 1)
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

    fun removePoints(item: ItemStack, points: Int): Boolean {
        val currentPoints = getPoints(item)
        if (currentPoints < points) return false

        setPoints(item, currentPoints - points)
        return true
    }

    // ===== BULK OPERATIONS =====

    /**
     * Copia todos os dados de evolução de um item para outro
     */
    fun copyEvolutionData(source: ItemStack, target: ItemStack) {
        val sourceMeta = source.itemMeta ?: return
        val targetMeta = target.itemMeta ?: return

        copyDataContainer(
            sourceMeta.persistentDataContainer,
            targetMeta.persistentDataContainer
        )

        target.itemMeta = targetMeta
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

    /**
     * Remove todos os dados de evolução de um item
     */
    fun clearEvolutionData(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        for (evolutionKey in EvolutionKey.entries) {
            container.remove(evolutionKey.key(plugin))
        }

        item.itemMeta = meta
    }

    /**
     * Verifica se o item possui dados de evolução
     */
    fun hasEvolutionData(item: ItemStack): Boolean {
        return isSoulTool(item)
    }
}