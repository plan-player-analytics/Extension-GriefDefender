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
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.djrapitops.extension;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.data.PlayerData;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DataExtension for the GriefDefender plugin.
 *
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

    private final Core api;

    public GriefDefenderExtension() {
        api = GriefDefender.getCore();
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_LEAVE
        };
    }

    private List<Claim> getClaimsOf(UUID playerUUID) {
        return api.getAllPlayerClaims(playerUUID);
    }
    private PlayerData getPlayerData(UUID playerUUID) {
        return Objects.requireNonNull(api.getUser(playerUUID)).getPlayerData();
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
            iconName = "map-marker",
            iconColor = Color.BLUE,
            showInPlayerTable = true
    )
    public String claimType(UUID playerUUID) {
        if (getClaimsOf(playerUUID) != null) {
            getClaimsOf(playerUUID)
                    .forEach(this::countType);
        }
        return b + "+" + t + "+" + s + "+" + a ;
    }

    int b=0, t=0, s=0, a=0;
    public void countType(Claim claim) {
        if (claim.isBasicClaim()) {
            b++;
        } else if(claim.isTown()) {
            t++;
        } else if(claim.isSubdivision()) {
            s++;
        } else a++;
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

    @StringProvider(
            text = "Claim Block",
            description = "If Eco is being used, the Blocks player can buy will be showed,\n " +
                    "or will show like [initial + Bonus + Accrued + Reaming]",
            iconName = "map",
            iconColor = Color.BLUE_GREY,
            iconFamily = Family.REGULAR,
            showInPlayerTable = true
    )
    public String claimBlock(UUID playerUUID) {
        if (api.isEconomyModeEnabled()) {
            return "Remaining Blocks: " + getPlayerData(playerUUID).getRemainingClaimBlocks();
        }
        return getPlayerData(playerUUID).getInitialClaimBlocks() + "+" +
                getPlayerData(playerUUID).getBonusClaimBlocks() + "+" +
                getPlayerData(playerUUID).getAccruedClaimBlocks() + "+" +
                getPlayerData(playerUUID).getRemainingClaimBlocks();
    }

    @TableProvider(tableColor = Color.BLUE_GREY)
    @Tab("Claims")
    public Table claimTable(UUID playerUUID) {
        Table.Factory table = Table.builder()
                .columnOne("Name", Icon.called("map-marker").build())
                .columnTwo("Type", Icon.called("map-marker").build())
                .columnThree("Pos", Icon.called("map-marker").build())
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