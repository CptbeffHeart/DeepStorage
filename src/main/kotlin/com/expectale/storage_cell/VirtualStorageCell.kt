package com.expectale.storage_cell

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.addLoreLines
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.nova.item.NovaItem

class VirtualStorageCell(
    val cellData: CellData,
    private val novaItem: NovaItem
) {
    
    fun add(item: ItemStack, amount: Int): Int {
        return cellData.add(item, amount)
    }
    
    fun remove(item: ItemStack, amount: Int): Int {
        return cellData.remove(item, amount)
    }
    
    fun getItems(): Map<ItemStack, Int> {
        return cellData.getItems()
    }
    
    fun isEmpty(): Boolean {
        return cellData.getStoredBytesAmount() == 0 && cellData.getStoredItemTypeAmount() == 0
    }
    
    fun toItem(): ItemStack {
        val itemStack = novaItem.createItemStack()
        val default = StorageCell.create(novaItem)
        default.setCellData(itemStack, cellData)
        return itemStack
    }
    
    fun toDisplay(): ItemBuilder {
        val builder = novaItem.createClientsideItemBuilder()
        builder.setDisplayName(novaItem.name)
        builder.addLoreLines(StorageCell.byteDisplay(cellData.getStoredBytesAmount(), cellData.capacity))
        builder.addLoreLines(StorageCell.typeDisplay(cellData.getStoredItemTypeAmount(), cellData.itemAmount))
        return builder
    }
}