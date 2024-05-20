package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.item.StorageCell
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage

@Init(stage = InitStage.PRE_PACK)
object GuiMaterials: ItemRegistry by DeepStorage.registry {
    
    val STORAGE_CELL_PLACEHOLDER = registerItem("gui_storage_cell_placeholder", localizedName = "", isHidden = true)
    val STORAGE_CELL = registerItem("gui_storage_cell", localizedName = "menu.deep_storage.items.storage_cell", isHidden = true)
    val ALPHABETICAL_SORT = registerItem("gui_sort_alphabetical", localizedName = "menu.deep_storage.items.alphabetical_sort", isHidden = true)
    val STACK_SORT = registerItem("gui_sort_stack", localizedName = "menu.deep_storage.items.stack_sort", isHidden = true)
    
}