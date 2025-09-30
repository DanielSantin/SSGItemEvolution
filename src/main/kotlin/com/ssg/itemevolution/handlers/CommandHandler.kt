package com.ssg.itemevolution.handlers

import com.ssg.itemevolution.services.ItemDataService
import com.ssg.itemevolution.services.ItemEvolutionService
import com.ssg.itemevolution.services.SoulToolService
import com.ssg.itemevolution.utils.ConfigManager
import com.ssg.itemevolution.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Handler centralizado para todos os comandos do plugin
 */
class CommandHandler(
    private val configManager: ConfigManager,
    private val itemUtils: ItemUtils,
    private val soulToolService: SoulToolService,
    private val itemDataService: ItemDataService,
    private val itemEvolutionService: ItemEvolutionService
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "info" -> handleInfo(sender, args)
            "addsoul" -> handleAddSoul(sender)
            "removesoul" -> handleRemoveSoul(sender)
            "setlevel" -> handleSetLevel(sender, args)
            "addpoints" -> handleAddPoints(sender, args)
            "debug" -> handleDebug(sender, args)
            "help" -> sendHelp(sender)
            "migrate" -> handleMigrate(sender)
            "setcounter" -> handleSetCounter(sender, args)
            else -> {
                sender.sendMessage("§cComando inválido. Use §6/ssgitemevolution help §cpara ver os comandos disponíveis.")
                return true
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {

        val completions = mutableListOf<String>()
        val config = configManager.getCustomConfig("enchantments.yml")
        val keys = config?.getConfigurationSection("enchantments")?.getKeys(false) ?: emptySet()

        when (args.size) {
            1 -> {
                val subcommands = listOf("reload", "info", "addsoul", "removesoul", "setlevel", "addpoints", "enchant", "debug", "help", "migrate", "setcounter", "setabsolutecounter")
                completions.addAll(subcommands.filter { it.startsWith(args[0].lowercase()) })
            }
            2 -> {
                when (args[0].lowercase()) {
                    "enchant" -> {
                        completions.addAll(keys.filter { it.startsWith(args[1].lowercase()) })
                    }
                    "debug" -> {
                        completions.addAll(listOf("item", "enchants", "config").filter {
                            it.startsWith(args[1].lowercase())
                        })
                    }
                    "migrate" -> {
                        completions.addAll(listOf("armor").filter { it.startsWith(args[1].lowercase()) })
                    }
                }
            }
            3 -> {
                when (args[0].lowercase()) {
                    "enchant" -> {
                        // Níveis de 1 a 5
                        completions.addAll((1..5).map { it.toString() }.filter {
                            it.startsWith(args[2])
                        })
                    }
                }
            }
        }

        return completions
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        try {
            configManager.reloadConfigurations()
            sender.sendMessage("§a[SSG] Configurações recarregadas com sucesso!")
        } catch (e: Exception) {
            sender.sendMessage("§c[SSG] Erro ao recarregar configurações: ${e.message}")
        }
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            sender.sendMessage("§cVocê precisa estar segurando um item na mão.")
            return
        }

        if (!itemUtils.isValidTool(item)) {
            sender.sendMessage("§cEste item não é uma ferramenta válida.")
            return
        }

        val hasSoul = soulToolService.hasSoul(item)
        val level = itemUtils.getItemLevel(item)
        val points = itemUtils.getItemPoints(item)
        val uses = itemUtils.getItemUses(item)
        val counterForActualLevel = itemEvolutionService.calculateCounterForLevel(level, item)
        val actualCounter = itemUtils.getItemCounter(item) - counterForActualLevel
        val neededCounter = itemEvolutionService.calculateCounterForLevel(level + 1, item) - counterForActualLevel

        sender.sendMessage("§6§l=== INFORMAÇÕES DO ITEM ===")
        sender.sendMessage("§7Tipo: §f${item.type.name}")
        sender.sendMessage("§7Possui Alma: ${if (hasSoul) "§a✓" else "§c✗"}")

        if (hasSoul) {
            sender.sendMessage("§7Nível: §e$level")
            sender.sendMessage("§7Pontos: §b$points")
            sender.sendMessage("§7Usos: §d$uses")
            sender.sendMessage("§7Contador: §d$actualCounter / $neededCounter")
        }
    }

    private fun handleAddSoul(sender: CommandSender) {
        if (!sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            sender.sendMessage("§cVocê precisa estar segurando um item na mão.")
            return
        }

        if (!itemUtils.isValidTool(item)) {
            sender.sendMessage("§cEste item não é uma ferramenta válida.")
            return
        }

        if (soulToolService.hasSoul(item)) {
            sender.sendMessage("§cEste item já possui uma alma.")
            return
        }

        val soulItem = itemUtils.setupItem(item)
        sender.inventory.setItemInMainHand(soulItem)
        sender.sendMessage("§a[SSG] Alma adicionada ao item com sucesso!")
    }

    private fun handleRemoveSoul(sender: CommandSender) {
        if (!sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            sender.sendMessage("§cVocê precisa estar segurando um item na mão.")
            return
        }

        if (!soulToolService.hasSoul(item)) {
            sender.sendMessage("§cEste item não possui uma alma.")
            return
        }

        // Remover dados da alma
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer

        val keysToRemove = listOf("soul_tool", "fplus_contador", "fplus_level", "fplus_pontos", "fplus_usos")
        keysToRemove.forEach { keyId ->
            val key = NamespacedKey(Bukkit.getPluginManager().getPlugin("ItemEvolution")!!, keyId)
            container.remove(key)
        }

        item.itemMeta = meta
        sender.sendMessage("§a[SSG] Alma removida do item com sucesso!")
    }

    private fun handleSetLevel(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUso: /ssgitemevolution setlevel <nível>")
            return
        }

        val level = args[1].toIntOrNull()
        if (level == null || level < 1) {
            sender.sendMessage("§cNível deve ser um número maior que 0.")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (!soulToolService.hasSoul(item)) {
            sender.sendMessage("§cEste item não possui uma alma.")
            return
        }

        itemUtils.setItemLevel(item, level)
        sender.sendMessage("§a[SSG] Nível do item definido para $level!")
    }

    private fun handleAddPoints(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUso: /ssgitemevolution addpoints <pontos>")
            return
        }

        val points = args[1].toIntOrNull()
        if (points == null || points < 1) {
            sender.sendMessage("§cPontos deve ser um número maior que 0.")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (!soulToolService.hasSoul(item)) {
            sender.sendMessage("§cEste item não possui uma alma.")
            return
        }

        itemUtils.addItemPoints(item, points)
        sender.sendMessage("§a[SSG] $points pontos adicionados ao item!")
    }


    private fun handleDebug(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("ssgitemevolution.debug")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUso: /ssgitemevolution debug <tipo>")
            sender.sendMessage("§7Tipos: item, enchants, config")
            return
        }

        when (args[1].lowercase()) {
            "item" -> debugItem(sender)
            "config" -> debugConfig(sender)
            else -> sender.sendMessage("§cTipo de debug inválido.")
        }
    }

    private fun debugItem(sender: CommandSender) {
        if (sender !is Player) return

        val item = sender.inventory.itemInMainHand
        sender.sendMessage("§6=== DEBUG ITEM ===")
        sender.sendMessage("§7Material: ${item.type}")
        sender.sendMessage("§7Quantidade: ${item.amount}")
        sender.sendMessage("§7Meta presente: ${item.hasItemMeta()}")

        if (item.hasItemMeta()) {
            val meta = item.itemMeta!!
            sender.sendMessage("§7PersistentData keys: ${meta.persistentDataContainer.keys.size}")
            meta.persistentDataContainer.keys.forEach { key ->
                sender.sendMessage("  §8${key.namespace}:${key.key}")
            }
        }
    }


    private fun debugConfig(sender: CommandSender) {
        sender.sendMessage("§6=== DEBUG CONFIGURAÇÃO ===")
        val settings = configManager.getEvolutionSettings()
        sender.sendMessage("§7Base Multiplier: ${settings.baseMultiplier}")
        sender.sendMessage("§7Level Exponent: ${settings.levelExponent}")
        sender.sendMessage("§7Armor Multiplier: ${settings.armorMultiplier}")
        sender.sendMessage("§7Max Level: ${settings.maxLevel}")
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== COMANDOS ITEMEVOLUTION ===")
        sender.sendMessage("§e/ssgitemevolution info §7- Mostra informações do item na mão")

        if (sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§e/ssgitemevolution reload §7- Recarrega as configurações")
            sender.sendMessage("§e/ssgitemevolution addsoul §7- Adiciona alma ao item")
            sender.sendMessage("§e/ssgitemevolution removesoul §7- Remove alma do item")
            sender.sendMessage("§e/ssgitemevolution setlevel <nível> §7- Define nível do item")
            sender.sendMessage("§e/ssgitemevolution addpoints <pontos> §7- Adiciona pontos ao item")
            sender.sendMessage("§e/ssgitemevolution enchant <encanto> <nível> §7- Aplica encantamento")

        }

        if (sender.hasPermission("ssgitemevolution.debug")) {
            sender.sendMessage("§e/ssgitemevolution debug <tipo> §7- Informações de debug")
        }

        sender.sendMessage("§e/ssgitemevolution help §7- Mostra esta ajuda")
    }

    // Função temporária que faz a atualização de um item
    fun updateTool(item: ItemStack) {
        val meta = item.itemMeta ?: return
        meta.lore(null)
        item.itemMeta = meta
        itemDataService.setSoulTool(item, true)
    }

    private fun handleMigrate(sender: CommandSender, args: Array<out String> = emptyArray()) {
        if (!sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return
        }

        if (args.isNotEmpty() && args[1].lowercase() == "armor") {
            val armorContents = sender.inventory.armorContents
            var migratedCount = 0

            armorContents.forEachIndexed { index, item ->
                if (item == null || item.type == Material.AIR) return@forEachIndexed
                updateTool(item)
                migratedCount++
            }

            sender.sendMessage("§a[SSG] $migratedCount peças de armadura migradas para o novo sistema com sucesso!")
            return
        }

        // Caso padrão: apenas a ferramenta na mão
        val item = sender.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            sender.sendMessage("§cVocê precisa estar segurando um item na mão.")
            return
        }

        updateTool(item)
        sender.sendMessage("§a[SSG] Item migrado para o novo sistema com sucesso!")
    }

    private fun handleSetCounter(sender: CommandSender, args: Array<out String> = emptyArray()) {
        if (!sender.hasPermission("ssgitemevolution.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return
        }

        val counter = args[1].toIntOrNull()
        if (counter == null || counter < 0) {
            sender.sendMessage("§cVocê precisa passar um valor inteiro positivo para o contador.")
            return
        }
        val item = sender.inventory.itemInMainHand
        itemEvolutionService.setCounterBasedOnLevel(item, counter)

    }



}