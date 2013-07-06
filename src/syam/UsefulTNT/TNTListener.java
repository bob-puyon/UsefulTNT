package syam.UsefulTNT;

import static com.sk89q.worldguard.bukkit.BukkitUtil.*;

import java.util.HashMap;
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

import syam.UsefulTNT.PrimedTNTManager.PrimedTNT;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class TNTListener implements Listener{
	public final static Logger log = UsefulTNT.log;
	private final static String logPrefix = UsefulTNT.logPrefix;
	private final static String msgPrefix = UsefulTNT.msgPrefix;

	private final UsefulTNT plugin;
	private final HashMap<Material,Integer> enhancer = PrimedTNTManager.enhancer;

	public TNTListener(UsefulTNT plugin){
		this.plugin = plugin;
	}

	/* 登録するイベントはここから下に */

	/**
	 * TODO:
	 * 今の方法では、TNT起爆時に真下が空気だとTNTが落下するが、特殊な爆発は起爆時のTNT座標が元になる問題がある
	 * bob_puyon:メソッド別のコストの修正が必要になる
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

			// 権限チェック
			/*
			if (!player.hasPermission("sakuraserver.citizen"))
				return;
			*/

			// 所持アイテムのチェック
			if (event.getItem() == null){ return; }

			//*************
			// 必要条件OK
			//*************

			// どの方向に爆発させるか
			BlockFace direction = null;
			// どれほどの強さで爆発させるか
			int strength = 0;
			// 起爆モード
			ExplosionMethod method = null;

			// 所持アイテムにて爆発メソッドを変更させる
			// 火気アイテム系に関連して爆発メソッドを切り替える
			Material holding = event.getItem().getType();
			switch( holding ){
				case COAL:
					method = ExplosionMethod.DIRECTIONAL;
					break;
				case FLINT_AND_STEEL:
					method = ExplosionMethod.DIRECTIONAL_WIDE;
					break;
				case LAVA_BUCKET:
					method = ExplosionMethod.DIRECTIONAL_WIDE_EX;
					break;
				default:
					break;
			}

			// 爆発メソッドが確定しなかった場合はそのまま返す
			if (method == null){ return; }

			// 周辺のブロックを走査して、特殊な整地用TNTかどうかチェック
			if ( enhancer.containsKey( block.getRelative(BlockFace.UP).getType() ) ){
				direction = BlockFace.UP;
				strength = enhancer.get( block.getRelative(BlockFace.UP).getType() );

			}else if( enhancer.containsKey( block.getRelative(BlockFace.DOWN).getType() ) ){
				direction = BlockFace.DOWN;
				strength = enhancer.get( block.getRelative(BlockFace.DOWN).getType() );

			}else if( enhancer.containsKey( block.getRelative(BlockFace.NORTH).getType() ) ){
				direction = BlockFace.NORTH;
				strength = enhancer.get( block.getRelative(BlockFace.NORTH).getType() );

			}else if( enhancer.containsKey( block.getRelative(BlockFace.EAST).getType() ) ){
				direction = BlockFace.EAST;
				strength = enhancer.get( block.getRelative(BlockFace.EAST).getType() );

			}else if( enhancer.containsKey( block.getRelative(BlockFace.SOUTH).getType() ) ){
				direction = BlockFace.SOUTH;
				strength = enhancer.get( block.getRelative(BlockFace.SOUTH).getType() );

			}else if( enhancer.containsKey( block.getRelative(BlockFace.WEST).getType() ) ){
				direction = BlockFace.WEST;
				strength = enhancer.get( block.getRelative(BlockFace.WEST).getType() );
			}

			// 周辺にダイヤブロックが通常のTNTとして無ければそのまま返す
			if (direction == null){ return; }

			// 起動済みのTNTをスポーンさせて元のTNTの座標を保存して削除
			Location loc = block.getLocation();
			TNTPrimed tntPrimed = block.getWorld().spawn(loc.clone().add(0.5D, 0.5D, 0.5D), TNTPrimed.class);
			block.setType(Material.AIR);

			// TNTを起動中TNTリストに登録
			UsefulTNT.primedManager.addPrimedList(player, tntPrimed, loc, method, direction, strength);

			// 火打ち石を減らす
			//player.setItemInHand( new ItemStack(Material.AIR) );

			Actions.message(null, player, "&cYay! This is a Directional TNT! :3");
			Actions.message(null, player, "&c**安全確認** 整地TNTが起動しました... 威力: " + strength);

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
		if (event.getEntity() == null || !event.getEntityType().equals(EntityType.PRIMED_TNT)){
			return;
		}

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
				case DIRECTIONAL_WIDE_EX:
					explode_DIRECTIONAL_WIDE_EX(primedTNT);
					break;
				default: // Exception: Undefined ExplosionMethod
					log.warning(logPrefix+"Error occurred on switching explode method (TNTListener.class)");
					break;
			}
			// イベントをキャンセルする
			event.setCancelled(true);

			// 煙と音を鳴らす
			Location loc = primedTNT.loc;
			// TNTのエフェクトについては課題
			loc.getWorld().playEffect(loc, Effect.SMOKE, 4, 2);
			loc.getWorld().createExplosion(loc, 0.0F, false);

			// 爆発処理後にリストから削除する
			UsefulTNT.primedManager.remPrimedList(tntPrimed);
		}
	}

	// DIRECTIONAL TNT (1x1x?破壊) の爆発処理
	private void explode_DIRECTIONAL(PrimedTNT primedTNT){
		// 方向取得
		BlockFace direction = primedTNT.direction;
		// 威力取得
		int strength = primedTNT.strength;
		// プレイヤー取得
		Player player = primedTNT.player;

		// 爆発地点のブロックを取得
		Block block = primedTNT.loc.getBlock();
		// 16ブロック分を空気に変える
		// i=0は自身のブロック、i=1は向きを決定するブロックになる
		A:for (int i = 2; i <= strength+1; i++){
			Block check = block.getRelative(direction, i);
			// 例外ブロック これらは破壊されずスキップされる
			Material mat = check.getType();
			switch (mat){
				case AIR: // 空気の場合はスキップ
					continue;
				case BEDROCK: // 特殊ブロックで遮られた場合は中断
				case CHEST:
				case TRAPPED_CHEST:
				case FURNACE:
				case BURNING_FURNACE:
				case HOPPER:
				case DROPPER:
				case DISPENSER:
				case BREWING_STAND:
					break A;
				default:
					break;
			}

			// エリア保護チェック
			if (UsefulTNT.usingWorldGuard ){
				if( isProtectedBlock( player, check ) ){ return; }
			}
			// 変換前のブロックデータが必要なので先にロギング
			//HawkEyeAPI.addEntry(plugin, new BlockEntry(player, DataType.EXPLOSION, check));
			// カスタムイベントは使わない ロールバック出来ない
			// HawkEyeAPI.addCustomEntry(plugin, "UsefulTNT", player, check.getLocation(), String.valueOf(mat.getId()));

			// 変換
			check.setType(Material.AIR);
		}
		// 通知
		notifyModerator( msgPrefix + "&6 "+player.getName()+" &fが整地用TNTを使用しました");
		notifyModerator( msgPrefix + "&c 種類：&6DIRECTIONAL TNT &c 場所：&6"+Actions.getBlockLocationString(block.getLocation()));

	}

	// DIRECTIONAL_WIDE TNT (3x3x?破壊) の爆発処理
	private void explode_DIRECTIONAL_WIDE(PrimedTNT primedTNT){
		// 方向取得
		BlockFace direction = primedTNT.direction;
		// 威力取得
		int strength = primedTNT.strength;
		// プレイヤー取得
		Player player = primedTNT.player;
		// 爆発地点のブロックを取得
		Block block = primedTNT.loc.getBlock();

		// 走査の定義
		Block origin = null;  //走査始点の定義
		BlockFace horizonal = null;  //水平走査方向の定義
		BlockFace vartical= null;   //垂直走査方向の定義
		switch( direction ){
			case UP:
				//北を向いた状態で上を見て、左上(SOUTH_WEST)を始点と定義
				horizonal = BlockFace.EAST;
				vartical = BlockFace.NORTH;
				origin = block.getRelative(BlockFace.SOUTH_WEST, 1);
				break;
			case DOWN:
				//北を向いた状態で下を見て、左上(NORTH_WEST)を始点と定義
				horizonal = BlockFace.EAST;
				vartical = BlockFace.SOUTH;
				origin = block.getRelative(BlockFace.NORTH_WEST, 1);
				break;
			case NORTH:
				horizonal = BlockFace.EAST;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.WEST, 1).getRelative(BlockFace.UP, 1+1);
				break;
			case EAST:
				horizonal = BlockFace.SOUTH;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.NORTH, 1).getRelative(BlockFace.UP, 1+1);
				break;
			case SOUTH:
				horizonal = BlockFace.WEST;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.EAST, 1).getRelative(BlockFace.UP, 1+1);
				break;
			case WEST:
				horizonal = BlockFace.NORTH;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.SOUTH, 1).getRelative(BlockFace.UP, 1+1);
				break;
			default:
				break;
		}

		if( origin == null ){ return; }

		// strengthブロック分を空気に変える
		// i=0は自身のブロック、i=1は向きを決定するブロックになる
		for( int h = 0; h < 3; h++){
			for( int v= 0; v < 3; v++ ){
				A:for( int d = 2; d <= strength+1; d++){
					Block check = origin.getRelative( horizonal, h).getRelative(vartical, v).getRelative(direction, d);
					// 例外ブロック これらは破壊されずスキップされる
					Material mat = check.getType();
					switch (mat){
					case AIR: // 空気の場合はスキップ
						continue;
					case BEDROCK: // 特殊ブロックで遮られた場合は中断
					case CHEST:
					case TRAPPED_CHEST:
					case FURNACE:
					case BURNING_FURNACE:
					case HOPPER:
					case DROPPER:
					case DISPENSER:
					case BREWING_STAND:
						break A;
					default:
						break;
					}

					// エリア保護チェック
					if (UsefulTNT.usingWorldGuard ){
						if( isProtectedBlock( player, check ) ){ return; }
					}
					// 変換前のブロックデータが必要なので先にロギング
					//HawkEyeAPI.addEntry(plugin, new BlockEntry(player, DataType.EXPLOSION, check));
					// カスタムイベントは使わない ロールバック出来ない
					// HawkEyeAPI.addCustomEntry(plugin, "UsefulTNT", player, check.getLocation(), String.valueOf(mat.getId()));

					// 変換
					check.setType(Material.AIR);
				}
			}
		}
		// 通知
		notifyModerator( msgPrefix + "&6 "+player.getName()+" &fが整地用TNTを使用しました");
		notifyModerator( msgPrefix + "&c 種類：&6DIRECTIONAL WIDE TNT &c 場所：&6"+Actions.getBlockLocationString(block.getLocation()));

	}

	// DIRECTIONAL_WIDE_EX TNT (5x5x?破壊) の爆発処理
	private void explode_DIRECTIONAL_WIDE_EX(PrimedTNT primedTNT){
		// 方向取得
		BlockFace direction = primedTNT.direction;
		// 威力取得
		int strength = primedTNT.strength;
		// プレイヤー取得
		Player player = primedTNT.player;
		// 爆発地点のブロックを取得
		Block block = primedTNT.loc.getBlock();

		// 走査の定義
		Block origin = null;  //走査始点の定義
		BlockFace horizonal = null;  //水平走査方向の定義
		BlockFace vartical= null;   //垂直走査方向の定義
		switch( direction ){
			case UP:
				//北を向いた状態で上を見て、左上(SOUTH_WEST)を始点と定義
				horizonal = BlockFace.EAST;
				vartical = BlockFace.NORTH;
				origin = block.getRelative(BlockFace.SOUTH_WEST, 2);
				break;
			case DOWN:
				//北を向いた状態で下を見て、左上(NORTH_WEST)を始点と定義
				horizonal = BlockFace.EAST;
				vartical = BlockFace.SOUTH;
				origin = block.getRelative(BlockFace.NORTH_WEST, 2);
				break;
			case NORTH:
				horizonal = BlockFace.EAST;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.WEST, 2).getRelative(BlockFace.UP, 2+2);
				break;
			case EAST:
				horizonal = BlockFace.SOUTH;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.NORTH, 2).getRelative(BlockFace.UP, 2+2);
				break;
			case SOUTH:
				horizonal = BlockFace.WEST;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.EAST, 2).getRelative(BlockFace.UP, 2+2);
				break;
			case WEST:
				horizonal = BlockFace.NORTH;
				vartical = BlockFace.DOWN;
				origin = block.getRelative(BlockFace.SOUTH, 2).getRelative(BlockFace.UP, 2+2);
				break;
			default:
				break;
		}

		if( origin == null ){ return; }

		// strengthブロック分を空気に変える
		// i=0は自身のブロック、i=1は向きを決定するブロックになる
		for( int h = 0; h < 5; h++){
			for( int v= 0; v < 5; v++ ){
				A:for( int d = 2; d <= strength+1; d++){
					Block check = origin.getRelative( horizonal, h).getRelative(vartical, v).getRelative(direction, d);
					// 例外ブロック これらは破壊されずスキップされる
					Material mat = check.getType();
					switch (mat){
					case AIR: // 空気の場合はスキップ
						continue;
					case BEDROCK: // 特殊ブロックで遮られた場合は中断
					case CHEST:
					case TRAPPED_CHEST:
					case FURNACE:
					case BURNING_FURNACE:
					case HOPPER:
					case DROPPER:
					case DISPENSER:
					case BREWING_STAND:
						break A;
					default:
						break;
					}

					// エリア保護チェック
					if (UsefulTNT.usingWorldGuard ){
						if( isProtectedBlock( player, check ) ){ return; }
					}
					// 変換前のブロックデータが必要なので先にロギング
					//HawkEyeAPI.addEntry(plugin, new BlockEntry(player, DataType.EXPLOSION, check));
					// カスタムイベントは使わない ロールバック出来ない
					// HawkEyeAPI.addCustomEntry(plugin, "UsefulTNT", player, check.getLocation(), String.valueOf(mat.getId()));

					// 変換
					check.setType(Material.AIR);
				}
			}
		}
		// 通知
		notifyModerator( msgPrefix + "&6 "+player.getName()+" &fが整地用TNTを使用しました");
		notifyModerator( msgPrefix + "&c 種類：&6DIRECTIONAL WIDE EX TNT &c 場所：&6"+Actions.getBlockLocationString(block.getLocation()));
	}

	// 例外ブロックのチェックを行う
	// do stuff

	// WorldGuardによる地形保護確認
	boolean isProtectedBlock(Player player,Block check){
		// 建築可否
		if (!plugin.wgPlugin.canBuild( player, check)){
			Actions.message(null, player, "&c他人の保護エリアに入っています！");
			return true;
		}
		// 保護領域取得
		RegionManager rm = plugin.wgPlugin.getRegionManager(check.getWorld());
		LocalPlayer localPlayer = plugin.wgPlugin.wrapPlayer(player);
		ApplicableRegionSet set = rm.getApplicableRegions(toVector(check));

		if (!set.allows(DefaultFlag.TNT, localPlayer)){
			Actions.message(null, player, "&cTNT使用不可エリアに入っています！");
			return true;
		}else if (!set.allows(DefaultFlag.LIGHTER, localPlayer)){
			Actions.message(null, player, "&c火打ち石使用不可エリアに入っています！");
			return true;
		}
		return false;
	}

	// 権限別の使用通知(要修正)
	void notifyModerator(String message){
		Actions.permcastMessage( "usefultnt.admin", message);
	}
}


