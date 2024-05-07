package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.item.StorageCell
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage

@Init(stage = InitStage.PRE_PACK)
object Items: ItemRegistry by DeepStorage.registry {
    
    //Storage cells
    val STORAGE_CELL_1k = registerItem("storage_cell_1k", StorageCell)
    val STORAGE_CELL_4k = registerItem("storage_cell_4k", StorageCell)
    val STORAGE_CELL_16k = registerItem("storage_cell_16k", StorageCell)
    val STORAGE_CELL_64k = registerItem("storage_cell_64k", StorageCell)
    
    //Blocks
    val DEEP_STORAGE_UNIT = registerItem(Blocks.DEEP_STORAGE_UNIT)
    
}