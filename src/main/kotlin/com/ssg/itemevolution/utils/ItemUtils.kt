package com.ssg.itemevolution.utils

import com.ssg.itemevolution.ItemEvolutionPlugin
import com.ssg.itemevolution.services.*
import com.ssg.itemevolution.ui.ItemDescriptionFormatter
import net.kyori.adventure.text.Component
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
    plugin: ItemEvolutionPlugin,
    configManager: ConfigManager
) {
    // Serviços especializados
    private val itemDataService = ItemDataService(plugin)
    private val evolutionService = ItemEvolutionService(plugin, configManager, itemDataService)
    private val descriptionFormatter = ItemDescriptionFormatter(itemDataService, evolutionService)

    // ===== MÉTODOS PÚBLICOS - COMPATIBILIDADE LEGADA =====

    /**
     * Retorna o nome da categoria da ferramenta (sword, pickaxe, etc.)
     */
    fun testTool(item: ItemStack?): String {
        return evolutionService.getToolCategoryName(item)
    }

    /**
     * Verifica se o item é uma ferramenta/armadura válida
     */
    fun isValidTool(item: ItemStack?): Boolean {
        return evolutionService.isValidTool(item)
    }

    /**
     * Configura um item novo com dados de evolução
     */
    fun setupItem(item: ItemStack): ItemStack {
        return evolutionService.setupItem(item)
    }

    /**
     * Atualiza o item após uso (incrementa contador e verifica level up)
     */
    fun upgradeItem(item: ItemStack): ItemStack {
        return evolutionService.upgradeItem(item)
    }

    /**
     * Melhora o item para o próximo material (ferro -> diamante, etc.)
     */
    fun improveItem(item: ItemStack): ItemStack {
        return evolutionService.improveItem(item)
    }

    // ===== MÉTODOS DE ACESSO A DADOS =====

    /**
     * Obtém o nível atual do item
     */
    fun getItemLevel(item: ItemStack): Int {
        return itemDataService.getLevel(item)
    }

    /**
     * Define o nível do item
     */
    fun setItemLevel(item: ItemStack, level: Int) {
        itemDataService.setLevel(item, level)
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
     * Define os pontos do item
     */
    fun setItemPoints(item: ItemStack, points: Int) {
        itemDataService.setPoints(item, points)
    }

    /**
     * Obtém o número de usos do item
     */
    fun getItemUses(item: ItemStack): Int {
        return itemDataService.getUses(item)
    }

    /**
     * Incrementa o contador de usos
     */
    fun incrementUses(item: ItemStack) {
        itemDataService.incrementUses(item)
    }

    /**
     * Define o número de usos do item
     */
    fun setItemUses(item: ItemStack, uses: Int) {
        itemDataService.setUses(item, uses)
    }

    /**
     * Obtém o contador interno de evolução
     */
    fun getItemCounter(item: ItemStack): Int {
        return itemDataService.getCounter(item)
    }

    /**
     * Define o contador interno de evolução
     */
    fun setItemCounter(item: ItemStack, counter: Int) {
        itemDataService.setCounter(item, counter)
    }

    /**
     * Calcula o progresso até o próximo nível (0.0 a 1.0)
     */
    fun getLevelProgress(item: ItemStack): Double {
        return evolutionService.getLevelProgress(item)
    }

    // ===== MÉTODOS DE UI/FORMATAÇÃO =====

    /**
     * Retorna a descrição completa do item para exibição
     */
    fun getDescription(item: ItemStack): List<Component> {
        return descriptionFormatter.getDescription(item)
    }

    /**
     * Retorna uma descrição resumida do item
     */
    fun getShortDescription(item: ItemStack): List<Component> {
        return descriptionFormatter.getShortDescription(item)
    }

    /**
     * Gera barra de progresso visual
     */
    fun getProgressBar(item: ItemStack, length: Int = 20): String {
        val progress = evolutionService.getLevelProgress(item)
        return descriptionFormatter.getProgressBar(progress, length)
    }

    // ===== MÉTODOS DE REPARO =====

    /**
     * Calcula o custo de reparo em materiais
     */
    fun getRepairCost(item: ItemStack): Int {
        return ItemRepairService.getRepairCost(item)
    }

    /**
     * Obtém o material necessário para reparar
     */
    fun getRepairMaterial(item: ItemStack): Material? {
        return ItemRepairService.getRepairMaterial(item)
    }

    /**
     * Repara o item completamente
     */
    fun repairItem(item: ItemStack) {
        ItemRepairService.repairItem(item)
    }

    /**
     * Repara o item parcialmente
     */
    fun repairItem(item: ItemStack, amount: Int) {
        ItemRepairService.repairItem(item, amount)
    }

    /**
     * Verifica se o item precisa de reparo
     */
    fun needsRepair(item: ItemStack): Boolean {
        return ItemRepairService.needsRepair(item)
    }

    /**
     * Obtém a porcentagem de durabilidade restante
     */
    fun getDurabilityPercentage(item: ItemStack): Double {
        return ItemRepairService.getDurabilityPercentage(item)
    }

    // ===== MÉTODOS DE UPGRADE DE MATERIAL =====

    /**
     * Obtém o material necessário para upgrade
     */
    fun getUpgradeMaterial(item: ItemStack): Material? {
        return MaterialUpgradeService.getUpgradeMaterial(item)
    }

    /**
     * Obtém a quantidade de material necessária para upgrade
     */
    fun getMaterialQuantity(item: ItemStack): Int {
        return MaterialUpgradeService.getUpgradeMaterialQuantity(item)
    }

    /**
     * Retorna o ItemStack completo necessário para upgrade
     */
    fun getUpgradeItemstack(item: ItemStack): ItemStack? {
        return MaterialUpgradeService.getUpgradeItemStack(item)
    }

    /**
     * Verifica se o item pode ser melhorado
     */
    fun canBeUpgraded(item: ItemStack): Boolean {
        return MaterialUpgradeService.canBeUpgraded(item)
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Verifica se o item possui dados de evolução
     */
    fun hasEvolutionData(item: ItemStack): Boolean {
        return itemDataService.hasEvolutionData(item)
    }

    /**
     * Remove todos os dados de evolução do item
     */
    fun clearEvolutionData(item: ItemStack) {
        itemDataService.clearEvolutionData(item)
    }

    /**
     * Copia dados de evolução de um item para outro
     */
    fun copyEvolutionData(source: ItemStack, target: ItemStack) {
        itemDataService.copyEvolutionData(source, target)
    }
}