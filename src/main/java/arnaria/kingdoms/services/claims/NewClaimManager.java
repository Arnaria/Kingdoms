package arnaria.kingdoms.services.claims;

import arnaria.kingdoms.Kingdoms;
import arnaria.kingdoms.interfaces.PlayerEntityInf;
import arnaria.kingdoms.services.data.KingdomsData;
import arnaria.kingdoms.services.procedures.KingdomProcedures;
import arnaria.kingdoms.util.BlueMapAPI;
import arnaria.kingdoms.util.ClaimHelpers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import eu.pb4.holograms.api.holograms.WorldHologram;
import mrnavastar.sqlib.api.DataContainer;
import mrnavastar.sqlib.api.Table;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class NewClaimManager {

    private static final Table claimData = Kingdoms.database.createTable("ClaimData");
    private static final HashMap<BlockPos, ArrayList<ChunkPos>> claims = new HashMap<>();
    private static final HashMap<BlockPos, String> owners = new HashMap<>();
    private static final ListMultimap<BlockPos, BlockPos> links = ArrayListMultimap.create();
    private static final HashMap<BlockPos, WorldHologram> holograms = new HashMap<>();

    public static ArrayList<BlockPos> getPoints(String kingdomId) {
        ArrayList<BlockPos> points = new ArrayList<>();
        owners.forEach((pos, kingdom) -> {
            if (kingdom.equals(kingdomId)) points.add(pos);
        });
        return points;
    }

    public static void init() {
        ClaimEvents.register();

        claimData.beginTransaction();
        for (DataContainer claim : claimData.getDataContainers()) {
            String kingdomId = claim.getString("KINGDOM_ID");
            BlockPos pos = claim.getBlockPos("POS");

            ArrayList<ChunkPos> chunks = new ArrayList<>();
            for (BlockPos chunk : claim.getBlockPosArray("CHUNKS")) chunks.add(new ChunkPos(chunk));
            claims.put(pos, chunks);

            WorldHologram hologram = new WorldHologram(Kingdoms.overworld, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
            holograms.put(pos, hologram);

            LiteralText claimTag = new LiteralText(kingdomId.toUpperCase());
            Formatting formatting = Formatting.byName(KingdomsData.getColor(kingdomId));
            if (formatting != null) claimTag.formatted(formatting);
            hologram.addText(claimTag);
            hologram.show();

            owners.put(pos, kingdomId);
        }
        claimData.endTransaction();
    }

    public static void addClaim(String kingdomId, BlockPos pos, boolean showCosmetics) {
        ArrayList<ChunkPos> chunks = ClaimHelpers.createChunkBox(pos, 5, true);
        ArrayList<ChunkPos> testChunks = ClaimHelpers.createChunkBox(pos, 7, false);
        claims.put(pos, chunks);
        owners.put(pos, kingdomId);

        ArrayList<ChunkPos> kingdomChunks = new ArrayList<>();
        claims.forEach((claimPos, claimChunks) -> {
            if (owners.get(claimPos).equals(kingdomId)) {
                kingdomChunks.addAll(claimChunks);

                if (isOverlapping(claimChunks, testChunks)) {
                    links.put(pos, claimPos);
                    links.put(claimPos, pos);
                }
            }
        });

        ClaimRenderer.updateRenderMap(kingdomId, kingdomChunks);
        KingdomProcedures.addToBannerCount(kingdomId, 1);
        if (KingdomsData.getStartingClaimPos(kingdomId) == null) KingdomProcedures.setStartingClaimPos(kingdomId, pos);


        if (showCosmetics) {
            WorldHologram hologram = new WorldHologram(Kingdoms.overworld, new Vec3d(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5));
            holograms.put(pos, hologram);

            LiteralText claimTag = new LiteralText(kingdomId.toUpperCase());
            Formatting formatting = Formatting.byName(KingdomsData.getColor(kingdomId));
            if (formatting != null) claimTag.formatted(formatting);
            hologram.addText(claimTag);
            hologram.show();

            BlueMapAPI.createMarker(kingdomId, chunks);
        }

       claimData.beginTransaction();
        DataContainer claimDataContainer = claimData.createDataContainer(pos.toShortString());
        claimDataContainer.put("KINGDOM_ID", kingdomId);
        claimDataContainer.put("POS", pos);

        ArrayList<BlockPos> chunkPositions = new ArrayList<>();
        chunks.iterator().forEachRemaining(chunk -> chunkPositions.add(chunk.getStartPos()));
        claimDataContainer.put("CHUNKS", chunkPositions.toArray(BlockPos[]::new));

        claimData.endTransaction();
    }

    public static void addAdminClaim(ChunkPos pos) {
        ArrayList<BlockPos> point = getPoints("ADMIN");
        if (point.isEmpty()) {
            point.add(pos.getStartPos());
            owners.put(pos.getStartPos(), "ADMIN");
        }
        BlockPos claimPos = point.get(0);

        ArrayList<ChunkPos> chunks = claims.get(claimPos);
        if (chunks == null) chunks = new ArrayList<>();
        chunks.add(pos);
        claims.put(claimPos, chunks);

        ClaimRenderer.updateRenderMap("ADMIN", chunks);

        claimData.beginTransaction();
        DataContainer claim = claimData.createDataContainer(claimPos.toShortString());
        claim.put("KINGDOM_ID", "ADMIN");
        claim.put("POS", claimPos);

        ArrayList<BlockPos> chunkPositions = new ArrayList<>();
        chunks.iterator().forEachRemaining(chunk -> chunkPositions.add(chunk.getStartPos()));
        claim.put("CHUNKS", chunkPositions.toArray(BlockPos[]::new));

        claimData.endTransaction();
    }

    public static void dropClaim(BlockPos pos) {
        String kingdomId = owners.get(pos);

        holograms.get(pos).hide();
        KingdomProcedures.removeFromBannerCount(kingdomId, 1);
        for (BlockPos claimPos : links.get(pos)) links.remove(claimPos, pos);
        BlueMapAPI.removeMarker(kingdomId, claims.remove(pos));

        owners.remove(pos);
        links.removeAll(pos);
        holograms.remove(pos);
        claimData.drop(pos.toShortString());

        ArrayList<ChunkPos> kingdomChunks = new ArrayList<>();
        claims.forEach((claimPos, claimChunks) -> {
            if (owners.get(claimPos).equals(kingdomId)) {
                kingdomChunks.addAll(claimChunks);
            }
        });
        ClaimRenderer.updateRenderMap(kingdomId, kingdomChunks);
    }

    public static void dropKingdom(String kingdomId) {
        claimData.beginTransaction();
        for (BlockPos pos : getPoints(kingdomId)) {
            dropClaim(pos);
        }
        claimData.endTransaction();
        ClaimRenderer.dropRenderMap(kingdomId);
    }

    public static void dropChunk(ChunkPos pos) {
        for (Map.Entry<BlockPos, ArrayList<ChunkPos>> claim : claims.entrySet()) {
            ArrayList<ChunkPos> chunks = claim.getValue();
            if (chunks.contains(pos)) {
                chunks.remove(pos);

                if (chunks.isEmpty()) {
                    claims.remove(pos.getStartPos());
                    owners.remove(pos.getStartPos());
                }

                String kingdomId = owners.get(claim.getKey());
                ClaimRenderer.updateRenderMap(kingdomId, chunks);
                BlueMapAPI.removeMarker(kingdomId, claims.get(claim.getKey()));
                BlueMapAPI.createMarker(kingdomId, chunks);

                claimData.beginTransaction();
                DataContainer claimDataContainer = claimData.get(claim.getKey().toShortString());
                ArrayList<BlockPos> chunkPositions = new ArrayList<>(Arrays.asList(claimDataContainer.getBlockPosArray("CHUNKS")));
                chunkPositions.remove(pos.getStartPos());
                claimDataContainer.put("CHUNKS", chunkPositions.toArray(BlockPos[]::new));
                claimData.endTransaction();
                break;
            }
        }
    }

    public static void updateColor(String kingdomId, String color) {
        holograms.forEach((pos, hologram) -> {
            if (owners.get(pos).equals(kingdomId)) {
                hologram.removeElement(0);
                hologram.addText(new LiteralText(kingdomId.toUpperCase()).formatted(Formatting.byName(color)));

                ArrayList<ChunkPos> chunks = claims.get(pos);
                BlueMapAPI.removeMarker(kingdomId, chunks);
                BlueMapAPI.createMarker(kingdomId, chunks);
            }
        });

        ClaimRenderer.updateColor(kingdomId, color);
    }

    public static void transferClaims(String kingdomId, String newKingdomId) {
        getPoints(kingdomId).forEach(pos -> owners.put(pos, newKingdomId));
        updateColor(newKingdomId, KingdomsData.getColor(newKingdomId));
    }

    public static boolean actionAllowedAt(BlockPos pos, PlayerEntity player) {
        if (pos.getY() < 0) return true;

        for (Map.Entry<BlockPos, ArrayList<ChunkPos>> claim : claims.entrySet()) {
            if (claim.getValue().contains(new ChunkPos(pos)) && !((PlayerEntityInf) player).allowedToEditIn(owners.get(claim.getKey()))) return false;
        }
        return true;
    }

    //Consider moving into valid banner pos if not used anywhere else
    private static boolean isOverlapping(ArrayList<ChunkPos> claim, ArrayList<ChunkPos> newChunks) {
        for (ChunkPos pos : newChunks) {
            if (claim.contains(pos)) return true;
        }
        return false;
    }

    public static boolean isClaimMarker(BlockPos pos) {
        return owners.containsKey(pos);
    }

    public static boolean validBannerPos(String kingdomId, BlockPos pos) {
        if (KingdomsData.getBannerCount(kingdomId) == 0) return true;

        ArrayList<ChunkPos> testChunks = ClaimHelpers.createChunkBox(pos, 7, false);
        for (ArrayList<ChunkPos> claim : claims.values()) {
            if (claim.contains(new ChunkPos(pos))) return false;
            if (kingdomId.equals(owners.get(pos)) && isOverlapping(claim, testChunks)) return true;
        }
        return false;
    }

    public static boolean claimExistsAt(ChunkPos pos) {
        for (ArrayList<ChunkPos> claim : claims.values()) {
            if (claim.contains(pos)) return true;
        }
        return false;
    }

    public static boolean canBreakClaim(BlockPos pos) {
        String kingdomId = owners.get(pos);
        BlockPos startingClaimPos = KingdomsData.getStartingClaimPos(kingdomId);
        if (pos.equals(startingClaimPos) && KingdomsData.getBannerCount(kingdomId) == 1) return true;

        for (BlockPos claimPos : links.get(pos)) {
            if (!(links.get(claimPos).size() > 1)) return false;
        }
        return true;
    }

    public static boolean canAffordBanner(String kingdomId) {
        int bannersAllowed = (int) Math.floor((float) KingdomsData.getXp(kingdomId) / 1000) + 1;
        return bannersAllowed > KingdomsData.getBannerCount(kingdomId);
    }
}