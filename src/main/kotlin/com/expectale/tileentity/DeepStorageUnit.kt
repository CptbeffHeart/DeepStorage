package com.expectale.tileentity

import com.expectale.block.SecurityCardHolder
import com.expectale.block.StorageCellHolder
import com.expectale.item.StorageCell
import com.expectale.registry.Blocks.DEEP_STORAGE_UNIT
import com.expectale.registry.GuiMaterials
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.AbstractScrollGui
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.structure.Structure
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.addIngredient
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigMenu
import xyz.xenondevs.nova.ui.item.BackItem
import xyz.xenondevs.nova.ui.item.clickableItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.VoidingVirtualInventory
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.runTaskLater

private val PREVENT_INFINITE_STORAGE by DEEP_STORAGE_UNIT.config.entry<Boolean>("prevent-infinite-storage")

class DeepStorageUnit(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState), StorageCellHolder, SecurityCardHolder {
    
    private val cellInv = retrieveData<VirtualInventory>("cell") {
        VirtualInventory(IntArray(12) { 1 }) }.apply {
        setPreUpdateHandler(::handleCellUpdate)
        setPostUpdateHandler(::handlePostCellUpdate) }
    private val cardInv = retrieveData<VirtualInventory>("card") {
        VirtualInventory(IntArray(14) { 1 }) }.apply {
        setPreUpdateHandler(::handleCardUpdate) }
    private val inputInv = VoidingVirtualInventory(1).apply { setPreUpdateHandler(::handlePreInput) }
    private val inventory = DeepStorageInventory(VirtualInventory(getSize()))
    
    override val itemHolder = NovaItemHolder(
        this,
        uuid to (inventory to NetworkConnectionType.BUFFER)
    ) { createSideConfig(NetworkConnectionType.BUFFER, BlockSide.FRONT) }
    
    private var sortMode by storedValue("sortMode") { SortMode.HIGHER_AMOUNT }
    
    init {
        inventory.updateInventory()
    }
    
    override var whiteList: Boolean = false
    
    override val cellInventory: VirtualInventory
        get() = cellInv
    
    override val cardInventory: VirtualInventory
        get() = cardInv
    
    override fun callUpdateCell() {
        inventory.resize()
        menuContainer.forEachMenu(DeepStorageUnitMenu::update)
    }
    
    override fun canInputCard(player: Player): Boolean {
        return player.isOp ||
            player.hasPermission("deep_storage.security.bypass") ||
            player.uniqueId == ownerUUID
    }
    
    fun hasAccess(player: Player): Boolean {
        if (canInputCard(player)) return true
        return hasCardAccess(player)
    }
    
    private fun handlePreInput(event: ItemPreUpdateEvent) {
        if (event.updateReason == SELF_UPDATE_REASON || event.newItem == null) return
        val item = event.newItem!!
        
        if (PREVENT_INFINITE_STORAGE) {
            val storageCell = item.novaItem?.getBehaviorOrNull<StorageCell>()
            if (storageCell != null && !storageCell.isEmpty(item)) {
                event.isCancelled = true
                return
            }
        }
        
        val rest = addItemToCell(item)
        menuContainer.forEachMenu(DeepStorageUnitMenu::update)
        
        if (rest == 0) return
        if (event.updateReason !is PlayerUpdateReason) return
        val player = (event.updateReason as PlayerUpdateReason).player
        runTaskLater(1) {player.addToInventoryOrDrop(listOf(item.apply { amount = rest }))}
    }
    
    override fun saveData() {
        super.saveData()
        storeData("cell", cellInv, true)
        storeData("card", cardInv, true)
    }
    
    enum class SortMode {
        ALPHABETICAL, HIGHER_AMOUNT
    }
    
    @TileEntityMenuClass
    inner class DeepStorageUnitMenu: GlobalTileEntityMenu() {
        
        private val openCellWindow = clickableItem(GuiMaterials.STORAGE_CELL.clientsideProvider) {
            it.playClickSound()
            cellWindow.open(it)
        }
        
        private val openCardWindow = clickableItem(GuiMaterials.SECURITY_CARD.clientsideProvider) {
            if (canInputCard(it)) {
                it.playClickSound()
                cardWindow.open(it)
            }
        }
        
        private val sideConfigGui = SideConfigMenu(
            this@DeepStorageUnit, listOf(inventory to "inventory.nova.input"), ::openWindow
        )
        
        private val customScroll = CustomScrollGui().apply { setContent(getDisplay()) }
        
        override val gui = Gui.normal()
            .setStructure(
                "d u # # # # c s #",
                "- - - - - - - - -",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x")
            .addIngredient('-', inputInv, DefaultGuiItems.LIGHT_HORIZONTAL_LINE.clientsideProvider)
            .addIngredient('d', openCellWindow)
            .addIngredient('s', OpenSideConfigItem(sideConfigGui))
            .addIngredient('u', SortButton())
            .addIngredient('c', openCardWindow)
            .addModifier { it.fillRectangle(0, 2, customScroll, true) }
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
            .addIngredient('d', cellInv, GuiMaterials.STORAGE_CELL_PLACEHOLDER)
            .build()
        
        private val cellWindow = Window.single()
            .setGui(cellGui)
            .setTitle(Component.translatable("menu.deep_storage.storage_cell_inventory"))
        
        private val cardGui = Gui.normal()
            .setStructure(
                "# # # # # # # # w",
                "1 - - - - - - - 2",
                "| x x x x x x x |",
                "| x x x x x x x |",
                "3 - - - - - - - 4",
                "b # # # # # # # #")
            .addIngredient('w', WhitelistButton())
            .addIngredient('b', BackItem {openWindow(it)})
            .addIngredient('x', cardInv, GuiMaterials.SECURITY_CARD_PLACEHOLDER)
            .build()
        
        private val cardWindow = Window.single()
            .setGui(cardGui)
            .setTitle(Component.translatable("menu.deep_storage.security_card_inventory"))
        
        fun update() {
            inventory.updateInventory()
            updateContent()
        }
        
        fun updateContent() {
            customScroll.setContent(getDisplay())
        }
        
        private fun getDisplay(): List<ItemDisplay> {
            val list = getItems().entries
                .map { (item, value) -> ItemDisplay(item, value) }
            
            return if (sortMode == SortMode.HIGHER_AMOUNT) list.sortedByDescending { it.amount }
            else list.sortedBy { it.name() }
        }
        
        inner class WhitelistButton: AbstractItem() {
            override fun getItemProvider(): ItemProvider {
                return if (whiteList) GuiMaterials.WHITELIST_ON.clientsideProvider
                else GuiMaterials.WHITELIST_OFF.clientsideProvider
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                player.playClickSound()
                whiteList = !whiteList
                notifyWindows()
            }
            
        }
        
        inner class SortButton: AbstractItem() {
            override fun getItemProvider(): ItemProvider {
                return if (sortMode == SortMode.ALPHABETICAL) GuiMaterials.ALPHABETICAL_SORT.clientsideProvider
                else GuiMaterials.STACK_SORT.clientsideProvider
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                player.playClickSound()
                sortMode = if (sortMode == SortMode.ALPHABETICAL) SortMode.HIGHER_AMOUNT else SortMode.ALPHABETICAL
                notifyWindows()
                updateContent()
            }
            
        }
        
        inner class ItemDisplay(val item: ItemStack, val amount: Int): AbstractItem() {
            
            fun name(): String {
                return ItemUtils.getName(item).toPlainText()
            }
            
            override fun getItemProvider(): ItemProvider {
                val itemBuilder = ItemBuilder(item)
                
                val displayName = ItemUtils.getName(item)
                    .append(Component.text(" x${amount}").color(NamedTextColor.GREEN))
                
                itemBuilder.setDisplayName(displayName)
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
        
        override fun openWindow(player: Player) {
            if (!hasAccess(player) && whiteList) {
                player.sendMessage(
                    Component.text()
                    .append(
                        Component.translatable("message.deep_storage.not_whitelisted")
                            .color(NamedTextColor.DARK_RED)
                    ).build()
                )
                return
            }
            super.openWindow(player)
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
    
    inner class CustomScrollGui: AbstractScrollGui<Item>(9, 4, true,
        Structure(
            "# x x x x x x x #",
            "# x x x x x x x u",
            "# x x x x x x x d",
            "# x x x x x x x #",
        )) {
        
        override fun bake() {
            val elements = ArrayList<SlotElement>(content.size)
            for (item in content) {
                elements.add(SlotElement.ItemSlotElement(item))
            }
            this.elements = elements
            update()
        }
        
        override fun handleClick(slotNumber: Int, player: Player?, clickType: ClickType?, event: InventoryClickEvent) {
            if (slotElements[slotNumber] == null) {
                event.isCancelled = true
                val cursor = player?.itemOnCursor ?: return
                
                if (!cursor.type.isAir && clickType == ClickType.LEFT) {
                    val rest = addItemToCell(cursor)
                    if (rest != cursor.amount) menuContainer.forEachMenu(DeepStorageUnitMenu::update)
                    cursor.amount = rest
                }
                
                return
            }
            
            super.handleClick(slotNumber, player, clickType, event)
        }
        
    }
    
}