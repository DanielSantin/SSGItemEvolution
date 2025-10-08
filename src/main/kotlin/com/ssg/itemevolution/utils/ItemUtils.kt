package com.ssg.itemevolution.utils

import com.ssg.itemevolution.services.*
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Classe facade que mantém compatibilidade com código legado.
 * Delega operações para os serviços especializados.
 *
 * Esta classe deve ser usada como ponto de entrada único para
 * operações relacionadas a itens de evolução.
 */
class ItemUtils(
    private val itemDataService: ItemDataService,
    private val itemEvolutionService: ItemEvolutionService,
    private val itemRepairService: ItemRepairService,
    private val materialUpgradeService: MaterialUpgradeService
) {
    // ===== MÉTODOS PÚBLICOS - COMPATIBILIDADE LEGADA =====

    /**
     * Retorna o nome da categoria da ferramenta (sword, pickaxe, etc.)
     */
    fun testTool(item: ItemStack?): String {
        return itemEvolutionService.getToolCategoryName(item)
    }

    /**
     * Verifica se o item é uma ferramenta/armadura válida
     */
    fun isValidTool(item: ItemStack?): Boolean {
        return itemEvolutionService.isValidTool(item)
    }

    /**
     * Configura um item novo com dados de evolução
     */
    fun setupItem(item: ItemStack): ItemStack {
        return itemEvolutionService.setupItem(item)
    }

    /**
     * Atualiza o item após uso (incrementa contador e verifica level up)
     */
    fun upgradeItem(item: ItemStack): ItemStack {
        return itemEvolutionService.upgradeItem(item)
    }

    /**
     * Melhora o item para o próximo material (ferro -> diamante, etc.)
     */
    fun improveItem(item: ItemStack): ItemStack {
        return itemEvolutionService.improveItem(item)
    }

    // ===== MÉTODOS DE ACESSO A DADOS =====

    /**
     * Obtém o nível atual do item
     */
    fun getItemLevel(item: ItemStack): Int {
        return itemEvolutionService.calculateLevelFromItem(item)
    }

    /**
     * Define o nível do item
     */
    fun setItemLevel(item: ItemStack, level: Int) {
        itemEvolutionService.setLevel(item, level)
    }

    /**
     * Obtém os pontos atuais do item
     */
    fun getItemPoints(item: ItemStack): Int {
        return itemDataService.getPoints(item)
    }

    /**
     * Adiciona pontos ao item
     */
    fun addItemPoints(item: ItemStack, points: Int) {
        itemDataService.addPoints(item, points)
    }
    /**
     * Obtém o número de usos do item
     */
    fun getItemUses(item: ItemStack): Int {
        return itemDataService.getUses(item)
    }

    /**
     * Obtém o contador interno de evolução
     */
    fun getItemCounter(item: ItemStack): Int {
        return itemDataService.getCounter(item)
    }

    // ===== MÉTODOS DE REPARO =====

    /**
     * Calcula o custo de reparo em materiais
     */
    fun getRepairCost(item: ItemStack): Int {
        return itemRepairService.getRepairCost(item)
    }

    /**
     * Obtém o material necessário para reparar
     */
    fun getRepairMaterial(item: ItemStack): Material? {
        return itemRepairService.getRepairMaterial(item)
    }

    /**
     * Retorna o ItemStack completo necessário para upgrade
     */
    fun getUpgradeItemstack(item: ItemStack): ItemStack? {
        return materialUpgradeService.getUpgradeItemStack(item)
    }


}