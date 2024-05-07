package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.tileentity.DeepStorageUnit
import org.bukkit.Material
import xyz.xenondevs.nova.addon.registry.BlockRegistry
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.item.tool.VanillaToolTiers
import xyz.xenondevs.nova.world.block.sound.SoundGroup

@Init(stage = InitStage.PRE_PACK)
object Blocks : BlockRegistry by DeepStorage.registry {
    
    private val DEEP_STORAGE_UNIT_BLOCK_OPTION = BlockOptions(3.0, VanillaToolCategories.PICKAXE, VanillaToolTiers.IRON,
        false, SoundGroup.STONE, Material.STONE)
    
    val DEEP_STORAGE_UNIT = tileEntity("deep_storage_unit", ::DeepStorageUnit)
        .blockOptions(DEEP_STORAGE_UNIT_BLOCK_OPTION).properties(Directional.NORMAL).register()
    
}