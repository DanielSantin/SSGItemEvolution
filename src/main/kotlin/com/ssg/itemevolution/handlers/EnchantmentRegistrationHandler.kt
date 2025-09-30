package com.ssg.itemevolution.handlers

class EnchantmentRegistrationHandler(
    private val eventManager: EventManager,
    private val enchantmentEventHandlers: EnchantmentEventHandlers
) {
    fun registerEnchantmentEventHandlers() {
        eventManager.registerEnchantmentHandler("supera:desabar", enchantmentEventHandlers.desabarHandler)
        eventManager.registerEnchantmentHandler("supera:vein_mining", enchantmentEventHandlers.veinMiningHandler)
        eventManager.registerEnchantmentHandler("supera:eterna", enchantmentEventHandlers.eternaHandler)
    }
}