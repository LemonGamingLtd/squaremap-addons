import xyz.jpenilla.resourcefactory.bukkit.Permission

description = "FactionsUUID addon for squaremap"

dependencies {
    compileOnly("com.massivecraft:Factions:1.6.9.5-U0.6.33")
}

bukkitPluginYaml {
    main = "xyz.jpenilla.squaremap.addon.factionsuuid.SquaremapFactions"
    authors.add("NahuLD")

    permissions {
        register("squaremap.factionsuuid.admin") {
            description = "Allow controlling the plugin"
            default = Permission.Default.OP
        }
    }
}
