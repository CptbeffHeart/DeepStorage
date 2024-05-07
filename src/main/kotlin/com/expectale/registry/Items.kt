package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.item.StorageDisk
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage

@Init(stage = InitStage.PRE_PACK)
object Items: ItemRegistry by DeepStorage.registry {
    
    //Storage disks
    val STORAGE_DISK_1k = registerItem("storage_disk_1k", StorageDisk)
    
    //Blocks
    val DEEP_STORAGE_UNIT = registerItem(Blocks.DEEP_STORAGE_UNIT)
    
}