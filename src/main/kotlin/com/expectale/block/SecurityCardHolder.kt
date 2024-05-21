package com.expectale.block

import com.expectale.item.SecurityCard
import org.bukkit.entity.Player
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.item.novaItem

interface SecurityCardHolder {

    val cardInventory: VirtualInventory
    
    var whiteList: Boolean
    
    fun canInputCard(player: Player): Boolean
    
    fun hasCardAccess(player: Player): Boolean {
        return cardInventory.items.any { item ->
            item?.novaItem?.getBehaviorOrNull<SecurityCard>()?.isOwner(item, player) == true
        }
    }
    
    fun handleCardUpdate(event: ItemPreUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || event.newItem == null) return
        if (event.updateReason !is PlayerUpdateReason) return
        val reason = event.updateReason as PlayerUpdateReason
        
        val card = event.newItem?.novaItem?.getBehaviorOrNull<SecurityCard>()
        if (card?.getOwner(event.newItem!!) == null || !canInputCard(reason.player)) {
            event.isCancelled = true
            return
        }
    }

}