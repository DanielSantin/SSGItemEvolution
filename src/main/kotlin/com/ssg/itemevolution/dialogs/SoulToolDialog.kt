package com.ssg.itemevolution.dialogs

import io.papermc.paper.dialog.*
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SoulToolDialog {

    fun checkEligibilityAndShowDialog(player: Player, tool: ItemStack) {
        val result = SoulToolUtils.checkSoulEligibility(tool)
        if (result != SoulToolUtils.SoulToolCheckResult.VALID) {
            result.message?.let { player.sendMessage(it) }
            return
        }
        showSoulTransformDialog(player, tool)
    }

    fun showSoulTransformDialog(player: Player, tool: ItemStack) {
        val confirmAction = DialogAction.customClick({ view, audience ->
            if (audience is Player) {
                handleTransformation(audience, tool)
            }
        }, ClickCallback.Options.builder()
            .uses(1)
            .lifetime(ClickCallback.DEFAULT_LIFETIME)
            .build()
        )

        val cancelAction = DialogAction.customClick({ view, audience ->
            if (audience is Player) {
                handleCancellation(audience)
            }
        }, ClickCallback.Options.builder()
            .uses(1)
            .lifetime(ClickCallback.DEFAULT_LIFETIME)
            .build()
        )

        val dialog = createTransformDialog(confirmAction, cancelAction)

        player.showDialog(dialog)
        playDialogOpenSound(player)
    }

    private fun handleTransformation(player: Player, tool: ItemStack) {
        val success = SoulToolUtils.transformToSoulTool(tool)

        if (success) {
            sendTransformationSuccessMessage(player)
            playTransformationSuccessSound(player)
        } else {
            sendTransformationErrorMessage(player)
            playErrorSound(player)
        }
    }

    private fun handleCancellation(player: Player) {
        sendCancellationMessage(player)
        playCancellationSound(player)
    }

    private fun createTransformDialog(confirmAction: DialogAction, cancelAction: DialogAction): Dialog {
        return Dialog.create({ builder ->
            builder.empty()
                .base(createDialogBase())
                .type(createConfirmationType(confirmAction, cancelAction))
        })
    }

    private fun createDialogBase(): DialogBase {
        return DialogBase.builder(
            Component.text("⚔ Ferramenta com Alma ⚔")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
        )
            .body(createDialogBody())
            .build()
    }

    private fun createDialogBody(): List<DialogBody> {
        return listOf(
            DialogBody.plainMessage(
                Component.text("Deseja transformar sua ferramenta em uma")
                    .color(NamedTextColor.YELLOW)
            ),
            DialogBody.plainMessage(
                Component.text("Ferramenta com Alma?")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.BOLD, true)
            ),
            DialogBody.plainMessage(Component.empty()),
            DialogBody.plainMessage(
                Component.text("✨ Vantagens:")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
            ),
            DialogBody.plainMessage(
                Component.text("• Evolui automaticamente com o uso")
                    .color(NamedTextColor.WHITE)
            ),
            DialogBody.plainMessage(
                Component.text("• Ganha pontos de melhoria ao usar")
                    .color(NamedTextColor.WHITE)
            ),
            DialogBody.plainMessage(
                Component.text("• Acesso a encantamentos especiais")
                    .color(NamedTextColor.WHITE)
            ),
            DialogBody.plainMessage(Component.empty()),
            DialogBody.plainMessage(
                Component.text("⚠ Limitações:")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
            ),
            DialogBody.plainMessage(
                Component.text("• Não pode receber encantamentos vanilla")
                    .color(NamedTextColor.GRAY)
            ),
            DialogBody.plainMessage(
                Component.text("• Não pode ser combinada com outras ferramentas")
                    .color(NamedTextColor.GRAY)
            ),
            DialogBody.plainMessage(
                Component.text("• Esta transformação é permanente!")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true)
            )
        )
    }

    private fun createConfirmationType(confirmAction: DialogAction, cancelAction: DialogAction): DialogType {
        return DialogType.confirmation(
            ActionButton.create(
                Component.text("✅ Sim, transformar!").color(TextColor.color(0x00FF7F)),
                Component.text("Clique para transformar sua ferramenta em uma Ferramenta com Alma."),
                100,
                confirmAction
            ),
            ActionButton.create(
                Component.text("❌ Não, cancelar").color(TextColor.color(0xFF6B6B)),
                Component.text("Clique para manter sua ferramenta normal."),
                100,
                cancelAction
            )
        )
    }

    // === Métodos de Feedback e Sons ===

    private fun sendTransformationSuccessMessage(player: Player) {
        player.sendMessage(
            """
            §6[SSG] §a✨ Transformação concluída com sucesso!
            §6[SSG] §7Sua ferramenta agora possui uma §dalma §7e evoluirá com o tempo.
            §6[SSG] §7Use-a para ganhar pontos de melhoria!
            """.trimIndent()
        )
    }

    private fun sendTransformationErrorMessage(player: Player) {
        player.sendMessage("§6[SSG] §cErro ao transformar a ferramenta. Tente novamente.")
    }

    private fun sendCancellationMessage(player: Player) {
        player.sendMessage("§6[SSG] §7Transformação cancelada.")
    }

    private fun playDialogOpenSound(player: Player) {
        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f)
    }

    private fun playTransformationSuccessSound(player: Player) {
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f)
    }

    private fun playCancellationSound(player: Player) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1.0f, 0.8f)
    }

    private fun playErrorSound(player: Player) {
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
    }
}