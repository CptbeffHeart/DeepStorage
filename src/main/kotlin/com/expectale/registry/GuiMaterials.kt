package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.item.StorageCell
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage

@Init(stage = InitStage.PRE_PACK)
object GuiMaterials: ItemRegistry by DeepStorage.registry {
    
    val SECURITY_CARD_PLACEHOLDER = registerItem("gui_security_card_placeholder", localizedName = "", isHidden = true)
    val STORAGE_CELL_PLACEHOLDER = registerItem("gui_storage_cell_placeholder", localizedName = "", isHidden = true)
    
    val ALPHABETICAL_SORT = registerItem("gui_sort_alphabetical", localizedName = "menu.deep_storage.items.alphabetical_sort", isHidden = true)
    val SECURITY_CARD = registerItem("gui_security_card", localizedName = "menu.deep_storage.items.security_card", isHidden = true)
    val STACK_SORT = registerItem("gui_sort_stack", localizedName = "menu.deep_storage.items.stack_sort", isHidden = true)
    val STORAGE_CELL = registerItem("gui_storage_cell", localizedName = "menu.deep_storage.items.storage_cell", isHidden = true)
    val WHITELIST_OFF = registerItem("gui_whitelist_off", localizedName = "menu.deep_storage.items.whitelist_off", isHidden = true)
    val WHITELIST_ON = registerItem("gui_whitelist_on", localizedName = "menu.deep_storage.items.whitelist_on", isHidden = true)
    
}