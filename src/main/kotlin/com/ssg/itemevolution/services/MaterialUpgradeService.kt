package com.ssg.itemevolution.services

import com.ssg.itemevolution.keys.ToolCategory
import com.ssg.itemevolution.keys.ToolType
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Serviço responsável por upgrade de materiais de itens.
 *
 * Gerencia a cadeia de evolução de materiais:
 * Madeira → Pedra → Ferro → Diamante → Netherite
 * Couro → Ferro (armadura)
 * Cota de Malha → Ferro
 */
class MaterialUpgradeService(

) {

    /**
     * Mapa que define a cadeia de upgrades de materiais.
     * Cada ToolType aponta para o próximo na hierarquia.
     */
    private val UPGRADE_PATH = mapOf(
        ToolType.WOOD to ToolType.STONE,
        ToolType.STONE to ToolType.IRON,
        ToolType.IRON to ToolType.DIAMOND,
        ToolType.DIAMOND to ToolType.NETHERITE,
        ToolType.LEATHER to ToolType.IRON,      // Couro pula direto para ferro
        ToolType.CHAINMAIL to ToolType.IRON     // Cota de malha vira ferro
    )

    /**
     * Obtém o próximo material na cadeia de upgrade.
     *
     * Exemplo:
     * - IRON_SWORD → DIAMOND_SWORD
     * - IRON_HELMET → DIAMOND_HELMET
     * - DIAMOND_PICKAXE → NETHERITE_PICKAXE
     *
     * @param current Material atual do item
     * @return O próximo material na cadeia, ou null se não houver upgrade disponível
     */
    fun getNextMaterial(current: Material): Material? {
        // Identifica a categoria (sword, pickaxe, helmet, etc.)
        val category = ToolCategory.fromMaterial(current)
        if (category == ToolCategory.UNKNOWN) return null

        // Identifica o tipo de material atual (wood, iron, diamond, etc.)
        val currentType = ToolType.fromMaterial(current) ?: return null

        // Busca o próximo tipo na cadeia
        val nextType = UPGRADE_PATH[currentType] ?: return null

        // Retorna o material correspondente à mesma categoria mas com o próximo tipo
        // Ex: IRON_SWORD (categoria: SWORD, tipo: IRON) → DIAMOND_SWORD (categoria: SWORD, tipo: DIAMOND)
        return nextType.materials.find {
            ToolCategory.fromMaterial(it) == category
        }
    }

    /**
     * Obtém o material (ingrediente) necessário para fazer o upgrade.
     *
     * Exemplo:
     * - Para upgradar madeira → pedra, precisa de COBBLESTONE
     * - Para upgradar ferro → diamante, precisa de DIAMOND
     * - Para upgradar diamante → netherite, precisa de NETHERITE_INGOT
     *
     * @param item Item que será upgradado
     * @return Material necessário como ingrediente, ou null se não puder ser upgradado
     */
    fun getUpgradeMaterial(item: ItemStack): Material? {
        val toolType = ToolType.fromMaterial(item.type)
        return when (toolType) {
            ToolType.WOOD -> Material.COBBLESTONE
            ToolType.STONE -> Material.IRON_INGOT
            ToolType.IRON -> Material.DIAMOND
            ToolType.DIAMOND -> Material.NETHERITE_INGOT
            ToolType.LEATHER -> Material.IRON_NUGGET
            ToolType.CHAINMAIL -> Material.IRON_INGOT
            else -> null
        }
    }

    /**
     * Obtém a quantidade de material necessária baseado na categoria do item.
     *
     * A quantidade varia conforme o tipo de item:
     * - Espadas, arcos, escudos, bestas: 2 unidades
     * - Picaretas, machados: 3 unidades
     * - Pás, enxadas: 1 unidade
     * - Capacetes: 5 unidades
     * - Botas: 4 unidades
     * - Calças: 7 unidades
     * - Peitoral: 8 unidades
     *
     * Isso reflete a "complexidade" ou quantidade de material que seria
     * necessária para craftar cada item na receita vanilla.
     *
     * @param item Item que será upgradado
     * @return Quantidade de material necessária
     */
    fun getUpgradeMaterialQuantity(item: ItemStack): Int {
        val category = ToolCategory.fromMaterial(item.type)
        return when (category) {
            ToolCategory.SWORD -> 2
            ToolCategory.PICKAXE -> 3
            ToolCategory.AXE -> 3
            ToolCategory.SHOVEL -> 1
            ToolCategory.HOE -> 1
            ToolCategory.HELMET -> 5
            ToolCategory.CHESTPLATE -> 8
            ToolCategory.LEGGINGS -> 7
            ToolCategory.BOOTS -> 4
            ToolCategory.BOW -> 2
            ToolCategory.SHIELD -> 2
            ToolCategory.CROSSBOW -> 2
            else -> 1
        }
    }

    /**
     * Retorna o ItemStack completo (material + quantidade) necessário para upgrade.
     *
     * Útil para exibir em GUIs ou verificar se o jogador tem os materiais.
     *
     * Caso especial: Netherite sempre precisa apenas de 1 ingot (receita do smithing table)
     *
     * @param item Item que será upgradado
     * @return ItemStack com o material e quantidade necessários, ou null se não puder upgradar
     */
    fun getUpgradeItemStack(item: ItemStack): ItemStack? {
        val material = getUpgradeMaterial(item) ?: return null

        // Netherite sempre precisa apenas de 1 ingot (receita vanilla do smithing table)
        val quantity = if (material == Material.NETHERITE_INGOT) {
            1
        } else {
            getUpgradeMaterialQuantity(item)
        }

        return ItemStack(material, quantity)
    }

    /**
     * Verifica se o item pode ser melhorado para um material superior.
     *
     * @param item Item a ser verificado
     * @return true se existe um próximo material na cadeia, false caso contrário
     */
    fun canBeUpgraded(item: ItemStack): Boolean {
        return getNextMaterial(item.type) != null
    }
}