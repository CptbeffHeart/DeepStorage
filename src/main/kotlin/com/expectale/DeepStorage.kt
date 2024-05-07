package com.expectale

import xyz.xenondevs.nova.addon.Addon
import java.util.logging.Logger

lateinit var LOGGER: Logger

object DeepStorage : Addon() {
    
    override fun init() {
        LOGGER = logger
    }
    
}