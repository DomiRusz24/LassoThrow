package me.domirusz24.as.lassothrow.lassothrow;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;

public class LassoThrowListener implements Listener {

    @EventHandler
    public void onHit(PlayerAnimationEvent event) {
        if (event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
            if (event.isCancelled() || bPlayer == null) {
                return;
            }

            if (bPlayer.getBoundAbilityName().equalsIgnoreCase(null)) {
                return;
            }

            if (bPlayer.getBoundAbilityName().equalsIgnoreCase("LassoThrow")) {
                new LassoThrow(bPlayer.getPlayer());
            }
        }
    }
}
