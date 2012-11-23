package syam.UsefulTNT;

import static com.sk89q.worldguard.bukkit.BukkitUtil.toVector;

import java.util.logging.Logger;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import syam.UsefulTNT.PrimedTNTManager.PrimedTNT;
import uk.co.oliwali.HawkEye.DataType;
import uk.co.oliwali.HawkEye.entry.BlockEntry;
import uk.co.oliwali.HawkEye.util.HawkEyeAPI;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class TNTListener implements Listener{
	public final static Logger log = UsefulTNT.log;
	private final static String logPrefix = UsefulTNT.logPrefix;
	private final static String msgPrefix = UsefulTNT.msgPrefix;

	private final UsefulTNT plugin;

	public TNTListener(UsefulTNT plugin){
		this.plugin = plugin;
	}

	/* 登録するイベントはここから下に */

	/**
	 * TODO:
	 * 今の方法では、TNT起爆時に真下が空気だとTNTが落下するが、特殊な爆発は起爆時のTNT座標が元になる問題がある
	 *
	 */

	/**
	 * プレイヤーがクリックした
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event){
		Player player = event.getPlayer();
		Block block = null;
		if (event.hasBlock()){
			block = event.getClickedBlock();
		}else{
			return;
		}

		// TNTを右クリックした
		if (block.getType() == Material.TNT && event.getAction() == Action.RIGHT_CLICK_BLOCK ){
			// ふるいに掛ける

			// エンドは弾く
			if (block.getWorld().getEnvironment() == Environment.THE_END)
				return;

			// 所持アイテムチェック 火打ち石以外は弾く
			if (event.getItem() == null || event.getItem().getType() != Material.FLINT_AND_STEEL)
				return;

			// 権限チェック
			if (!player.hasPermission("sakuraserver.citizen"))
				return;

			// 必要条件OK
			// 周辺のブロックを走査して、特殊な整地用TNTかどうかチェック

			// どの方向に爆発させるか
			BlockFace direction = null;
			// 起爆モード
			ExplosionMethod method = ExplosionMethod.DIRECTIONAL; // TODO:暫定的に固定

			// 周辺走査
			if (block.getRelative(BlockFace.UP).getType() == Material.LAPIS_BLOCK)
				direction = BlockFace.UP;
			else if(block.getRelative(BlockFace.DOWN).getType() == Material.LAPIS_BLOCK)
				direction = BlockFace.DOWN;
			else if(block.getRelative(BlockFace.NORTH).getType() == Material.LAPIS_BLOCK)
				direction = BlockFace.NORTH;
			else if(block.getRelative(BlockFace.EAST).getType() == Material.LAPIS_BLOCK)
				direction = BlockFace.EAST;
			else if(block.getRelative(BlockFace.SOUTH).getType() == Material.LAPIS_BLOCK)
				direction = BlockFace.SOUTH;
			else if(block.getRelative(BlockFace.WEST).getType() == Material.LAPIS_BLOCK)
				direction = BlockFace.WEST;

			// 周辺にダイヤブロックが通常のTNTとして無ければそのまま返す
			if (direction == null)
				return;

			// 起動済みのTNTをスポーンさせて元のTNTの座標を保存して削除
			Location loc = block.getLocation();
			TNTPrimed tntPrimed = block.getWorld().spawn(loc.clone().add(0.5D, 0.5D, 0.5D), TNTPrimed.class);
			block.setType(Material.AIR);

			// TNTを起動中TNTリストに登録
			UsefulTNT.primedManager.addPrimedList(player, tntPrimed, loc, method, direction);

			// 火打ち石を減らす
			player.setItemInHand(new ItemStack(Material.AIR));

			Actions.message(null, player, "&cYay! This is a Directional TNT! :3");

			// イベントキャンセル
			event.setCancelled(true);
		}
	}

	/**
	 * 爆発が発生した
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event){
		// TNT以外は弾く
		if (event.getEntity() == null || !event.getEntityType().equals(EntityType.PRIMED_TNT))
			return;

		// TNTPrimedにキャスト
		TNTPrimed tntPrimed = (TNTPrimed)event.getEntity();

		// それが特殊なTNTかどうかチェックする
		PrimedTNT primedTNT = UsefulTNT.primedManager.getPrimedTNT(tntPrimed);
		if (primedTNT != null){
			// 特殊なTNTなら種類によって処理を分ける
			ExplosionMethod method = primedTNT.method;

			switch (method){
				case DIRECTIONAL:
					explode_DIRECTIONAL(primedTNT);
					break;
				case DIRECTIONAL_WIDE:
					explode_DIRECTIONAL_WIDE(primedTNT);
					break;
				default: // Exception: Undefined ExplosionMethod
					log.warning(logPrefix+"Error occurred on switching explode method (TNTListener.class)");
					break;
			}
			// イベントをキャンセルする
			event.setCancelled(true);

			// 煙と音を鳴らす
			Location loc = primedTNT.loc;
			loc.getWorld().playEffect(loc, Effect.SMOKE, 3);
			loc.getWorld().createExplosion(loc, 0.0F, false);

			// 爆発処理後にリストから削除する
			UsefulTNT.primedManager.remPrimedList(tntPrimed);
		}
	}

	// DIRECTIONAL TNT (1x1x16破壊) の爆発処理
	private void explode_DIRECTIONAL(PrimedTNT primedTNT){
		// 方向取得
		BlockFace direction = primedTNT.direction;
		// プレイヤー取得
		Player player = primedTNT.player;

		// 爆発地点のブロックを取得
		Block block = primedTNT.loc.getBlock();
		// 16ブロック分を空気に変える
		// i=0は自身のブロック、i=1は向きを決定するブロックになる
		A:for (int i = 2; i <= 17; i++){
			Block check = block.getRelative(direction, i);
			// 例外ブロック これらは破壊されずスキップされる
			Material mat = check.getType();
			switch (mat){
				case AIR: // 空気の場合はスキップ
					continue;
				case BEDROCK: // 特殊ブロックで遮られた場合は中断
				case CHEST:
				case FURNACE:
					break A;
				case DISPENSER:
					return;
			}


			// エリア保護チェック
			if (UsefulTNT.usingWorldGuard){
				// 建築可否
				if (!plugin.wgPlugin.canBuild(player, check)){
					Actions.message(null, player, "&c他人の保護エリアに入っています！");
					return;
				}

				// 保護領域取得
				RegionManager rm = plugin.wgPlugin.getRegionManager(check.getWorld());
				LocalPlayer localPlayer = plugin.wgPlugin.wrapPlayer(player);
				ApplicableRegionSet set = rm.getApplicableRegions(toVector(check));

				if (!set.allows(DefaultFlag.TNT, localPlayer)){
					Actions.message(null, player, "&cTNT使用不可エリアに入っています！");
					return;
				}else if (!set.allows(DefaultFlag.LIGHTER, localPlayer)){
					Actions.message(null, player, "&c火打ち石使用不可エリアに入っています！");
					return;
				}
			}


			// 変換前のブロックデータが必要なので先にロギング
			HawkEyeAPI.addEntry(plugin, new BlockEntry(player, DataType.EXPLOSION, check));
			// カスタムイベントは使わない ロールバック出来ない
			// HawkEyeAPI.addCustomEntry(plugin, "UsefulTNT", player, check.getLocation(), String.valueOf(mat.getId()));

			// 変換
			check.setType(Material.AIR);
		}
		// 通知
		Actions.permcastMessage("sakuraserver.helper", "&c[通知] &6 "+player.getName()+"&fが&6DIRECTIONAL TNT&fを使用: &6"+Actions.getBlockLocationString(block.getLocation()));
	}

	// DIRECTIONAL_WIDE TNTの爆発処理
	private void explode_DIRECTIONAL_WIDE(PrimedTNT primedTNT){
		// do stuff
	}
}


