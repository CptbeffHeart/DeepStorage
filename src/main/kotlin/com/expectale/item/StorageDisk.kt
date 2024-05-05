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

interface StorageDisk {
    
    val capacity: Int
    
    val itemAmount: Int
    
    fun add(disk: ItemStack, item: ItemStack) = add(disk, item, item.amount)
    
    fun add(disk: ItemStack, item: ItemStack, amount: Int): Int
    
    fun remove(disk: ItemStack, item: ItemStack) = remove(disk, item, item.amount)
    
    fun remove(disk: ItemStack, item: ItemStack, amount: Int): Int
    
    fun getItems(disk: ItemStack): Map<ItemStack, Int>
    
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
    ): ItemBehavior, StorageDisk {
        
        override val capacity by capacity
        override val itemAmount by itemAmount
        
        override fun add(disk: ItemStack, item: ItemStack, amount: Int): Int {
            val diskData = getDiskData(disk)
            val rest = diskData.add(item, amount)
            setDiskData(disk, diskData)
            return rest
        }
        
        override fun remove(disk: ItemStack, item: ItemStack, amount: Int): Int {
            val diskData = getDiskData(disk)
            val rest = diskData.remove(item, amount)
            setDiskData(disk, diskData)
            return rest
        }
        
        override fun getItems(disk: ItemStack): Map<ItemStack, Int> {
            return getDiskData(disk).getItems()
        }
        
        private fun getDiskData(disk: ItemStack): DiskData {
            return disk.retrieveData<DiskData>(DeepStorage, "diskData") ?: DiskData(capacity, itemAmount)
        }
        
        private fun setDiskData(disk: ItemStack, diskData: DiskData) {
            disk.storeData(DeepStorage, "diskData", diskData)
        }
        
    }
    
    class DiskData(
        private val capacity: Int,
        private val itemAmount: Int
    ) {
        
        private val dataMap: MutableMap<ItemStack, Int> = mutableMapOf()
        
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