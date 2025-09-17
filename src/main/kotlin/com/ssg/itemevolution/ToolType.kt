package com.ssg.itemevolution

import org.bukkit.Material

enum class ToolType(val materials: List<Material>) {
    LEATHER(listOf(Material.LEATHER_HELMET, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE)),
    CHAINMAIL(listOf(Material.CHAINMAIL_HELMET, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_CHESTPLATE)),
    WOOD(listOf(Material.WOODEN_SWORD, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_HOE, Material.WOODEN_SHOVEL)),
    STONE(listOf(Material.STONE_SWORD, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE, Material.STONE_SHOVEL)),
    IRON(listOf(
        Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE, Material.IRON_SHOVEL,
        Material.IRON_HELMET, Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE
    )),
    GOLD(listOf(
        Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE, Material.GOLDEN_SHOVEL,
        Material.GOLDEN_HELMET, Material.GOLDEN_BOOTS, Material.GOLDEN_LEGGINGS, Material.GOLDEN_CHESTPLATE
    )),
    DIAMOND(listOf(
        Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE, Material.DIAMOND_SHOVEL,
        Material.DIAMOND_HELMET, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE
    )),
    NETHERITE(listOf(
        Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE, Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HELMET, Material.NETHERITE_BOOTS, Material.NETHERITE_LEGGINGS, Material.NETHERITE_CHESTPLATE
    ));

    companion object {
        fun fromMaterial(material: Material): ToolType? {
            return entries.find { it.materials.contains(material) }
        }

        fun getAllToolMaterials(): List<Material> {
            val allMaterials = mutableListOf<Material>()
            entries.forEach { allMaterials.addAll(it.materials) }
            allMaterials.addAll(listOf(Material.BOW, Material.SHIELD, Material.CROSSBOW))
            return allMaterials
        }
    }
}

enum class ToolCategory {
    SWORD,
    PICKAXE,
    AXE,
    SHOVEL,
    HOE,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    BOW,
    SHIELD,
    CROSSBOW,
    UNKNOWN;

    companion object {
        fun fromMaterial(material: Material): ToolCategory {
            return when {
                material.name.endsWith("_SWORD") -> SWORD
                material.name.endsWith("_PICKAXE") -> PICKAXE
                material.name.endsWith("_AXE") -> AXE
                material.name.endsWith("_SHOVEL") -> SHOVEL
                material.name.endsWith("_HOE") -> HOE
                material.name.endsWith("_HELMET") -> HELMET
                material.name.endsWith("_CHESTPLATE") -> CHESTPLATE
                material.name.endsWith("_LEGGINGS") -> LEGGINGS
                material.name.endsWith("_BOOTS") -> BOOTS
                material == Material.BOW -> BOW
                material == Material.SHIELD -> SHIELD
                material == Material.CROSSBOW -> CROSSBOW
                else -> UNKNOWN
            }
        }

        fun isArmor(category: ToolCategory): Boolean {
            return category in listOf(HELMET, CHESTPLATE, LEGGINGS, BOOTS)
        }

//        fun isWeapon(category: ToolCategory): Boolean {
//            return category in listOf(SWORD, AXE, BOW, CROSSBOW)
//        }
//
//        fun isTool(category: ToolCategory): Boolean {
//            return category in listOf(PICKAXE, AXE, SHOVEL, HOE)
//        }
    }
}

object MaterialQuantity {
    fun getMaterialQuantity(material: Material): Int {
        return when (ToolCategory.fromMaterial(material)) {
            ToolCategory.SWORD -> 2
            ToolCategory.PICKAXE -> 3
            ToolCategory.AXE -> 3
            ToolCategory.SHOVEL -> 1
            ToolCategory.HOE -> 1
            ToolCategory.HELMET -> 5
            ToolCategory.CHESTPLATE -> 8
            ToolCategory.LEGGINGS -> 7
            ToolCategory.BOOTS -> 4
            ToolCategory.BOW -> 2
            ToolCategory.SHIELD -> 2
            ToolCategory.CROSSBOW -> 2
            else -> 1
        }
    }
}