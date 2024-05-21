package com.expectale.item

import com.expectale.registry.Items
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.addToInventoryOrDrop

object EmptySecurityCard: ItemBehavior {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        if (!player.isSneaking) return
        itemStack.subtract()
        val novaCard = Items.SECURITY_CARD
        val card = novaCard.createItemStack()
        novaCard.getBehaviorOrNull<SecurityCard>()?.apply { setOwner(card, player) }
        player.addToInventoryOrDrop(listOf(card))
    }
    
    override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
        itemData.addLore(Component.text()
            .append(Component.translatable("item.deep_storage.empty_security_card.lore").color(NamedTextColor.GRAY))
            .build())
    }
}