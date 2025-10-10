package com.lunar_prototype.aM_Life;

import me.deecaad.weaponmechanics.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ScavEntity {

    private final Pillager entity;
    private final MLModel model;
    private Player currentTarget;
    private long lastSeenTime = 0;
    private final long TARGET_TIMEOUT = 5000;
    private List<Location> patrolPoints = new ArrayList<>();
    private Location currentlocationTarget = null;
    private int patrolIndex = -1;
    private final JavaPlugin plugin;
    private final String weaponName;
    private static final long REACTION_DELAY_MS = 300;
    private long targetAcquiredTime = 0;
    private Location defenceLoc;
    private long lastSoundTime = 0;
    private static final long SOUND_COOLDOWN_MS = 6000;

    private int ammo = 10;
    private int ticks = 0;

    public ScavEntity(Pillager entity,JavaPlugin plugin, World world,String weaponName,Location defenceLoc) {
        this.entity = entity;
        this.model = new MLModel();
        this.plugin = plugin;
        this.weaponName = weaponName;
        equipWeapon();
        loadPatrolPoints(world);
    }

    public Pillager getEntity() {
        return entity;
    }

    public UUID getUUID() {
        return entity.getUniqueId();
    }

    public boolean isDead() {
        return entity.isDead() || !entity.isValid();
    }

    private void equipWeapon() {
        ItemStack weaponItem = WeaponMechanicsAPI.generateWeapon(weaponName);
        if (weaponItem != null) {
            entity.getEquipment().setItemInMainHand(weaponItem);
        }
    }

    public void start() {

        Player target = getTarget();
        if (target != null) {
            entity.setTarget(target); // または pathfinding で移動
        } else {
            entity.setTarget(null);
        }
        if (currentTarget == null && currentlocationTarget != null){
            entity.getPathfinder().moveTo(currentlocationTarget);
        }
        if (currentTarget == null && (currentlocationTarget == null ||  entity.getLocation().distance(currentlocationTarget) < 5)) {
            if (defenceLoc != null) {
                Location newTarget = null;
                int tries = 0;

                while (tries < 20) {
                    double dx = (Math.random() - 0.5) * 2 * 30; // -50〜+50
                    double dz = (Math.random() - 0.5) * 2 * 30;

                    int x = defenceLoc.getBlockX() + (int) dx;
                    int z = defenceLoc.getBlockZ() + (int) dz;
                    World world = defenceLoc.getWorld();

                    int y = world.getHighestBlockYAt(x, z);
                    Location candidate = new Location(world, x + 0.5, y, z + 0.5);

                    // 移動可能な地面（落下しすぎない・空中でない・水でないなど）の確認
                    Material ground = world.getBlockAt(x, y - 1, z).getType();
                    Material above = world.getBlockAt(x, y, z).getType();

                    if (ground.isSolid() && above.isAir()) {
                        newTarget = candidate;
                        break;
                    }
                    tries++;
                }

                if (newTarget != null) {
                    currentlocationTarget = newTarget;
                }
            }
        }
        if (currentTarget == null && (currentlocationTarget == null || entity.getLocation().distance(currentlocationTarget) < 5)) {
            int attempts = 0;
            boolean moved = false;

            while (attempts < 10 && !moved) {
                Location potential = patrolPoints.get(new Random().nextInt(patrolPoints.size()));

                // 必要ならここで isValidTarget(potential) のようなチェックも追加
                moved = entity.getPathfinder().moveTo(potential);

                if (moved) {
                    currentlocationTarget = potential;
                    // Bukkit.broadcastMessage("新しい巡回地点へ移動開始: " + potential);
                }

                attempts++;
            }
            if (!moved) {
                // Bukkit.broadcastMessage("有効な移動先が見つかりませんでした。");
            }
            return;
        }
        if (target == null || currentTarget == null){
            return;
        }

        float[] input = buildStateVector(target);
        int[] actions = model.inferMulti(input);
        Bukkit.getLogger().info("SCAV AI Action = " + actions);
        System.out.println("Input state: " + Arrays.toString(input));

        int moveAction = actions[0];
        int combatAction = actions[1];

        // --- 移動系 ---
        switch (moveAction) {
            case 1 -> moveTowards(target, 0.1);   // 前進
            case 2 -> moveAway(target);           // 後退
            case 3 -> moveSide(target, -1);       // 左へ
            case 4 -> moveSide(target, 1);        // 右へ
            default -> stayIdle();                // 0または無効値
        }

        // --- 戦闘系 ---
        switch (combatAction) {
            case 1 -> shoot(target);              // 射撃
            case 2 -> reload();                    // リロード
            case 3 -> hide();                      // 遮蔽に移動
            default -> {}                          // 0または無効値は何もしない
        }
    }

    private Player getNearestPlayer(Entity self) {
        World selfWorld = self.getWorld();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(selfWorld)) continue; // ワールド（ディメンション）チェック
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue; // ゲームモードチェック
            if (isValidTarget(p)) return p;
        }
        return null;
    }

    private boolean isValidTarget(Player p) {
        return !p.isDead()
                && p.getWorld().equals(entity.getWorld())
                && entity.getLocation().distanceSquared(p.getLocation()) < 100
                && entity.hasLineOfSight(p);
    }

    private Player getTarget() {
        long now = System.currentTimeMillis();

        // 既存ターゲットチェック
        if (currentTarget != null && currentTarget.isOnline() && !currentTarget.isDead()) {
            double distance = currentTarget.getLocation().distance(entity.getLocation());
            if (distance <= 30) {
                lastSeenTime = now;

                // 遅延判定（初回設定時からの経過）
                if (now - targetAcquiredTime >= REACTION_DELAY_MS) {
                    return currentTarget;
                } else {
                    return null; // まだ反応しない
                }

            } else if (now - lastSeenTime < TARGET_TIMEOUT) {
                return currentTarget;
            } else {
                currentTarget = null;
            }
        }

        // 新規ターゲット探索
        Player nearest = getNearestPlayer(entity);
        if (nearest != null) {
            currentTarget = nearest;
            lastSeenTime = now;
            targetAcquiredTime = now; // ← 遅延の起点

            // サウンド再生：クールダウン判定付き
            if (now - lastSoundTime >= SOUND_COOLDOWN_MS) {
                entity.getWorld().playSound(entity.getLocation(), "minecraft:scav1", SoundCategory.HOSTILE, 1.0f, 1.0f);
                lastSoundTime = now;
            }
        }

        return null; // すぐには反応しない
    }

    private Location getRandomNearbyPatrolPoint(Location currentLocation, double maxDistance) {
        List<Location> nearbyPoints = new ArrayList<>();

        for (Location loc : patrolPoints) {
            if (loc.getWorld().equals(currentLocation.getWorld())
                    && loc.distanceSquared(currentLocation) <= maxDistance * maxDistance
                    && isValidPatrolLocation(loc)) { // ★ここを追加
                nearbyPoints.add(loc);
            }
        }

        if (nearbyPoints.isEmpty()) {
            // fallback: 全体から有効な場所を探す
            List<Location> validFallbacks = patrolPoints.stream()
                    .filter(this::isValidPatrolLocation)
                    .collect(Collectors.toList());

            if (validFallbacks.isEmpty()) {
                // 最悪の場合、元のリストから適当に選ぶ（最後の手段）
                return patrolPoints.get(new Random().nextInt(patrolPoints.size()));
            }

            return validFallbacks.get(new Random().nextInt(validFallbacks.size()));
        }

        return nearbyPoints.get(new Random().nextInt(nearbyPoints.size()));
    }


    private boolean isValidPatrolLocation(Location loc) {
        Block ground = loc.clone().add(0, -1, 0).getBlock();
        Block body = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();

        return ground.getType().isSolid()
                && body.getType().isAir()
                && head.getType().isAir();
    }


    private float[] buildStateVector(Player target) {
        Location scavLoc = entity.getLocation();
        Location targetLoc = target.getLocation();

        double dx = targetLoc.getX() - scavLoc.getX();
        double dz = targetLoc.getZ() - scavLoc.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float hp = (float) entity.getHealth() / (float) entity.getMaxHealth();
        float enemyHp = (float) target.getHealth() / (float) target.getMaxHealth();

        return new float[]{
                (float) (dist / 30.0),                  // 距離（正規化）
                (float) (dx / 30.0),                    // x差（正規化）
                (float) (dz / 30.0),                    // z差（正規化）
                hp,                                     // 自HP
                enemyHp,                                // 敵HP
                ammo / 10f,                             // 弾数
                isNearCover(scavLoc) ? 1.0f : 0.0f,     // 自分がカバー近く
                isNearCover(targetLoc) ? 1.0f : 0.0f,   // 敵がカバー近く
                lineOfSight(scavLoc, targetLoc) ? 1.0f : 0.0f // LOS判定
        };
    }

    private boolean lineOfSight(Location from, Location to) {
        if (from.getWorld() != to.getWorld()) return false;
        return from.getWorld().rayTraceBlocks(from, to.toVector().subtract(from.toVector()).normalize(),
                from.distance(to), FluidCollisionMode.NEVER, true) == null;
    }

    private boolean isNearCover(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (block.getType().isOccluding()) return true;
            }
        }
        return false;
    }


    public boolean hasLineOfSight(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);

        for (double i = 0; i < distance; i += 0.5) {
            Location checkLoc = from.clone().add(direction.clone().multiply(i));
            if (checkLoc.getBlock().getType().isOccluding()) {
                return false; // 遮蔽物あり
            }
        }
        return true; // 遮蔽物なし（見通せる）
    }


    private void moveTowards(Player target, double speed) {
        entity.getPathfinder().moveTo(target,1.0);
    }

    private void moveAway(Player target) {
        Location scavLoc = entity.getLocation();
        Location targetLoc = target.getLocation();

        // 敵との方向ベクトル（scav <- target の方向）
        double dx = scavLoc.getX() - targetLoc.getX();
        double dz = scavLoc.getZ() - targetLoc.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // 安全のため小さい距離に対処
        if (dist < 0.01) dist = 0.01;

        // 逃げる先を計算（正規化して10ブロック後退）
        double moveX = scavLoc.getX() + (dx / dist) * 10;
        double moveZ = scavLoc.getZ() + (dz / dist) * 10;
        Location targetLocation = new Location(scavLoc.getWorld(), moveX, scavLoc.getY(), moveZ);

        // 地面のY座標を取得（オプション）
        targetLocation.setY(scavLoc.getWorld().getHighestBlockYAt(targetLocation) + 1);

        // バニラ風に逃げる
        entity.getPathfinder().moveTo(targetLocation, 1.0); // 速度1.0で後退
    }

    private void loadPatrolPoints(World world) {
        File file = new File(plugin.getDataFolder(), "patrols/" + world.getName() + ".yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> pointStrings = config.getStringList("patrolPoints");
        for (String s : pointStrings) {
            String[] parts = s.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            patrolPoints.add(new Location(world, x + 0.5, y, z + 0.5));
        }
    }


    private void shoot(Player target) {
        if (ammo > 0) {
            entity.lookAt(target.getLocation());
            // ターゲットとの距離を取得
            Location scavLoc = entity.getEyeLocation();
            Location targetLoc = target.getEyeLocation();
            double distance = scavLoc.distance(targetLoc);

            // 発射方向ベクトル
            Vector direction = targetLoc.toVector().subtract(scavLoc.toVector()).normalize();

            // 拡散を加える
            direction = applySpread(direction, 5.0); // ±5度の拡散

            // 発射ターゲット地点 = 距離に応じてずらした位置
            Location shotTarget = scavLoc.clone().add(direction.multiply(distance));

            WeaponMechanicsAPI.shoot(entity, weaponName,shotTarget);
            ammo--;
        }
    }

    private Vector applySpread(Vector direction, double degrees) {
        Random random = new Random();

        // 角度（ラジアン）に変換して拡散
        double yaw = Math.toRadians((random.nextDouble() - 0.5) * degrees);
        double pitch = Math.toRadians((random.nextDouble() - 0.5) * degrees);

        // 現在の方向ベクトルに回転を加える（yaw + pitch）
        Vector newDir = direction.clone();

        // apply yaw (左右)
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double x = newDir.getX() * cosYaw - newDir.getZ() * sinYaw;
        double z = newDir.getX() * sinYaw + newDir.getZ() * cosYaw;
        newDir.setX(x);
        newDir.setZ(z);

        // apply pitch (上下)
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double y = newDir.getY() * cosPitch - newDir.length() * sinPitch;
        newDir.setY(y);

        return newDir.normalize();
    }


    private void reload() {
        ammo = 10;
    }

    private void hide() {
        Location origin = entity.getLocation();
        World world = origin.getWorld();

        double searchRadius = 10.0;
        Location bestCover = null;
        double bestScore = Double.MAX_VALUE;

        for (double x = -searchRadius; x <= searchRadius; x++) {
            for (double z = -searchRadius; z <= searchRadius; z++) {
                Location checkLoc = origin.clone().add(x, 0, z);
                Location topLoc = world.getHighestBlockAt(checkLoc).getLocation();

                if (isCoverBlock(topLoc)) {
                    double score = origin.distanceSquared(topLoc);
                    if (score < bestScore) {
                        bestScore = score;
                        bestCover = topLoc.clone().add(0, 1, 0); // 上に移動するため +1
                    }
                }
            }
        }

        if (bestCover != null) {
            entity.getPathfinder().moveTo(bestCover);
        }
    }

    public void setCurrentLocationTarget(Location shootLoc) {
        if (shootLoc == null) {
            plugin.getLogger().warning("ScavEntity: 渡されたshootLocがnullです。currentlocationTargetを設定しません。");
            return;
        }
        currentlocationTarget = shootLoc;
    }

    private boolean isCoverBlock(Location loc) {
        Material type = loc.getBlock().getType();

        // 遮蔽物になりそうなブロック例
        return type.isOccluding() && type != Material.OAK_LEAVES && type != Material.GLASS;
    }

    private void moveSide(Player target, int dir) {
        Location scavLoc = entity.getLocation();
        Location targetLoc = target.getLocation();

        double dx = targetLoc.getX() - scavLoc.getX();
        double dz = targetLoc.getZ() - scavLoc.getZ();
        double dist = Math.sqrt(dx*dx + dz*dz);

        if (dist < 0.01) dist = 0.01;

        // 方向ベクトルに垂直なベクトルを作る
        double sideX = -dz / dist * dir * 2;  // 左右に2ブロック移動
        double sideZ = dx / dist * dir * 2;

        Location moveLoc = scavLoc.clone().add(sideX, 0, sideZ);
        moveLoc.setY(scavLoc.getWorld().getHighestBlockYAt(moveLoc) + 1);

        entity.getPathfinder().moveTo(moveLoc, 1.0);
    }

    private void stayIdle() {
        // その場で待機
    }
}
