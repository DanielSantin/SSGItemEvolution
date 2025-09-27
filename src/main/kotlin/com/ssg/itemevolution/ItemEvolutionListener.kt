package com.ssg.itemevolution

import com.nexomc.nexo.api.NexoFurniture
import com.ssg.itemevolution.dialogs.SoulToolDialog
import com.ssg.itemevolution.dialogs.SoulToolUtils.hasSoul
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.entity.Snowman
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.util.Vector
import kotlin.math.abs

class ItemEvolutionListener : Listener {

    private val heldArmorIntercept: MutableMap<java.util.UUID, ItemStack> = mutableMapOf()
    private val hasNexo = Bukkit.getPluginManager().getPlugin("Nexo") != null
    private val soulToolDialog = SoulToolDialog()

    fun interceptToolAndOpenMerchant(player: Player, tool: ItemStack){
        if (ItemUtils.testTool(tool).isNotEmpty()) {
            heldArmorIntercept[player.uniqueId] = tool.clone() // Clona para evitar modificações
            player.inventory.removeItem(tool) // Remove o item da mão do jogador

            // Abrir o mercador num próximo tick para evitar conflitos de eventos.
            // Passaremos o item interceptado para o mercador.
            Bukkit.getScheduler().runTaskLater(ItemEvolutionPlugin.instance, Runnable {
                val interceptedTool = heldArmorIntercept.remove(player.uniqueId)

                if (interceptedTool != null) {
                    // Se a mão principal estiver vazia, coloca o item lá
                    if (player.inventory.itemInMainHand.isEmpty) {
                        player.inventory.setItemInMainHand(interceptedTool)
                    } else {
                        // Senão, adiciona no inventário
                        player.inventory.addItem(interceptedTool)
                    }

                    MerchantHandler.openMerchant(player, interceptedTool)
                } else {
                    // Se por algum motivo o item não foi interceptado,
                    // usamos o item atual na mão
                    MerchantHandler.openMerchant(player, player.inventory.itemInMainHand)
                }

                player.updateInventory() // Garante que o cliente veja a mudança
            }, 1L)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val player = event.player
        val tool = player.inventory.itemInMainHand
        val isBancadaNexo = hasNexo &&
                NexoFurniture.isFurniture(clickedBlock.location) &&
                NexoFurniture.furnitureMechanic(NexoFurniture.baseEntity(clickedBlock.location))?.itemID == "bancada_de_melhoria"

        val isVanillaSmithing = !hasNexo && clickedBlock.type == Material.SMITHING_TABLE

        if (isBancadaNexo || isVanillaSmithing) {
            cancelInteraction(event)
            if (hasSoul(tool)){
                interceptToolAndOpenMerchant(player, tool)
            } else {
                soulToolDialog.checkEligibilityAndShowDialog(player, tool)
            }
        }
    }

    private fun cancelInteraction(event: PlayerInteractEvent) {
        event.isCancelled = true
        event.setUseItemInHand(Event.Result.DENY)
        event.setUseInteractedBlock(Event.Result.DENY)
    }

    @EventHandler
        fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val tool = attacker.inventory.itemInMainHand

        //if (!hasSoul(tool)) return
        val toolCategory = ItemUtils.testTool(tool)

        when (toolCategory) {
            "bow", "crossbow" -> {
                attacker.inventory.setItemInMainHand(ItemUtils.upgradeItem(tool))
            }
            "sword", "axe" -> {
                val upgradedTool = ItemUtils.upgradeItem(tool)
                attacker.inventory.setItemInMainHand(upgradedTool)

                // Aplicar dano ao item (simulando desgaste)
                damageItem(upgradedTool, attacker)
            }
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = (event as? EntityDamageByEntityEvent)?.damager

        // Não processar dano de boneco de neve
        if (attacker is Snowman) return

        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
            event.cause == EntityDamageEvent.DamageCause.PROJECTILE) {

            // Processar armaduras
            processArmorUpgrade(victim.inventory.helmet)
            processArmorUpgrade(victim.inventory.chestplate)
            processArmorUpgrade(victim.inventory.leggings)
            processArmorUpgrade(victim.inventory.boots)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand
        //if (!hasSoul(tool)) return
        val toolCategory = ItemUtils.testTool(tool)

        if (toolCategory in listOf("axe", "hoe", "pickaxe", "shovel")) {
            val upgradedTool = ItemUtils.upgradeItem(tool)
            player.inventory.setItemInMainHand(upgradedTool)
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        MerchantHandler.restoreToolItem(player)
    }


    @EventHandler
    fun onEnchantItem(event: EnchantItemEvent) {
        val item = event.item
        if (hasSoul(item)) {
            event.isCancelled = true
            return
        }
    }

    private fun processArmorUpgrade(armor: ItemStack?) {
        if (armor != null && ToolType.getAllToolMaterials().contains(armor.type)) {
            //if (!hasSoul(armor)) return
            val upgradedArmor = ItemUtils.upgradeItem(armor)
            armor.itemMeta = upgradedArmor.itemMeta
            armor.addEnchantments(upgradedArmor.enchantments)
        }
    }

    private fun damageItem(item: ItemStack, player: Player) {
        if (item.type.maxDurability > 0) {
            val meta = item.itemMeta as? org.bukkit.inventory.meta.Damageable ?: return
            val newDamage = meta.damage + 1

            if (newDamage >= item.type.maxDurability) {
                // Item quebrou
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            } else {
                meta.damage = newDamage
                item.itemMeta = meta
            }
        }
    }
}