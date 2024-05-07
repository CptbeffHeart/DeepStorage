package com.expectale.item

import com.expectale.DeepStorage
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData

interface StorageCell {
    
    /**
     * How many items the [StorageCell] can store.
     */
    val capacity: Int
    
    /**
     * How many different items ths [StorageCell] can store.
     */
    val itemAmount: Int
    
    fun add(cell: ItemStack, item: ItemStack) = add(cell, item, item.amount)
    
    /**
     * Function to add [ItemStack] to the specified [StorageCell]
     */
    fun add(cell: ItemStack, item: ItemStack, amount: Int): Int
    
    fun remove(cell: ItemStack, item: ItemStack) = remove(cell, item, item.amount)
    
    /**
     * Function to remove [ItemStack] from the specified [StorageCell].
     */
    fun remove(cell: ItemStack, item: ItemStack, amount: Int): Int
    
    /**
     * [Map] containing all the [ItemStack] stored in the [StorageCell] with their amount
     */
    fun getItems(cell: ItemStack): Map<ItemStack, Int>
    
    companion object : ItemBehaviorFactory<Default> {
        
        override fun create(item: NovaItem): Default {
            return Default(
                item.config.entry<Int>("capacity"),
                item.config.entry<Int>("itemAmount")
            )
        }
        
    }
    
    class Default(
        capacity: Provider<Int>,
        itemAmount: Provider<Int>,
    ): ItemBehavior, StorageCell {
        
        override val capacity by capacity
        override val itemAmount by itemAmount
        
        override fun add(cell: ItemStack, item: ItemStack, amount: Int): Int {
            val cellData = getCellData(cell)
            val rest = cellData.add(item, amount)
            setCellData(cell, cellData)
            return rest
        }
        
        override fun remove(cell: ItemStack, item: ItemStack, amount: Int): Int {
            val cellData = getCellData(cell)
            val rest = cellData.remove(item, amount)
            setCellData(cell, cellData)
            return rest
        }
        
        override fun getItems(cell: ItemStack): Map<ItemStack, Int> {
            return getCellData(cell).getItems()
        }
        
        private fun getCellData(cell: ItemStack): CellData {
            val map = cell.retrieveData<MutableMap<ItemStack, Int>>(DeepStorage, "cell_data") ?: mutableMapOf()
            return CellData(capacity, itemAmount, map)
        }
        
        private fun setCellData(cell: ItemStack, cellData: CellData) {
            cell.storeData(DeepStorage, "cell_data", cellData.dataMap)
        }
        
    }
    
    class CellData(
        private val capacity: Int,
        private val itemAmount: Int,
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

}