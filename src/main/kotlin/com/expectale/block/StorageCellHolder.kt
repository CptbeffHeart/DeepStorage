package com.expectale.block

import com.expectale.storage_cell.StorageCell
import com.expectale.storage_cell.VirtualStorageCell
import org.bukkit.inventory.ItemStack

interface StorageCellHolder {
    
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
        virtualMap.values.forEach { entry ->
            remainingAmount = entry.add(item, remainingAmount)
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
        virtualMap.values.forEach { entry ->
            remainingAmount = entry.remove(item, remainingAmount)
            if (remainingAmount == 0) return 0
        }
        return remainingAmount
    }
    
}