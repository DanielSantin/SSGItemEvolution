import com.ssg.itemevolution.EnchantmentHandler
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class MiningEnchantment(val name: String) {
    abstract fun execute(player: Player, block: Block, tool: ItemStack)
    abstract fun isValidBlock(block: Block): Boolean

    fun canExecute(tool: ItemStack): Boolean {
        return EnchantmentHandler.quickHasCustomEnchantment(tool, name)
    }
}