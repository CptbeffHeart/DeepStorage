package com.expectale.item

import com.expectale.DeepStorage
import com.expectale.registry.Items
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData
import java.util.*

interface SecurityCard {
    
    fun setOwner(card: ItemStack, player: Player)
    
    fun getOwner(card: ItemStack): UUID?
    
    fun clear(card: ItemStack)
    
    fun isOwner(card: ItemStack, player: Player): Boolean
    
    companion object : ItemBehaviorFactory<Default> {
        
        override fun create(item: NovaItem): Default {
            return Default()
        }
        
    }
    
    class Default: ItemBehavior, SecurityCard {
        
        override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
            if (!player.isSneaking) return
            itemStack.subtract()
            player.addToInventoryOrDrop(listOf(Items.EMPTY_SECURITY_CARD.createItemStack()))
        }
        
        override fun setOwner(card: ItemStack, player: Player) {
            val data = getData(card)
            if (data != null) return
            setData(card, player.uniqueId)
        }
        
        override fun getOwner(card: ItemStack): UUID? {
            return getData(card)
        }
        
        override fun clear(card: ItemStack) {
            setData(card, null)
        }
        
        override fun isOwner(card: ItemStack, player: Player): Boolean {
            return player.uniqueId == getOwner(card)
        }
        
        private fun getData(card: ItemStack): UUID? {
            return card.retrieveData<UUID>(DeepStorage, "owner")
        }
        
        private fun getData(data: NamespacedCompound): UUID? {
            return data[DeepStorage, "owner"]
        }
        
        private fun setData(card: ItemStack, uuid: UUID?) {
            card.storeData(DeepStorage, "owner", uuid)
        }
        
        override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
            val uuid = getData(data)
            val name = if (uuid != null) Bukkit.getOfflinePlayer(uuid).name ?: "" else ""
            itemData.name = (Component.text()
                .append(Component.translatable(
                    "item.deep_storage.security_card",
                    Component.text(name).color(NamedTextColor.BLUE)))
                .build())
            itemData.addLore(Component.text()
                .append(Component.translatable("item.deep_storage.security_card.lore").color(NamedTextColor.GRAY))
                .build())
        }
    }
    
}