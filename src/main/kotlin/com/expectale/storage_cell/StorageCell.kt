package com.expectale.storage_cell

import com.expectale.DeepStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.util.item.novaItem
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
    
    /**
     * [Map] containing all the [ItemStack] stored in the [StorageCell] with their amount
     */
    fun getItems(cell: ItemStack): Map<ItemStack, Int>
    
    /**
     * Function to check if [StorageCell] is empty
     */
    fun isEmpty(cell: ItemStack): Boolean
    
    fun toVirtual(cell: ItemStack): VirtualStorageCell
    
    companion object : ItemBehaviorFactory<Default> {
        
        override fun create(item: NovaItem): Default {
            return Default(
                item.config.entry<Int>("capacity"),
                item.config.entry<Int>("itemAmount")
            )
        }
        
        fun byteDisplay(value: Int, max: Int): TextComponent {
            return Component.text()
                .append(
                    Component.translatable(
                        "item.deep_storage.storage_cell.bytes",
                        Component.text("$value").color(NamedTextColor.GREEN),
                        Component.text("$max").color(NamedTextColor.BLUE)
                    ).color(NamedTextColor.GRAY)
                )
                .build()
        }
        
        fun typeDisplay(value: Int, max: Int): TextComponent {
            return Component.text()
                .append(
                    Component.translatable(
                        "item.deep_storage.storage_cell.types",
                        Component.text("$value").color(NamedTextColor.GREEN),
                        Component.text("$max").color(NamedTextColor.BLUE)
                    ).color(NamedTextColor.GRAY)
                )
                .build()
        }
        
    }
    
    class Default(
        capacity: Provider<Int>,
        itemAmount: Provider<Int>,
    ): ItemBehavior, StorageCell {
        
        override val capacity by capacity
        override val itemAmount by itemAmount
        
        override fun getItems(cell: ItemStack): Map<ItemStack, Int> {
            return getCellData(cell).getItems()
        }
        
        override fun isEmpty(cell: ItemStack): Boolean {
            val cellData = getCellData(cell)
            return cellData.getStoredBytesAmount() == 0 && cellData.getStoredItemTypeAmount() == 0
        }
        
        override fun toVirtual(cell: ItemStack): VirtualStorageCell {
            return VirtualStorageCell(getCellData(cell), cell.novaItem!!)
        }
        
        private fun getCellData(cell: ItemStack): CellData {
            val map = cell.retrieveData<MutableMap<ItemStack, Int>>(DeepStorage, "cell_data") ?: mutableMapOf()
            return CellData(capacity, itemAmount, map)
        }
        
        fun setCellData(cell: ItemStack, cellData: CellData) {
            cell.storeData(DeepStorage, "cell_data", cellData.dataMap)
        }
        
        private fun getCellData(data: NamespacedCompound): CellData {
            val mapData: MutableMap<ItemStack, Int> = data[DeepStorage, "cell_data"] ?: mutableMapOf()
            return CellData(capacity, itemAmount, mapData)
        }
        
        override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
            val cellData = getCellData(data)
            itemData.addLore(byteDisplay(cellData.getStoredBytesAmount(), cellData.capacity))
            itemData.addLore(typeDisplay(cellData.getStoredItemTypeAmount(), cellData.itemAmount))
        }
    }

}