package com.expectale.block

import com.expectale.storage_cell.StorageCell
import com.expectale.storage_cell.VirtualStorageCell
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.invui.inventory.get
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.VoidingVirtualInventory
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.item.novaItem

interface StorageCellHolder {
    
    val cellInventory: VirtualInventory
    
    val virtualMap: HashMap<Int, VirtualStorageCell>
    
    /**
     * Function called when the [StorageCell] are updated.
     */
    fun callUpdateCell()
    
    /**
     * How many slots the inventory has.
     */
    fun getSize(): Int {
        return virtualMap.values.sumOf { virtualStorage -> virtualStorage.cellData.itemAmount }
    }
    
    /**
     * [Map] of all items from all [StorageCell] with their amount.
     */
    fun getItems(): Map<ItemStack, Int> {
        return mutableMapOf<ItemStack, Int>().apply {
            virtualMap.values.forEach {
                putAll(it.cellData.getItems())
            }
        }
    }
    
    /**
     * Amount of specified [ItemStack] left on all [StorageCell].
     */
    fun getItemAmount(item: ItemStack): Int {
        return virtualMap.values.sumOf { entry ->
            entry.cellData.getItems().filter { it.key.isSimilar(item) }.values.sum()
        }
    }
    
    /**
     * Add [ItemStack] in [StorageCell] that has space for.
     * Return the amount of item that can't be stored.
     */
    fun addItemToCell(item: ItemStack, update: Boolean = true): Int {
        var remainingAmount = item.amount
        virtualMap.values.forEachIndexed { index, entry ->
            remainingAmount = entry.add(item, remainingAmount)
            updateCell(index)
            if (remainingAmount == 0) return 0
        }
        return remainingAmount
    }
    
    
    /**
     * Remove [ItemStack] from [StorageCell] that has it.
     * Return the amount of item that can't be removed.
     */
    fun removeItem(item: ItemStack, amount: Int = item.amount, update: Boolean = true): Int {
        var remainingAmount = amount
        virtualMap.values.forEachIndexed { index, entry ->
            remainingAmount = entry.remove(item, remainingAmount)
            updateCell(index)
            if (remainingAmount == 0) return 0
        }
        return remainingAmount
    }
    
    //TODO Change save method (hotfix)
    fun toInventory(): VirtualInventory {
        val virtualInventory = VirtualInventory(12)
        virtualMap.entries.forEach { (key, value) ->
            virtualInventory.setItem(TileEntity.SELF_UPDATE_REASON, key, value.toItem())
        }
        
        return virtualInventory
    }
    
    fun fromInventory(virtualInventory: VirtualInventory): HashMap<Int, VirtualStorageCell> {
        val map = HashMap<Int, VirtualStorageCell>()
        for (i in 0..< virtualInventory.size) {
            val itemStack = virtualInventory[i] ?: continue
            val toVirtual = itemStack.novaItem?.getBehaviorOrNull<StorageCell>()?.toVirtual(itemStack) ?: continue
            map[i] = toVirtual
        }
        return map
    }
    
    fun updateCell(slot: Int) {
        cellInventory.setItem(TileEntity.SELF_UPDATE_REASON, slot,
            if (virtualMap.containsKey(slot)) virtualMap[slot]!!.toDisplay().get() else null)
    }
    
    fun updateCellInv() {
        for (i in 0..< cellInventory.size) updateCell(i)
        cellInventory.notifyWindows()
        callUpdateCell()
    }
    
    fun handleCellUpdate(event: ItemPreUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || (event.newItem == null && event.previousItem == null)) return
        
        if (event.isAdd) {
            event.isCancelled = event.newItem?.novaItem?.getBehaviorOrNull<StorageCell>() == null
        } else if (event.isRemove) {
            event.isCancelled = true
            if (!virtualMap.containsKey(event.slot)) return
            if (event.updateReason is PlayerUpdateReason) {
                val player = (event.updateReason as PlayerUpdateReason).player
                player.addToInventoryOrDrop(listOf(virtualMap.remove(event.slot)!!.toItem()))
            }
            updateCellInv()
        } else if (event.isSwap) {
            event.isCancelled = true
        }
    }
    
    fun handlePostCellUpdate(event: ItemPostUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || event.newItem == null) return
        if (event.isAdd) {
            val stack = event.newItem
            val storageCell = stack?.novaItem?.getBehaviorOrNull<StorageCell>() ?: return
            virtualMap[event.slot] = storageCell.toVirtual(stack)
            stack.amount = 0
            updateCellInv()
        }
    }
    
}