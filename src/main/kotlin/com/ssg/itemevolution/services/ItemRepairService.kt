package com.ssg.itemevolution.services

import com.ssg.itemevolution.enchantments.utility.EternaEnchantment.Companion.isItemBroken
import com.ssg.itemevolution.enchantments.utility.EternaEnchantment.Companion.removeBrokenItemMark
import com.ssg.itemevolution.keys.ToolType
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import kotlin.math.ceil

/**
 * Serviço responsável pelo sistema de reparo de itens.
 *
 * Gerencia cálculos de custo, materiais necessários e execução de reparos.
 */
class ItemRepairService {
    private val REPAIR_COST_MULTIPLIER: Double = 3.0

    /**
     * Multiplicador usado no cálculo do custo de reparo.
     *
     * Fórmula: ceil(MULTIPLICADOR * dano / durabilidade_máxima)
     *
     * Com multiplicador 3.0:
     * - Item com 33% de dano → 1 material
     * - Item com 50% de dano → 2 materiais
     * - Item com 100% de dano → 3 materiais
     */

    /**
     * Calcula o custo de reparo baseado no dano atual do item.
     *
     * O custo aumenta proporcionalmente ao dano:
     * - Mais dano = mais materiais necessários
     * - Fórmula: ceil(3.0 * dano / durabilidade_máxima)
     *
     * Exemplos (espada de diamante - 1561 durabilidade):
     * - 520 de dano (33%) → 1 material
     * - 780 de dano (50%) → 2 materiais
     * - 1561 de dano (100%) → 3 materiais
     *
     * @param item Item a ser reparado
     * @return Número de materiais necessários para reparo completo
     */
    fun getRepairCost(item: ItemStack): Int {
        val meta = item.itemMeta as? Damageable ?: return 0
        val damage = meta.damage
        val maxDurability = item.type.maxDurability.toInt()

        if (maxDurability == 0) return 0

        return ceil(REPAIR_COST_MULTIPLIER * damage / maxDurability).toInt()
    }

    /**
     * Calcula o custo de reparo com multiplicador aplicado.
     *
     * Útil para penalidades (ex: item quebrado custa 2x mais para reparar).
     *
     * @param item Item a ser reparado
     * @param multiplier Multiplicador do custo (default: 1)
     * @return Número de materiais necessários
     */
    fun getRepairCostWithMultiplier(item: ItemStack, multiplier: Int = 1): Int {
        return getRepairCost(item) * multiplier
    }

    /**
     * Cria um ItemStack do material de reparo com a quantidade calculada.
     *
     * Considera automaticamente se o item está quebrado (2x custo).
     *
     * @param item Item a ser reparado
     * @return ItemStack com material e quantidade, ou null se não for reparável
     */
    fun getRepairItemStack(item: ItemStack): ItemStack? {
        val material = getRepairMaterial(item) ?: return null
        val isBroken = isItemBroken(item)
        val multiplier = if (isBroken) 2 else 1
        val cost = getRepairCostWithMultiplier(item, multiplier)

        return ItemStack(material, cost)
    }

    /**
     * Cria uma cópia do item completamente reparado.
     *
     * Remove o dano e marca de item quebrado, se existir.
     *
     * @param item Item original
     * @return Nova instância do item reparado
     */
    fun createRepairedItem(item: ItemStack): ItemStack {
        val repaired = item.clone()
        removeBrokenItemMark(repaired)

        val meta = repaired.itemMeta as? Damageable
        if (meta != null) {
            meta.damage = 0
            repaired.itemMeta = meta
        }

        return repaired
    }

    /**
     * Verifica se o item pode ser reparado.
     *
     * Critérios:
     * - Deve ter durabilidade
     * - Deve estar danificado
     * - Deve ter material de reparo disponível
     *
     * @param item Item a verificar
     * @return true se pode ser reparado
     */
    fun canRepair(item: ItemStack): Boolean {
        return needsRepair(item) && getRepairMaterial(item) != null
    }

    /**
     * Obtém o material necessário para reparar o item.
     *
     * Cada tipo de ferramenta/armadura usa um material específico para reparo:
     * - Madeira → Tábuas de carvalho
     * - Pedra → Pedregulho
     * - Ferro → Barra de ferro
     * - Diamante → Diamante
     * - Netherite → Fragmento de netherite
     * - Couro → Couro
     * - Cota de Malha → Pepita de ferro
     * - Ouro → Barra de ouro
     * - Arco/Besta → Graveto
     * - Escudo → Tábuas de carvalho
     *
     * @param item Item a ser reparado
     * @return Material necessário para reparo, ou null se não for reparável
     */
    fun getRepairMaterial(item: ItemStack): Material? {
        val toolType = ToolType.fromMaterial(item.type)
        return when (toolType) {
            ToolType.WOOD -> Material.OAK_PLANKS
            ToolType.STONE -> Material.COBBLESTONE
            ToolType.IRON -> Material.IRON_INGOT
            ToolType.DIAMOND -> Material.DIAMOND
            ToolType.NETHERITE -> Material.NETHERITE_SCRAP
            ToolType.LEATHER -> Material.LEATHER
            ToolType.CHAINMAIL -> Material.IRON_NUGGET
            ToolType.GOLD -> Material.GOLD_INGOT
            null -> when (item.type) {
                Material.BOW -> Material.STICK
                Material.SHIELD -> Material.OAK_PLANKS
                Material.CROSSBOW -> Material.STICK
                else -> null
            }
        }
    }

    /**
     * Verifica se o item está danificado e precisa de reparo.
     *
     * @param item Item a ser verificado
     * @return true se o item tem algum dano, false caso contrário
     */
    fun needsRepair(item: ItemStack): Boolean {
        val meta = item.itemMeta as? Damageable ?: return false
        return meta.damage > 0
    }

    fun applyDurabilityDamage(tool: ItemStack) {
        val meta = tool.itemMeta
        if (meta.isUnbreakable || tool.type.maxDurability <= 0) return
        val damageable = meta as? Damageable ?: return

        val unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING)
        val chanceToLoseDurability = 100.0 / (unbreakingLevel + 1)
        val random = kotlin.random.Random.nextDouble(0.0, 100.0)

        if (random >= chanceToLoseDurability) return
        damageable.damage += 1
        tool.itemMeta = damageable
    }
}