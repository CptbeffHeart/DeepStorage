package com.expectale

import com.expectale.protection.DeepStorageProtection
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.api.Nova
import xyz.xenondevs.nova.update.ProjectDistributor
import java.util.logging.Logger

lateinit var LOGGER: Logger

object DeepStorage : Addon() {
    
    override val projectDistributors = listOf(ProjectDistributor.github("CptbeffHeart/DeepStorage"))
    
    override fun init() {
        LOGGER = logger
        Nova.getNova().registerProtectionIntegration(DeepStorageProtection)
    }
    
}