package com.expectale.block

import com.expectale.item.StorageDisk
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.item.novaItem

interface StorageDiskHolder {

    val diskInventory: VirtualInventory
    
    fun callUpdateDisk()
    
    fun getSize(): Int {
        return diskInventory.items
            .filterNotNull()
            .mapNotNull { it.novaItem?.getBehaviorOrNull<StorageDisk>()?.itemAmount }
            .sum()
    }
    
    fun getItems(): MutableMap<ItemStack, Int> {
        return diskInventory.items
            .filterNotNull()
            .flatMap { disk ->
                disk.novaItem?.getBehaviorOrNull<StorageDisk>()?.getItems(disk)?.entries ?: emptySet()
            }
            .fold(mutableMapOf()) { map, (key, value) ->
                map.apply { merge(key, value) { oldVal, newVal -> oldVal + newVal } }
            }
    }
    
    fun getItemAmount(item: ItemStack): Int {
        return diskInventory.items
            .filterNotNull()
            .flatMap { disk ->
                disk.novaItem?.getBehaviorOrNull<StorageDisk>()?.getItems(disk)?.entries ?: emptySet()
            }
            .filter { it.key.isSimilar(item) }
            .sumOf { it.value }
    }
    
    fun addItemToDisk(item: ItemStack): Int {
        var add = item.amount
        diskInventory.items.forEachIndexed { index, disk ->
            if (disk == null) return@forEachIndexed
            val storageDisk = disk.novaItem?.getBehaviorOrNull<StorageDisk>() ?: return@forEachIndexed
            add = storageDisk.add(disk, item, add)
            diskInventory.setItem(TileEntity.SELF_UPDATE_REASON, index, disk)
            if (add == 0) return 0
        }
        return add
    }
    
    fun removeItem(item: ItemStack, amount: Int = item.amount): Int {
        var rest = amount
        diskInventory.items.forEachIndexed { index, disk ->
            if (disk == null) return@forEachIndexed
            val storageDisk = disk.novaItem?.getBehaviorOrNull<StorageDisk>() ?: return@forEachIndexed
            rest = storageDisk.remove(disk, item, rest)
            diskInventory.setItem(TileEntity.SELF_UPDATE_REASON, index, disk)
            if (rest == 0) return 0
        }
        return rest
    }
    
    fun handleDiskUpdate(event: ItemPreUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || (event.newItem == null && event.previousItem == null)) return
        val storageDisk = (event.newItem ?: event.previousItem)?.novaItem?.getBehaviorOrNull<StorageDisk>()
        if (storageDisk == null) {
            event.isCancelled = true
            return
        }
    }
    
    fun handlePostDiskUpdate(event: ItemPostUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || (event.newItem == null && event.previousItem == null)) return
        (event.newItem ?: event.previousItem)?.novaItem?.getBehaviorOrNull<StorageDisk>() ?: return
        callUpdateDisk()
    }

}