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
    public Color warzoneStrokeColor = Color.RED;
    public Color warzoneFillColor = Color.RED;
    public double fillOpacity = 0.2D;
    public boolean colorPerFaction = true;
    public Color raidClaimStrokeColor = new Color(0xFF8800);
    public int raidClaimStrokeWeight = 3;
    public String stringsPublic = "Public";
    public int zIndex = 99;
    public int layerPriority = 99;
    public String claimTooltip = "Id: <span style=\"font-weight:bold;\">{id}</span><br/>" +
        "Claim Owners: <span style=\"font-weight:bold;\">{owners}</span><br/>" +
        "Members: <span style=\"font-weight:bold;\">{managers}</span><br/>" +
        "Created: <span style=\"font-weight:bold;\">{created}</span><br/>";
    public String raidClaimTooltip = "<span style=\"font-weight:bold;color:#ff8800;\">Raid claim</span> (expires in {expires})<br/>";
    public String hoverTooltip = "<span style=\"font-weight:bold;\">{tag}</span>";
    public String raidClaimHoverTooltip = "<span style=\"font-weight:bold;\">{tag}</span> - raid claim, expires in {expires}";
    public boolean showChunks = false;
    public boolean webSearch = true;

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
        this.colorPerFaction = this.getBoolean("settings.style.color-per-faction", this.colorPerFaction);
        this.raidClaimStrokeColor = this.getColor("settings.style.raid-claim.stroke.color", this.raidClaimStrokeColor);
        this.raidClaimStrokeWeight = this.getInt("settings.style.raid-claim.stroke.weight", this.raidClaimStrokeWeight);
        this.stringsPublic = this.getString("settings.strings.public", this.stringsPublic);
        this.claimTooltip = this.getString("settings.region.tooltip.regular-claim", this.claimTooltip);
        this.raidClaimTooltip = this.getString("settings.region.tooltip.raid-claim", this.raidClaimTooltip);
        this.hoverTooltip = this.getString("settings.region.tooltip.hover", this.hoverTooltip);
        this.raidClaimHoverTooltip = this.getString("settings.region.tooltip.hover-raid-claim", this.raidClaimHoverTooltip);
        this.zIndex = this.getInt("settings.control.z-index", this.zIndex);
        this.layerPriority = this.getInt("settings.control.layer-priority", this.layerPriority);
        this.warzoneFillColor = this.getColor("settings.style.warzone-fill-color", this.warzoneFillColor);
        this.warzoneStrokeColor = this.getColor("settings.style.warzone-stroke-color", this.warzoneStrokeColor);
        this.showChunks = this.getBoolean("settings.claim.show-chunks", this.showChunks);
        this.webSearch = this.getBoolean("settings.web.faction-search", this.webSearch);
    }

    public FactionsConfig(Plugin plugin) {
        super(FactionsConfig.class, plugin);
    }
}
