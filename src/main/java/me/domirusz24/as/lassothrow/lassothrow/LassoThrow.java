package me.domirusz24.as.lassothrow.lassothrow;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.waterbending.Torrent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;

public final class LassoThrow extends ChiAbility implements AddonAbility {

    private static long cooldown = 5000;
    private int speed = 1;
    private long bindingTime = 40;
    private long holdTime = 120;
    private int pullDistance = 5;
    private int breakDistance = 8;
    private double firstPull = 0.5;
    private int range = 8;
    private int particleAmount = 20;
    private double[][] circle;

    private LassoStages stage = LassoStages.InFlight;

    public LassoThrow(Player player) {
        super(player);
        if (bPlayer.isOnCooldown(this)) return;
        LassoThrow oldLasso = getAbility(player, LassoThrow.class);
        if (oldLasso != null) {
            return;
        }
        setFields();
        start();
    }

    public void setFields() {
        origin = player.getEyeLocation().clone();
        direction = origin.getDirection().clone().multiply(speed);
        ropeEnd = origin.clone();
        speed = ConfigManager.getConfig().getInt("ExtraAbilities.DomiRusz24.Chi.LassoThrow.Speed");
        bindingTime = (int) (ConfigManager.getConfig().getLong("ExtraAbilities.DomiRusz24.Chi.LassoThrow.BindingTime") / 50);
        pullDistance = ConfigManager.getConfig().getInt("ExtraAbilities.DomiRusz24.Chi.LassoThrow.PullDistance");
        breakDistance = ConfigManager.getConfig().getInt("ExtraAbilities.DomiRusz24.Chi.LassoThrow.BreakDistance");
        range = ConfigManager.getConfig().getInt("ExtraAbilities.DomiRusz24.Chi.LassoThrow.Range");
        particleAmount = ConfigManager.getConfig().getInt("ExtraAbilities.DomiRusz24.Chi.LassoThrow.ParticleAmount");
        firstPull = ConfigManager.getConfig().getDouble("ExtraAbilities.DomiRusz24.Chi.LassoThrow.FirstPullStrength");
        holdTime = (int) (ConfigManager.getConfig().getLong("ExtraAbilities.DomiRusz24.Chi.LassoThrow.HoldTime") / 50);
        double circleIncrement = (2 * Math.PI) / particleAmount;
        circle = new double[particleAmount][2];
        for (int i = 0; i < particleAmount; i++) {
            double angle = circleIncrement * i;
            double x = Math.cos(angle) * 1;
            double z = Math.sin(angle) * 1;
            circle[i][0] = x;
            circle[i][1] = z;
        }
    }

    // InFlight

    private Location origin;
    private Vector direction;
    private Location ropeEnd;
    private HashSet<Location> particles = new HashSet<>();

    // -- Caught --

    // Binding

    private int bindingTicks = 0;

    private LivingEntity target;
    private Player p;
    private int heldTime = 0;
    private double distance;

    @Override
    public void remove() {
        bPlayer.addCooldown(this);
        super.remove();
    }

    public void displayCircle() {
        distance = player.getLocation().distance(target.getLocation());
        int last = (int) (particleAmount * ((float) bindingTicks / bindingTime));
        double progress = 1 - ((double) heldTime / ((double) holdTime));
        for (int a = 0; a < last; a++) {
            double x = circle[a][0] * 0.5 * progress + circle[a][0] * 0.5;
            double z =  circle[a][1] * 0.5 * progress + circle[a][1] * 0.5;
            Location point = new Location(origin.getWorld(), x + target.getLocation().getX(), target.getLocation().getY() + 1, z + target.getLocation().getZ());
            if (a == last - 1) {
                origin.getWorld().spawnParticle(Particle.CRIT_MAGIC, point, 0);
            } else if (stage.equals(LassoStages.Binding)) {
                origin.getWorld().spawnParticle(Particle.CRIT, point, 0);
            } else {
                origin.getWorld().spawnParticle(Particle.CRIT_MAGIC, point, 0);
            }
        }
        Vector dir = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        if (stage.equals(LassoStages.Binding)) {
            for (double d = 0; d <= distance; d += 0.5) {
                origin.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0).add(dir.clone().multiply(d)), 0);
            }
        } else {
            for (double d = 0; d <= distance; d += 0.5) {
                origin.getWorld().spawnParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 1, 0).add(dir.clone().multiply(d)), 0);
            }
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || bPlayer.isChiBlocked() || bPlayer.isParalyzed()) {
            remove();
            return;
        }
        if (target != null) {
            if (target.isDead()) {
                remove();
                return;
            }
            if (p != null && !p.isOnline()) {
                remove();
                return;
            }
        }
        switch (stage) {
            case InFlight:
                ropeEnd.add(direction);
                if (ropeEnd.distance(origin) > range) {
                    remove();
                    return;
                }
                particles.add(ropeEnd.clone());
                for (Location l : particles) {
                    l.getWorld().spawnParticle(Particle.CRIT, l, 0);
                    l.getWorld().spawnParticle(Particle.CRIT, l.clone().add(direction.clone().multiply(0.5)), 0);
                }
                if (GeneralMethods.isSolid(ropeEnd.getBlock())) {
                    remove();
                    particles.clear();
                    break;
                }
                for (Entity en : GeneralMethods.getEntitiesAroundPoint(ropeEnd, 2.0)) {
                    if (en.equals(player)) continue;
                    if (en instanceof LivingEntity) {
                        target = (LivingEntity) en;
                        if (target.getType().equals(EntityType.PLAYER)) {
                            p = (Player) en;
                        }
                        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_WOOD_BREAK, 1, 1);
                        target.setVelocity(player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(firstPull));
                        stage = LassoStages.Binding;
                        particles.clear();
                        break;
                    }
                }
                break;
            case Binding:
                bindingTicks++;
                displayCircle();
                if (distance >= pullDistance) {
                    bindingTicks-= 2;
                    if (bindingTicks <= 0) {
                        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1, 1);
                        remove();
                        return;
                    }
                }
                if (bindingTicks >= bindingTime) {
                    player.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1);
                    stage = LassoStages.Caught;
                    break;
                }
                break;
            case Caught:
                heldTime++;
                displayCircle();
                if (heldTime >= holdTime) {
                    remove();
                    return;
                }
                if (distance > pullDistance) {
                    if (distance > breakDistance) {
                        remove();
                        return;
                    }
                    target.setVelocity(player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply((distance - pullDistance) * 1));
                }
                break;
            case Pull:


                break;
            case Charge:


                break;
        }
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "LassoThrow";
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public void load() {
        ProjectKorra.plugin.getLogger().info("Ability LassoThrow by DomiRusz24 has been enabled!");
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new LassoThrowListener(), ProjectKorra.plugin);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.Cooldown", 5000L);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.Speed", 1);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.BindingTime", 2000L);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.PullDistance", 5);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.BreakDistance", 8);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.HoldTime", 5000L);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.FirstPullStrength", 0.6D);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.Range", 8);
        ConfigManager.getConfig().addDefault("ExtraAbilities.DomiRusz24.Chi.LassoThrow.ParticleAmount", 20);
        cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.DomiRusz24.Chi.LassoThrow.Cooldown");
    }

    @Override
    public void stop() {
        ProjectKorra.plugin.getLogger().info("Ability LassoThrow by DomiRusz24 has been disabled!");
        super.remove();
    }

    @Override
    public String getAuthor() {
        return "DomiRusz24";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    public enum LassoStages {
        InFlight, Binding, Caught, Pull, Charge;
    }
}
