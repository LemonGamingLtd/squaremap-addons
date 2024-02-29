package xyz.jpenilla.squaremap.addon.factionsuuid.config;

import java.awt.Color;

import org.bukkit.plugin.Plugin;
import xyz.jpenilla.squaremap.addon.common.config.Config;
import xyz.jpenilla.squaremap.addon.common.config.WorldConfig;

public final class FactionsConfig extends Config<FactionsConfig, WorldConfig> {
    public String controlLabel = "Factions";
    public boolean controlShow = true;
    public boolean controlHide = false;
    public int updateInterval = 300;
    public Color strokeColor = Color.GREEN;
    public int strokeWeight = 1;
    public double strokeOpacity = 1.0D;
    public Color fillColor = Color.GREEN;
    public double fillOpacity = 0.2D;
    public String stringsPublic = "Public";
    public int zIndex = 99;
    public int layerPriority = 99;
    public String claimTooltip = "Id: <span style=\"font-weight:bold;\">{id}</span><br/>" +
        "Claim Owners: <span style=\"font-weight:bold;\">{owners}</span><br/>" +
        "Members: <span style=\"font-weight:bold;\">{managers}</span><br/>" +
        "Created: <span style=\"font-weight:bold;\">{created}</span><br/>";
    private void init() {
        this.controlLabel = this.getString("settings.control.label", this.controlLabel);
        this.controlShow = this.getBoolean("settings.control.show", this.controlShow);
        this.controlHide = this.getBoolean("settings.control.hide-by-default", this.controlHide);
        this.updateInterval = this.getInt("settings.update-interval", this.updateInterval);
        this.strokeColor = this.getColor("settings.style.stroke.color", this.strokeColor);
        this.strokeWeight = this.getInt("settings.style.stroke.weight", this.strokeWeight);
        this.strokeOpacity = this.getDouble("settings.style.stroke.opacity", this.strokeOpacity);
        this.fillColor = this.getColor("settings.style.fill.color", this.fillColor);
        this.fillOpacity = this.getDouble("settings.style.fill.opacity", this.fillOpacity);
        this.stringsPublic = this.getString("settings.strings.public", this.stringsPublic);
        this.claimTooltip = this.getString("settings.region.tooltip.regular-claim", this.claimTooltip);
        this.zIndex = this.getInt("settings.control.z-index", this.zIndex);
        this.layerPriority = this.getInt("settings.control.layer-priority", this.layerPriority);
    }

    public FactionsConfig(Plugin plugin) {
        super(FactionsConfig.class, plugin);
    }
}
