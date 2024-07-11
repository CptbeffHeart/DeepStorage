package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.item.EmptySecurityCard
import com.expectale.item.SecurityCard
import com.expectale.storage_cell.StorageCell
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage

@Init(stage = InitStage.PRE_PACK)
object Items: ItemRegistry by DeepStorage.registry {
    
    //Storage Cells
    val STORAGE_CELL_1k = registerItem("storage_cell_1k", StorageCell)
    val STORAGE_CELL_4k = registerItem("storage_cell_4k", StorageCell)
    val STORAGE_CELL_16k = registerItem("storage_cell_16k", StorageCell)
    val STORAGE_CELL_64k = registerItem("storage_cell_64k", StorageCell)
    
    //Storage Components
    val CELL_COMPONENT_1k = registerItem("cell_component_1k")
    val CELL_COMPONENT_4k = registerItem("cell_component_4k")
    val CELL_COMPONENT_16k = registerItem("cell_component_16k")
    val CELL_COMPONENT_64k = registerItem("cell_component_64k")
    
    //Security Card
    val EMPTY_SECURITY_CARD = registerItem("empty_security_card", EmptySecurityCard)
    val SECURITY_CARD = registerItem("security_card", SecurityCard)
    
    //Blocks
    val DEEP_STORAGE_UNIT = registerItem(Blocks.DEEP_STORAGE_UNIT)
    
}