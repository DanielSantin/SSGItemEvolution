package com.ssg.itemevolution.services

import com.ssg.itemevolution.ItemEvolutionPlugin
import org.bukkit.inventory.ItemStack

/**
 * Serviço responsável por transferir metadados entre itens.
 *
 * Usado principalmente quando um item é melhorado para um material superior
 * (ex: espada de ferro → espada de diamante) e precisa manter suas
 * propriedades personalizadas.
 */
class ItemMetaTransferService() {
    fun transferMetadata(
        source: ItemStack,
        target: ItemStack,
        dataService: ItemDataService
    ) {
        val sourceMeta = source.itemMeta ?: return
        val targetMeta = target.itemMeta ?: return

        dataService.copyDataContainer(
            sourceMeta.persistentDataContainer,
            targetMeta.persistentDataContainer
        )

        targetMeta.displayName(sourceMeta.displayName())

        if (sourceMeta.hasCustomModelDataComponent()) {
            targetMeta.setCustomModelDataComponent(sourceMeta.customModelDataComponent)
        }

        targetMeta.isUnbreakable = sourceMeta.isUnbreakable
        targetMeta.addItemFlags(*sourceMeta.itemFlags.toTypedArray())

        sourceMeta.attributeModifiers?.let {
            targetMeta.attributeModifiers = it
        }

        target.itemMeta = targetMeta
    }
}