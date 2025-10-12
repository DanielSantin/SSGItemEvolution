package com.ssg.itemevolution.handlers

import EnchantmentEventResult
import EnchantmentEventType
import ItemUsageEventResult
import ItemUsageType
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

    private val enchantmentHandlers = ConcurrentHashMap<String, ConcurrentHashMap<EnchantmentEventType, MutableList<EnchantmentEventHandler>>>()
    private val itemUsageHandlers = mutableListOf<ItemUsageEventHandler>()

    override fun initialize() {
        plugin.logger.info("Sistema de eventos inicializado")
    }

    override fun dispose() {
        enchantmentHandlers.clear()
        itemUsageHandlers.clear()
    }

    // REGISTRO DE HANDLERS

    /**
     * Registra um handler para encantamento específico
     */
    fun registerEnchantmentHandler(
        enchantmentKey: String,
        handler: EnchantmentEventHandler,
        eventType: EnchantmentEventType
    ) {
        val typeMap = enchantmentHandlers.computeIfAbsent(enchantmentKey) { ConcurrentHashMap() }
        typeMap.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
    }

    /**
     * Dispara evento de uso de encantamento
     */
    fun fireEnchantmentEvent(event: EnchantmentEvent): EnchantmentEventResult {
        val typeMap = enchantmentHandlers[event.enchantmentKey]
            ?: return EnchantmentEventResult.IGNORED

        // AQUI: Pegue a lista de handlers específicos para o tipo de evento
        val handlers = typeMap[event.type]
            ?: return EnchantmentEventResult.IGNORED

        var result = EnchantmentEventResult.IGNORED
        for (handler in handlers) {
            try {
                // Seu loop de execução e verificação de cancelamento permanece o mesmo
                val handlerResult = handler.handle(event)

                // Prioriza CANCELLED, depois HANDLED sobre IGNORED
                if (handlerResult != EnchantmentEventResult.IGNORED) {
                    // Mantenha o resultado mais importante (CANCELLED > HANDLED > IGNORED)
                    if (result != EnchantmentEventResult.CANCELLED) {
                        result = handlerResult
                    }
                }

                // Se foi cancelado, para aqui
                if (handlerResult == EnchantmentEventResult.CANCELLED) {
                    result = EnchantmentEventResult.CANCELLED // Garante que o retorno seja CANCELLED
                    break
                }
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao executar handler de encantamento ${event.enchantmentKey}:${event.type}: ${e.message}")
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
 * Evento de uso de item
 */
data class ItemUsageEvent(
    val usageType: ItemUsageType,
    val context: Map<String, Any> = emptyMap()
) : CustomEvent(
    context["player"] as Player,
    context["item"] as ItemStack
)

// BUILDER PARA EVENTOS

class EventBuilder {
    companion object {
        fun enchantment(enchantmentKey: String, level: Int, type: EnchantmentEventType): EnchantmentEventBuilder {
            return EnchantmentEventBuilder(enchantmentKey, level, type)
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