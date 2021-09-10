/*
    Copyright(c) 2019 Risto Lahtela (AuroraLS3)

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.djrapitops.extension;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.data.PlayerData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DataExtension for the GriefDefender plugin.
 * <p>
 * Port of the GriefPreventionSponge extension by AuroraLS3
 *
 * @author Vankka
 */
@PluginInfo(name = "GriefDefender", iconName = "shield-alt", iconFamily = Family.SOLID, color = Color.BLUE_GREY)
@TabInfo(
        tab = "Claims",
        iconName = "map-marker",
        elementOrder = {ElementOrder.TABLE}
)
public class GriefDefenderExtension implements DataExtension {


    public GriefDefenderExtension() {
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_LEAVE
        };
    }

    private Core getApi() {
        try {
            return GriefDefender.getCore();
        } catch (IllegalStateException griefDefenderNotEnabled) {
            throw new NotReadyException();
        }
    }

    private List<Claim> getClaimsOf(UUID playerUUID) {
        return getApi().getAllPlayerClaims(playerUUID);
    }

    private PlayerData getPlayerData(UUID playerUUID) {
        try {
            return Optional.ofNullable(getApi().getUser(playerUUID))
                    .map(User::getPlayerData)
                    .orElseThrow(NotReadyException::new);
        } catch (NullPointerException e) {
            throw new NotReadyException();
        }
    }

    @NumberProvider(
            text = "Claims",
            description = "How many claims the player has",
            iconName = "map-marker",
            iconColor = Color.BLUE_GREY
    )
    public long claimCount(UUID playerUUID) {
        return getClaimsOf(playerUUID).size();
    }

    @StringProvider(
            text = "Claim Type",
            description = "Basic + Town + SUBDIVISION + Admin",
            iconName = "sign",
            iconColor = Color.BLUE,
            showInPlayerTable = true
    )
    public String claimType(UUID playerUUID) {
        int basic = 0, town = 0, sub = 0, admin = 0;
        List<Claim> claimsList = getClaimsOf(playerUUID);
        for (Claim claim : claimsList) {
            if (claim.isBasicClaim()) {
                basic++;
            } else if (claim.isTown()) {
                town++;
            } else if (claim.isSubdivision()) {
                sub++;
            } else {admin++;}
        }
        return "basic: " + basic + ",town: " + town + ",sub claims: " + sub + ",admin: " + admin;
    }

    @NumberProvider(
            text = "Claimed Area",
            description = "How large area the player has claimed",
            iconName = "map",
            iconColor = Color.BLUE_GREY,
            iconFamily = Family.REGULAR,
            showInPlayerTable = true
    )
    public long claimedArea(UUID playerUUID) {
        return getClaimsOf(playerUUID).stream()
                .mapToLong(Claim::getArea)
                .sum();
    }

    @NumberProvider(
            text = "Initial Blocks",
            description = "Initial Blocks that player first play on your server",
            iconName = "cube",
            iconColor = Color.BLUE_GREY,
            iconFamily = Family.REGULAR,
            showInPlayerTable = true
    )
    public long initialBlocks(UUID playerUUID) {
        return getPlayerData(playerUUID).getInitialClaimBlocks();
    }

    @NumberProvider(
            text = "Bonus Blocks",
            description = "Bonus Blocks that player get reward",
            iconName = "medal",
            iconColor = Color.BLUE_GREY,
            iconFamily = Family.REGULAR,
            showInPlayerTable = true
    )
    public long bonusBlocks(UUID playerUUID) {
        return getPlayerData(playerUUID).getBonusClaimBlocks();
    }

    @NumberProvider(
            text = "Accrued Blocks",
            description = "Accrued Blocks that player accrue during the game time",
            iconName = "cubes",
            iconColor = Color.BLUE_GREY,
            iconFamily = Family.REGULAR,
            showInPlayerTable = true
    )
    public long accruedBlocks(UUID playerUUID) {
        return getPlayerData(playerUUID).getAccruedClaimBlocks();
    }

    @NumberProvider(
            text = "Remaining Blocks",
            description = "Remaining Blocks that player has at present",
            iconName = "square",
            iconColor = Color.BLUE_GREY,
            iconFamily = Family.REGULAR,
            showInPlayerTable = true
    )
    public long remainingBlocks(UUID playerUUID) {
        return getPlayerData(playerUUID).getRemainingClaimBlocks();
    }

    @TableProvider(tableColor = Color.BLUE_GREY)
    @Tab("Claims")
    public Table claimTable(UUID playerUUID) {
        Table.Factory table = Table.builder()
                .columnOne("Name", Icon.called("address-book").build())
                .columnTwo("Type", Icon.called("sign").build())
                .columnThree("Location", Icon.called("map-marker").build())
                .columnFour("Area", Icon.called("map").of(Family.REGULAR).build());

        getClaimsOf(playerUUID).stream()
                .sorted((one, two) -> Integer.compare(two.getArea(), one.getArea()))
                .forEach(
                        claim -> table.addRow(claim.getName(), formatType(claim), formatLocation(claim.getGreaterBoundaryCorner()), claim.getArea())
                );

        return table.build();
    }

    private String formatLocation(Vector3i greaterBoundaryCorner) {
        return "x: " + greaterBoundaryCorner.getX() + "y: " + greaterBoundaryCorner.getY() + " z: " + greaterBoundaryCorner.getZ();
    }

    private String formatType(Claim claim) {
        return claim.getType() + "(" + (claim.isCuboid() ? "Cuboid" : "Square") + ")";
    }

}