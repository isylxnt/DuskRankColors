package dev.dusk.rankcolors.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

import java.util.UUID;

public final class LuckPermsHook implements RankProvider {
    private final LuckPerms luckPerms;

    public LuckPermsHook(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @Override
    public String primaryGroup(UUID playerId) {
        User user = luckPerms.getUserManager().getUser(playerId);
        return user == null ? null : user.getPrimaryGroup();
    }
}
