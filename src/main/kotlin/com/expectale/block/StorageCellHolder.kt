package com.expectale.block

import com.expectale.item.StorageCell
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.item.novaItem

interface StorageCellHolder {
    
    /**
     * [VirtualInventory] where the [StorageCell] are stored.
     */
    val cellInventory: VirtualInventory
    
    /**
     * Function called when the [StorageCell] are updated.
     */
    fun callUpdateCell()
    
    /**
     * How many slots the inventory has.
     */
    fun getSize(): Int {
        return cellInventory.items
            .filterNotNull()
            .mapNotNull { it.novaItem?.getBehaviorOrNull<StorageCell>()?.itemAmount }
            .sum()
    }
    
    /**
     * [Map] of all items from all [StorageCell] with their amount.
     */
    fun getItems(): Map<ItemStack, Int> {
        return cellInventory.items
            .filterNotNull()
            .flatMap { cell ->
                cell.novaItem?.getBehaviorOrNull<StorageCell>()?.getItems(cell)?.entries ?: emptySet()
            }
            .fold(mutableMapOf()) { map, (key, value) ->
                map.apply { merge(key, value) { oldVal, newVal -> oldVal + newVal } }
            }
    }
    
    /**
     * Amount of specified [ItemStack] left on all [StorageCell].
     */
    fun getItemAmount(item: ItemStack): Int {
        return cellInventory.items
            .filterNotNull()
            .flatMap { cell ->
                cell.novaItem?.getBehaviorOrNull<StorageCell>()?.getItems(cell)?.entries ?: emptySet()
            }
            .filter { it.key.isSimilar(item) }
            .sumOf { it.value }
    }
    
    
    /**
     * Add [ItemStack] in [StorageCell] that has space for.
     * Return the amount of item that can't be stored.
     */
    fun addItemToCell(item: ItemStack): Int {
        var add = item.amount
        cellInventory.items.forEachIndexed { index, cell ->
            if (cell == null) return@forEachIndexed
            val storageCell = cell.novaItem?.getBehaviorOrNull<StorageCell>() ?: return@forEachIndexed
            add = storageCell.add(cell, item, add)
            cellInventory.setItem(TileEntity.SELF_UPDATE_REASON, index, cell)
            if (add == 0) return 0
        }
        return add
    }
    
    /**
     * Remove [ItemStack] from [StorageCell] that has it.
     * Return the amount of item that can't be removed.
     */
    fun removeItem(item: ItemStack, amount: Int = item.amount): Int {
        var rest = amount
        cellInventory.items.forEachIndexed { index, cell ->
            if (cell == null) return@forEachIndexed
            val storageCell = cell.novaItem?.getBehaviorOrNull<StorageCell>() ?: return@forEachIndexed
            rest = storageCell.remove(cell, item, rest)
            cellInventory.setItem(TileEntity.SELF_UPDATE_REASON, index, cell)
            if (rest == 0) return 0
        }
        return rest
    }
    
    fun handleCellUpdate(event: ItemPreUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || (event.newItem == null && event.previousItem == null)) return
        val storageCell = (event.newItem ?: event.previousItem)?.novaItem?.getBehaviorOrNull<StorageCell>()
        if (storageCell == null) {
            event.isCancelled = true
            return
        }
    }
    
    fun handlePostCellUpdate(event: ItemPostUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || (event.newItem == null && event.previousItem == null)) return
        (event.newItem ?: event.previousItem)?.novaItem?.getBehaviorOrNull<StorageCell>() ?: return
        callUpdateCell()
    }

}