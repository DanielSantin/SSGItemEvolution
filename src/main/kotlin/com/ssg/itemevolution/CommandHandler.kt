package com.ssg.itemevolution

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.inventory.meta.Damageable


class CommandHandler(private val plugin: ItemEvolutionPlugin) : CommandExecutor, TabCompleter {

    private val subCommands = listOf(
        "getnbt", "encantarcustom", "testarencantamentos", "reload",
        "setlevel", "setusos", "setpontos", "getinfo", "resetitem", "openmenu"
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§eUso: /ssgitemevolution <comando>")
            sender.sendMessage("§7Comandos disponíveis:")
            sender.sendMessage("§7- getnbt: Ver dados NBT do item")
            sender.sendMessage("§7- encantarcustom <nome> <nível>: Aplicar encantamento")
            sender.sendMessage("§7- testarencantamentos: Ver encantamentos customizados")
            sender.sendMessage("§7- setlevel <nível>: Definir nível da ferramenta")
            sender.sendMessage("§7- setusos <usos>: Definir usos restantes")
            sender.sendMessage("§7- setpontos <pontos>: Definir pontos da ferramenta")
            sender.sendMessage("§7- getinfo: Ver informações completas do item")
            sender.sendMessage("§7- resetitem: Resetar item para estado inicial")
            sender.sendMessage("§7- reload: Recarregar configurações")
            return true
        }

        when (args[0].lowercase()) {
            "getnbt" -> {
                if (sender is Player) handleGetNBT(sender) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "encantarcustom" -> {
                if (sender is Player) handleCustomEnchant(sender, args.drop(1).toTypedArray()) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "testarencantamentos" -> {
                if (sender is Player) handleTestEnchantments(sender) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "setlevel" -> {
                if (sender is Player) handleSetLevel(sender, args.drop(1).toTypedArray()) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "setusos" -> {
                if (sender is Player) handleSetUsos(sender, args.drop(1).toTypedArray()) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "setpontos" -> {
                if (sender is Player) handleSetPontos(sender, args.drop(1).toTypedArray()) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "getinfo" -> {
                if (sender is Player) handleGetInfo(sender) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "resetitem" -> {
                if (sender is Player) handleResetItem(sender) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "openmenu" -> {
                if (sender is Player) handleOpenMenu(sender) else sender.sendMessage("§cApenas jogadores podem usar este comando!")
            }
            "reload" -> handleReload(sender)
            else -> sender.sendMessage("§cSubcomando inválido! Use sem argumentos para ver a lista.")
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        if (args.size == 1) {
            return subCommands.filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
        }
        return mutableListOf()
    }

    private fun handleGetNBT(player: Player) {
        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer

        val nbtData = mutableListOf<String>()
        nbtData.add("§6=== NBT Data do Item ===")
        nbtData.add("§7Material: §e${tool.type.name}")
        nbtData.add("§7Quantidade: §e${tool.amount}")

        for (key in container.keys) {
            var value = "null"

            when {
                container.has(key, PersistentDataType.STRING) -> value = container.get(key, PersistentDataType.STRING) ?: "null"
                container.has(key, PersistentDataType.INTEGER) -> value = container.get(key, PersistentDataType.INTEGER)?.toString() ?: "null"
                container.has(key, PersistentDataType.DOUBLE) -> value = container.get(key, PersistentDataType.DOUBLE)?.toString() ?: "null"
                container.has(key, PersistentDataType.LONG) -> value = container.get(key, PersistentDataType.LONG)?.toString() ?: "null"
                container.has(key, PersistentDataType.BYTE) -> value = container.get(key, PersistentDataType.BYTE)?.toString() ?: "null"
            }

            nbtData.add("§7${key.key}: §e$value")
        }

        if (tool.enchantments.isNotEmpty()) {
            nbtData.add("§6=== Encantamentos Vanilla ===")
            tool.enchantments.forEach { (enchant, level) ->
                nbtData.add("§7${enchant.key.key}: §e$level")
            }
        }

        nbtData.forEach { player.sendMessage(it) }
    }

    private fun handleCustomEnchant(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!")
            return
        }

        if (args.size != 2) {
            player.sendMessage("§cUso: /ssgitemevolution encantarcustom <nome> <nível>")
            return
        }

        val enchantName = args[0]
        val level = args[1].toIntOrNull()

        if (level == null || level <= 0) {
            player.sendMessage("§cO nível deve ser um número positivo!")
            return
        }

        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer

        val customEnchantmentsKey = NamespacedKey(plugin, "custom_enc")
        val customLevelsKey = NamespacedKey(plugin, "custom_enc_lvl")

        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",")?.toMutableList() ?: mutableListOf()
        val levels = container.get(customLevelsKey, PersistentDataType.STRING)?.split(",")?.toMutableList() ?: mutableListOf()

        val index = enchantments.indexOf(enchantName)
        if (index >= 0) {
            levels[index] = level.toString()
        } else {
            enchantments.add(enchantName)
            levels.add(level.toString())
        }

        container.set(customEnchantmentsKey, PersistentDataType.STRING, enchantments.joinToString(","))
        container.set(customLevelsKey, PersistentDataType.STRING, levels.joinToString(","))

        tool.itemMeta = meta
        ItemUtils.updateLore(tool)

        player.sendMessage("§aEncantamento customizado §e$enchantName $level §aaplicado com sucesso!")
    }

    private fun handleTestEnchantments(player: Player) {
        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer

        val customEnchantmentsKey = NamespacedKey(plugin, "custom_enc")
        val customLevelsKey = NamespacedKey(plugin, "custom_enc_lvl")

        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",") ?: emptyList()
        val levels = container.get(customLevelsKey, PersistentDataType.STRING)?.split(",") ?: emptyList()

        if (enchantments.isEmpty()) {
            player.sendMessage("§cEste item não possui encantamentos customizados!")
            return
        }

        player.sendMessage("§6=== Encantamentos Customizados ===")
        for (i in enchantments.indices) {
            if (i < levels.size) {
                player.sendMessage("§7${enchantments[i]}: §e${levels[i]}")
            }
        }
    }

    private fun handleSetLevel(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!")
            return
        }

        if (args.isEmpty()) {
            player.sendMessage("§cUso: /ssgitemevolution setlevel <nível>")
            return
        }

        val level = args[0].toIntOrNull()
        if (level == null || level < 0) {
            player.sendMessage("§cO nível deve ser um número não-negativo!")
            return
        }

        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer
        val levelKey = NamespacedKey(plugin, "fplus_nivel")

        container.set(levelKey, PersistentDataType.INTEGER, level)
        tool.itemMeta = meta
        ItemUtils.updateLore(tool)

        player.sendMessage("§aNível da ferramenta definido para §e$level§a!")
    }

    private fun handleSetUsos(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!")
            return
        }

        if (args.isEmpty()) {
            player.sendMessage("§cUso: /ssgitemevolution setusos <usos>")
            return
        }

        val usos = args[0].toIntOrNull()
        if (usos == null || usos < 0) {
            player.sendMessage("§cOs usos devem ser um número não-negativo!")
            return
        }

        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer
        val usosKey = NamespacedKey(plugin, "fplus_usos")

        container.set(usosKey, PersistentDataType.INTEGER, usos)
        tool.itemMeta = meta
        ItemUtils.updateLore(tool)

        player.sendMessage("§aUsos da ferramenta definidos para §e$usos§a!")
    }

    private fun handleSetPontos(player: Player, args: Array<out String>) {
        if (!player.isOp) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!")
            return
        }

        if (args.isEmpty()) {
            player.sendMessage("§cUso: /ssgitemevolution setpontos <pontos>")
            return
        }

        val pontos = args[0].toIntOrNull()
        if (pontos == null || pontos < 0) {
            player.sendMessage("§cOs pontos devem ser um número não-negativo!")
            return
        }

        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer
        val pontosKey = NamespacedKey(plugin, "fplus_pontos")

        container.set(pontosKey, PersistentDataType.INTEGER, pontos)
        tool.itemMeta = meta
        ItemUtils.updateLore(tool)

        player.sendMessage("§aPontos da ferramenta definidos para §e$pontos§a!")
    }

    private fun handleGetInfo(player: Player) {
        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer

        // Keys importantes
        val levelKey = NamespacedKey(plugin, "fplus_nivel")
        val usosKey = NamespacedKey(plugin, "fplus_usos")
        val pontosKey = NamespacedKey(plugin, "fplus_pontos")
        val customEnchantmentsKey = NamespacedKey(plugin, "custom_enc")
        val customLevelsKey = NamespacedKey(plugin, "custom_enc_lvl")

        // Obter valores
        val level = container.get(levelKey, PersistentDataType.INTEGER) ?: 0
        val usos = container.get(usosKey, PersistentDataType.INTEGER) ?: 0
        val pontos = container.get(pontosKey, PersistentDataType.INTEGER) ?: 0
        val durabilidade = if (meta is Damageable) tool.type.maxDurability - meta.damage else 0

        player.sendMessage("§6=== Informações da Ferramenta ===")
        player.sendMessage("§7Material: §e${tool.type.name}")
        player.sendMessage("§7Nível: §e$level")
        player.sendMessage("§7Usos: §e$usos")
        player.sendMessage("§7Pontos: §e$pontos")
        player.sendMessage("§7Durabilidade: §e$durabilidade§7/§e${tool.type.maxDurability}")

        // Mostrar encantamentos customizados
        val enchantments = container.get(customEnchantmentsKey, PersistentDataType.STRING)?.split(",") ?: emptyList()
        val levels = container.get(customLevelsKey, PersistentDataType.STRING)?.split(",") ?: emptyList()

        if (enchantments.isNotEmpty()) {
            player.sendMessage("§6=== Encantamentos Customizados ===")
            for (i in enchantments.indices) {
                if (i < levels.size) {
                    player.sendMessage("§7${enchantments[i]}: §e${levels[i]}")
                }
            }
        }

        // Mostrar encantamentos vanilla
        if (tool.enchantments.isNotEmpty()) {
            player.sendMessage("§6=== Encantamentos Vanilla ===")
            tool.enchantments.forEach { (enchant, enchantLevel) ->
                player.sendMessage("§7${enchant.key.key}: §e$enchantLevel")
            }
        }
    }

    private fun handleResetItem(player: Player) {
        if (!player.isOp) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!")
            return
        }

        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        val meta = tool.itemMeta ?: return
        val container = meta.persistentDataContainer

        // Remover todas as keys customizadas
        val keysToRemove = listOf(
            NamespacedKey(plugin, "fplus_nivel"),
            NamespacedKey(plugin, "fplus_usos"),
            NamespacedKey(plugin, "fplus_pontos"),
            NamespacedKey(plugin, "custom_enc"),
            NamespacedKey(plugin, "custom_enc_lvl"),
            NamespacedKey(plugin, "custom_enc_display")
        )

        for (key in keysToRemove) {
            container.remove(key)
        }

        // Remover encantamentos vanilla
        tool.enchantments.keys.forEach { enchant ->
            tool.removeEnchantment(enchant)
        }

        // Resetar durabilidade
        if (meta is Damageable) {
            meta.damage = 0
        }

        // Resetar lore
        meta.lore(null)
        tool.itemMeta = meta

        player.sendMessage("§aItem resetado para o estado inicial!")
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.isOp) {
            sender.sendMessage("§cVocê não tem permissão para isso!")
            return
        }

        plugin.reloadConfigurations()
        sender.sendMessage("§aConfigurações recarregadas com sucesso!")
    }

    private fun handleOpenMenu(player: Player) {
        if (!player.isOp) {
            player.sendMessage("§cVocê não tem permissão para isso!")
            return
        }

        val tool = player.inventory.itemInMainHand
        if (tool.type.isAir) {
            player.sendMessage("§cVocê precisa ter um item na mão!")
            return
        }

        ItemEvolutionPlugin.itemEvolutionListener.interceptToolAndOpenMerchant(player, tool)
        player.sendMessage("§aMenu aberto com sucesso!")
    }
}