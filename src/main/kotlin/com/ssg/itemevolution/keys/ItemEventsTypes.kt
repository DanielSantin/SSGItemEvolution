enum class EnchantmentEventType {
    BLOCK_BREAK,
    ENTITY_DAMAGE,
    ENTITY_DAMAGED,
    BEFORE_USE,
    AFTER_USE,
}

enum class EnchantmentEventResult {
    HANDLED,
    CANCELLED,
    IGNORED
}

enum class ItemUsageType {
    ATTACK,
    BLOCK_BREAK,
    TAKE_DAMAGE
}

enum class ItemUsageEventResult {
    CONTINUE,
    CANCELLED
}