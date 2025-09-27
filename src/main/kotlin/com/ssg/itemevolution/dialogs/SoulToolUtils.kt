package com.ssg.itemevolution.dialogs

import com.ssg.itemevolution.EvolutionKey
import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.ItemUtils
import com.ssg.itemevolution.ItemUtils.setupItem
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object SoulToolUtils {
    private val plugin = ItemEvolutionPlugin.instance

    enum class SoulToolCheckResult(val message: Component?) {
        VALID(null),
        ALREADY_SOUL(Component.text("§cEsta ferramenta já possui uma alma!")),
        INVALID_TOOL(Component.text("§cEste item não pode ser transformado.")),
        HAS_ENCHANT(Component.text("§cRemova os encantamentos antes de transformar."))
    }

    // === Verificações e Validações ===

    /**
     * Verifica se uma ferramenta tem alma
     */
    fun hasSoul(tool: ItemStack): Boolean {
        val meta = tool.itemMeta ?: return false
        return meta.persistentDataContainer.get(
            EvolutionKey.SOUL_TOOL.key(plugin),
            PersistentDataType.BOOLEAN
        ) ?: false
    }

    /**
     * Verifica se uma ferramenta é elegível para receber alma
     */
    fun checkSoulEligibility(tool: ItemStack?): SoulToolCheckResult {
        if (tool == null) return SoulToolCheckResult.INVALID_TOOL
        if (!ItemUtils.isValidTool(tool)) return SoulToolCheckResult.INVALID_TOOL
        if (hasSoul(tool)) return SoulToolCheckResult.ALREADY_SOUL

        // Verifica se tem qualquer encantamento (vanilla ou data-driven)
        if (tool.enchantments.isNotEmpty()) return SoulToolCheckResult.HAS_ENCHANT

        return SoulToolCheckResult.VALID
    }

    // === Transformação da Ferramenta ===

    /**
     * Transforma uma ferramenta em ferramenta com alma
     */
    fun transformToSoulTool(tool: ItemStack): Boolean {
        return try {
            // Verifica elegibilidade uma última vez
            if (checkSoulEligibility(tool) != SoulToolCheckResult.VALID) {
                return false
            }

            val toolCopy = tool.clone()

            // Aplica as transformações
            addSoulToTool(toolCopy)
            setupItem(toolCopy)

            // Copia os dados de volta para o item original
            tool.itemMeta = toolCopy.itemMeta
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Adiciona a marca de alma à ferramenta
     */
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