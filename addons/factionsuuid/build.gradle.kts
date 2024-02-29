import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

description = "FactionsUUID addon for squaremap"

dependencies {
    compileOnly("com.massivecraft:Factions:1.6.9.5-U0.6.33")
}

bukkit {
    main = "xyz.jpenilla.squaremap.addon.factionsuuid.SquaremapFactions"
    addAuthor("NahuLD")

    permissions {
        register("squaremap.factionsuuid.admin") {
            description = "Allow controlling the plugin"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}
