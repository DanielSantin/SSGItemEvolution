package com.ssg.itemevolution.services

import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.keys.EvolutionKey
import com.ssg.itemevolution.utils.ItemUtils
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class SoulToolService(
    private val plugin: ItemEvolutionPlugin,
    private val itemUtils: ItemUtils
) {
    enum class SoulToolCheckResult(val message: Component?) {
        VALID(null),
        ALREADY_SOUL(Component.text("§cEsta ferramenta já possui uma alma!")),
        INVALID_TOOL(Component.text("§cEste item não pode ser transformado.")),
        HAS_ENCHANT(Component.text("§cRemova os encantamentos antes de transformar."))
    }

    fun hasSoul(tool: ItemStack): Boolean {
        val meta = tool.itemMeta ?: return false
        val value = meta.persistentDataContainer.get(
            EvolutionKey.SOUL_TOOL.key(plugin),
            PersistentDataType.INTEGER
        ) ?: 0
        return value == 1
    }

    fun checkSoulEligibility(tool: ItemStack?): SoulToolCheckResult {
        if (tool == null) return SoulToolCheckResult.INVALID_TOOL
        if (!itemUtils.isValidTool(tool)) return SoulToolCheckResult.INVALID_TOOL
        if (hasSoul(tool)) return SoulToolCheckResult.ALREADY_SOUL
        if (tool.enchantments.isNotEmpty()) return SoulToolCheckResult.HAS_ENCHANT
        return SoulToolCheckResult.VALID
    }

    fun transformToSoulTool(tool: ItemStack): Boolean {
        return try {
            if (checkSoulEligibility(tool) != SoulToolCheckResult.VALID) return false
            val toolCopy = tool.clone()
            addSoulToTool(toolCopy)
            itemUtils.setupItem(toolCopy)
            tool.itemMeta = toolCopy.itemMeta
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun addSoulToTool(tool: ItemStack) {
        val meta = tool.itemMeta ?: throw IllegalStateException("ItemMeta não pode ser null")
        val container = meta.persistentDataContainer
        container.set(
            EvolutionKey.SOUL_TOOL.key(plugin),
            PersistentDataType.BOOLEAN,
            true
        )
        tool.itemMeta = meta
    }
}