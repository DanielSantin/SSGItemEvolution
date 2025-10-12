package com.ssg.itemevolution.handlers

class EnchantmentRegistrationHandler(
    private val eventManager: EventManager,
    private val enchantmentEventHandlers: EnchantmentEventHandlers
) {
    fun registerEnchantmentEventHandlers() {
        eventManager.registerEnchantmentHandler("supera:desabar", enchantmentEventHandlers.desabarHandler, EnchantmentEventType.BLOCK_BREAK)
        eventManager.registerEnchantmentHandler("supera:vein_mining", enchantmentEventHandlers.veinMiningHandler, EnchantmentEventType.BLOCK_BREAK)
        eventManager.registerEnchantmentHandler("supera:area_mining", enchantmentEventHandlers.areaMiningHandler, EnchantmentEventType.BLOCK_BREAK)
        eventManager.registerEnchantmentHandler("supera:eterna", enchantmentEventHandlers.eternaBeforeUseHandler, EnchantmentEventType.BEFORE_USE)
        eventManager.registerEnchantmentHandler("supera:eterna", enchantmentEventHandlers.eternaAfterUseHandler, EnchantmentEventType.AFTER_USE)
    }
}