package com.expectale.storage_cell

import org.bukkit.inventory.ItemStack

class CellData(
    val capacity: Int,
    val itemAmount: Int,
    val dataMap: MutableMap<ItemStack, Int>
) {
    
    fun getStoredItemTypeAmount(): Int {
        return dataMap.size
    }
    
    fun getStoredBytesAmount(): Int {
        return dataMap.values.sum()
    }
    
    fun getItems(): Map<ItemStack, Int> {
        return dataMap.toMap()
    }
    
    fun get(item: ItemStack): Int {
        return dataMap[item] ?: 0
    }
    
    fun add(item: ItemStack, amount: Int): Int {
        return addData(transform(item), amount)
    }
    
    private fun addData(item: ItemStack, amount: Int): Int {
        val currentAmount = get(item)
        if (currentAmount == 0 &&
            (getStoredItemTypeAmount() >= itemAmount || getStoredBytesAmount() >= capacity)) return amount
        
        return if (getStoredBytesAmount() + amount <= capacity) {
            dataMap[item] = (dataMap[item] ?: 0) + amount
            0
        } else {
            val diff = capacity - getStoredBytesAmount()
            dataMap[item] = (dataMap[item] ?: 0) + diff
            amount - diff
        }
    }
    
    fun remove(item: ItemStack, amount: Int): Int {
        return removeData(transform(item), amount)
    }
    
    private fun removeData(item: ItemStack, amount: Int): Int {
        if (!dataMap.containsKey(item)) return amount
        val currentAmount = get(item)
        
        return if (currentAmount >= amount) {
            if (currentAmount == amount) dataMap.remove(item)
            else dataMap[item] = dataMap[item]!! - amount
            0
        } else {
            val diff = amount - currentAmount
            dataMap.remove(item)
            diff
        }
    }
    
    private fun transform(item: ItemStack): ItemStack {
        val clone = item.clone()
        clone.amount = 1
        return clone
    }
    
}