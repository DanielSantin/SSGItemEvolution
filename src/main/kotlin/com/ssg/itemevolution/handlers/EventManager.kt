package com.ssg.itemevolution.handlers

import com.ssg.itemevolution.core.DisposableService
import com.ssg.itemevolution.core.InitializableService
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * Sistema centralizado de eventos do plugin
 */
class EventManager(private val plugin: JavaPlugin) : InitializableService, DisposableService {

    private val enchantmentHandlers = ConcurrentHashMap<String, MutableList<EnchantmentEventHandler>>()
    private val evolutionHandlers = mutableListOf<EvolutionEventHandler>()
    private val itemUsageHandlers = mutableListOf<ItemUsageEventHandler>()

    override fun initialize() {
        plugin.logger.info("Sistema de eventos inicializado")
    }

    override fun dispose() {
        enchantmentHandlers.clear()
        evolutionHandlers.clear()
        itemUsageHandlers.clear()
    }

    // REGISTRO DE HANDLERS

    /**
     * Registra um handler para encantamento específico
     */
    fun registerEnchantmentHandler(enchantmentKey: String, handler: EnchantmentEventHandler) {
        enchantmentHandlers.computeIfAbsent(enchantmentKey) { mutableListOf() }.add(handler)
    }

    /**
     * Registra um handler para eventos de evolução
     */
    fun registerEvolutionHandler(handler: EvolutionEventHandler) {
        evolutionHandlers.add(handler)
    }

    /**
     * Registra um handler para uso de itens
     */
    fun registerItemUsageHandler(handler: ItemUsageEventHandler) {
        itemUsageHandlers.add(handler)
    }

    // DISPATCH DE EVENTOS

    /**
     * Dispara evento de uso de encantamento
     */
    fun fireEnchantmentEvent(event: EnchantmentEvent): EnchantmentEventResult {
        plugin.logger.info("Enviando evento de encantamento para ${event.enchantmentKey}")
        val handlers = enchantmentHandlers[event.enchantmentKey] ?: return EnchantmentEventResult.IGNORED
        plugin.logger.info("Encontramos ${handlers.size} handlers para o encantamento ${event.enchantmentKey}")

        var result = EnchantmentEventResult.IGNORED
        for (handler in handlers) {
            try {
                plugin.logger.info("Executando handler de encantamento ${event.enchantmentKey}")
                plugin.logger.info("Resultado: ${event.type}")
                val handlerResult = handler.handle(event)
                if (handlerResult != EnchantmentEventResult.IGNORED) {
                    result = handlerResult
                }

                // Se foi cancelado, para aqui
                if (handlerResult == EnchantmentEventResult.CANCELLED) {
                    break
                }
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao executar handler de encantamento ${event.enchantmentKey}: ${e.message}")
                e.printStackTrace()
            }
        }

        return result
    }

    /**
     * Dispara evento de evolução
     */
    fun fireEvolutionEvent(event: EvolutionEvent): EvolutionEventResult {
        var result = EvolutionEventResult.CONTINUE

        for (handler in evolutionHandlers) {
            try {
                val handlerResult = handler.handle(event)
                if (handlerResult == EvolutionEventResult.CANCELLED) {
                    result = EvolutionEventResult.CANCELLED
                    break
                }
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao executar handler de evolução: ${e.message}")
                e.printStackTrace()
            }
        }

        return result
    }

    /**
     * Dispara evento de uso de item
     */
    fun fireItemUsageEvent(event: ItemUsageEvent): ItemUsageEventResult {
        var result = ItemUsageEventResult.CONTINUE

        for (handler in itemUsageHandlers) {
            try {
                val handlerResult = handler.handle(event)
                if (handlerResult == ItemUsageEventResult.CANCELLED) {
                    result = ItemUsageEventResult.CANCELLED
                    break
                }
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao executar handler de uso de item: ${e.message}")
                e.printStackTrace()
            }
        }

        return result
    }
}

// INTERFACES DOS HANDLERS

interface EnchantmentEventHandler {
    fun handle(event: EnchantmentEvent): EnchantmentEventResult
}

interface EvolutionEventHandler {
    fun handle(event: EvolutionEvent): EvolutionEventResult
}

interface ItemUsageEventHandler {
    fun handle(event: ItemUsageEvent): ItemUsageEventResult
}



// EVENTOS

/**
 * Evento base para todos os eventos customizados
 */
abstract class CustomEvent(
    val player: Player,
    val item: ItemStack
)

/**
 * Evento de uso de encantamento
 */
data class EnchantmentEvent(
    val enchantmentKey: String,
    val level: Int,
    val type: EnchantmentEventType,
    val context: Map<String, Any> = emptyMap()
) : CustomEvent(
    context["player"] as Player,
    context["item"] as ItemStack
)

/**
 * Evento de evolução de item
 */
data class EvolutionEvent(
    val oldLevel: Int,
    val newLevel: Int,
    val type: EvolutionEventType,
    val context: Map<String, Any> = emptyMap()
) : CustomEvent(
    context["player"] as Player,
    context["item"] as ItemStack
)

/**
 * Evento de uso de item
 */
data class ItemUsageEvent(
    val usageType: ItemUsageType,
    val context: Map<String, Any> = emptyMap()
) : CustomEvent(
    context["player"] as Player,
    context["item"] as ItemStack
)

// ENUMS

enum class EnchantmentEventType {
    BLOCK_BREAK,
    ENTITY_DAMAGE,
    ENTITY_DAMAGED,
    INTERACT,
    PROJECTILE_HIT,
    BEFORE_USE,
    AFTER_USE,
    ANVIL_APPLY,
}

enum class EnchantmentEventResult {
    HANDLED,
    CANCELLED,
    IGNORED
}

enum class EvolutionEventType {
    LEVEL_UP,
    UPGRADE_MATERIAL,
    GAIN_POINTS
}

enum class EvolutionEventResult {
    CONTINUE,
    CANCELLED
}

enum class ItemUsageType {
    ATTACK,
    BLOCK_BREAK,
    TAKE_DAMAGE,
    REPAIR,
    UPGRADE
}

enum class ItemUsageEventResult {
    CONTINUE,
    CANCELLED
}

// BUILDER PARA EVENTOS

class EventBuilder {
    companion object {
        fun enchantment(enchantmentKey: String, level: Int, type: EnchantmentEventType): EnchantmentEventBuilder {
            return EnchantmentEventBuilder(enchantmentKey, level, type)
        }

        fun evolution(oldLevel: Int, newLevel: Int, type: EvolutionEventType): EvolutionEventBuilder {
            return EvolutionEventBuilder(oldLevel, newLevel, type)
        }

        fun itemUsage(type: ItemUsageType): ItemUsageEventBuilder {
            return ItemUsageEventBuilder(type)
        }
    }
}

class EnchantmentEventBuilder(
    private val enchantmentKey: String,
    private val level: Int,
    private val type: EnchantmentEventType
) {
    private val context = mutableMapOf<String, Any>()

    fun player(player: Player) = apply { context["player"] = player }
    fun item(item: ItemStack) = apply { context["item"] = item }
    fun context(key: String, value: Any) = apply { context[key] = value }

    fun build(): EnchantmentEvent {
        require(context.containsKey("player")) { "Player é obrigatório" }
        require(context.containsKey("item")) { "Item é obrigatório" }
        return EnchantmentEvent(enchantmentKey, level, type, context.toMap())
    }
}

class EvolutionEventBuilder(
    private val oldLevel: Int,
    private val newLevel: Int,
    private val type: EvolutionEventType
) {
    private val context = mutableMapOf<String, Any>()

    fun player(player: Player) = apply { context["player"] = player }
    fun item(item: ItemStack) = apply { context["item"] = item }
    fun context(key: String, value: Any) = apply { context[key] = value }

    fun build(): EvolutionEvent {
        require(context.containsKey("player")) { "Player é obrigatório" }
        require(context.containsKey("item")) { "Item é obrigatório" }
        return EvolutionEvent(oldLevel, newLevel, type, context.toMap())
    }
}

class ItemUsageEventBuilder(private val type: ItemUsageType) {
    private val context = mutableMapOf<String, Any>()

    fun player(player: Player) = apply { context["player"] = player }
    fun item(item: ItemStack) = apply { context["item"] = item }
    fun context(key: String, value: Any) = apply { context[key] = value }

    fun build(): ItemUsageEvent {
        require(context.containsKey("player")) { "Player é obrigatório" }
        require(context.containsKey("item")) { "Item é obrigatório" }
        return ItemUsageEvent(type, context.toMap())
    }
}