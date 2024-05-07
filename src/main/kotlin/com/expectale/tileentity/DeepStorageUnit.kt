package com.expectale.tileentity

import com.expectale.block.StorageCellHolder
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.item.DefaultItems
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigMenu
import xyz.xenondevs.nova.ui.item.BackItem
import xyz.xenondevs.nova.ui.item.clickableItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.VoidingVirtualInventory
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.runTaskLater

class DeepStorageUnit(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), StorageCellHolder {
    
    private val cellInv = getInventory("cell", 12, IntArray(12) { 1 }, false, ::handleCellUpdate, ::handlePostCellUpdate)
    private val inputInv = VoidingVirtualInventory(1).apply { setPreUpdateHandler(::handlePreInput) }
    private val inventory = DeepStorageInventory(getInventory("inventory", getSize()))
    
    override val itemHolder = NovaItemHolder(
        this,
        uuid to (inventory to NetworkConnectionType.BUFFER)
    ) { createSideConfig(NetworkConnectionType.BUFFER, BlockSide.FRONT) }
    
    override val cellInventory: VirtualInventory
        get() = cellInv
    
    override fun callUpdateCell() {
        inventory.resize()
        menuContainer.forEachMenu(DeepStorageUnitMenu::update)
    }
    
    private fun handlePreInput(event: ItemPreUpdateEvent) {
        if (event.updateReason == SELF_UPDATE_REASON || event.newItem == null) return
        val item = event.newItem!!
        val rest = addItemToCell(item)
        menuContainer.forEachMenu(DeepStorageUnitMenu::update)
        
        if (rest == 0) return
        if (event.updateReason !is PlayerUpdateReason) return
        val player = (event.updateReason as PlayerUpdateReason).player
        runTaskLater(1) {player.addToInventoryOrDrop(listOf(item.apply { amount = rest }))}
    }
    
    @TileEntityMenuClass
    inner class DeepStorageUnitMenu: GlobalTileEntityMenu() {
        
        private val openCellWindow = clickableItem(DefaultItems.WRENCH.clientsideProvider) {
            it.playClickSound()
            cellWindow.open(it)
        }
        
        private val sideConfigGui = SideConfigMenu(
            this@DeepStorageUnit, listOf(inventory to "inventory.nova.input"), ::openWindow
        )
        
        private val contentGui = ScrollGui.items()
            .setStructure(
                "| x x x x x x | #",
                "| x x x x x x | #",
                "| x x x x x x | u",
                "| x x x x x x | d"
            )
            .setContent(getDisplay())
            .build()
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - 2 d",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "3 - - - - - - 4 s")
            .addIngredient('1', inputInv, DefaultGuiItems.LIGHT_CORNER_TOP_LEFT.clientsideProvider)
            .addIngredient('d', openCellWindow)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addModifier { it.fillRectangle(0, 1, contentGui, true) }
            .build()
        
        private val cellGui = Gui.normal()
            .setStructure(
                "# # 1 - - - 2 # #",
                "# # | d d d | # #",
                "# # | d d d | # #",
                "# # | d d d | # #",
                "# # | d d d | # #",
                "b # 3 - - - 4 # #",)
            .addIngredient('b', BackItem {openWindow(it)})
            .addIngredient('d', cellInv)
            .build()
        
        private val cellWindow = Window.single()
            .setGui(cellGui)
            .setTitle(Component.translatable("menu.deep_storage.cell_inventory"))
        
        fun update() {
            inventory.updateInventory()
            updateContent()
        }
        
        fun updateContent() {
            contentGui.setContent(getDisplay())
        }
        
        private fun getDisplay(): List<ItemDisplay> {
            return getItems().entries.map { (item, value) ->
                ItemDisplay(item, value)
            }.toList()
        }
        
        inner class ItemDisplay(private val item: ItemStack, private val amount: Int): AbstractItem() {
            
            override fun getItemProvider(): ItemProvider {
                val itemBuilder = ItemBuilder(item)
                
                val name = Component.text()
                    .append(Component.text("x${amount}"))
                    .build()
                
                itemBuilder.setDisplayName(name)
                return itemBuilder
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                val itemStack = item.clone()
                itemStack.amount = itemStack.maxStackSize.coerceAtMost(amount)
                
                val cursor = player.itemOnCursor
                if (!cursor.type.isAir && clickType == ClickType.LEFT) {
                    val rest = addItemToCell(cursor)
                    if (rest != cursor.amount) menuContainer.forEachMenu(DeepStorageUnitMenu::update)
                    cursor.amount = rest
                    return
                }
                
                if (clickType == ClickType.LEFT) {
                    player.setItemOnCursor(itemStack)
                } else if (clickType == ClickType.SHIFT_LEFT) {
                    player.addToInventoryOrDrop(listOf(itemStack))
                } else if (clickType == ClickType.RIGHT) {
                    
                    if (cursor.isSimilar(itemStack)) {
                        if (cursor.amount == cursor.maxStackSize) return
                        cursor.amount = cursor.maxStackSize.coerceAtMost(cursor.amount + 1)
                        itemStack.apply { amount = 1 }
                    } else if (cursor.type.isAir) {
                        player.setItemOnCursor(itemStack.apply { amount = 1 })
                    } else return
                    
                } else if (clickType == ClickType.SHIFT_RIGHT) {
                    player.addToInventoryOrDrop(listOf(itemStack.apply { amount = 1 }))
                } else if (clickType == ClickType.MIDDLE) {
                    
                    if (player.gameMode != GameMode.CREATIVE) return
                    if (!cursor.type.isAir) return
                    player.setItemOnCursor(itemStack.apply { amount = itemStack.maxStackSize })
                    return
                    
                } else return
                
                removeItem(itemStack)
                menuContainer.forEachMenu(DeepStorageUnitMenu::update)
            }
            
        }
    }
    
    inner class DeepStorageInventory(private val virtualInventory: VirtualInventory) : NetworkedInventory {
        
        override val size: Int
            get() = virtualInventory.size
        
        fun resize() {
            virtualInventory.resize(getSize())
        }
        
        override val items: Array<ItemStack?>
            get() = virtualInventory.items
        
        override fun setItem(slot: Int, item: ItemStack?): Boolean {
            if (item != null) {
                removeItem(item)
                menuContainer.forEachMenu(DeepStorageUnitMenu::update)
            }
            return true
        }
        
        override fun addItem(item: ItemStack): Int {
            val toCell = addItemToCell(item)
            menuContainer.forEachMenu(DeepStorageUnitMenu::update)
            return toCell
        }
        
        override fun canDecrementByOne(slot: Int): Boolean {
            val itemStack = virtualInventory.getUnsafeItem(slot) ?: return false
            if (getItemAmount(itemStack) == 0) {
                virtualInventory.setItem(SELF_UPDATE_REASON, slot, null)
                return false
            }
            return true
        }
        
        override fun decrementByOne(slot: Int) {
            val item = inventory.getItem(slot)
            if (item != null) {
                val itemAmount = getItemAmount(item)
                virtualInventory.setItem(UpdateReason.SUPPRESSED, slot, item.clone()
                    .apply{ amount = maxStackSize.coerceAtMost(itemAmount - 1) })
                removeItem(item, 1)
                menuContainer.forEachMenu(DeepStorageUnitMenu::updateContent)
            }
        }
        
        override fun isFull(): Boolean {
            return false
        }
        
        fun updateInventory() {
            getItems().entries.mapIndexed { index, entry ->
                virtualInventory.setItem(SELF_UPDATE_REASON, index,
                    entry.key.apply { amount = maxStackSize.coerceAtMost(entry.value) })
            }
        }
        
    }
    
}