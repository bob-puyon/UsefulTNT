package syam.UsefulTNT;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

/**
 * 点火状態にあるTNTを管理します
 * @author syam
 */
public class PrimedTNTManager {
	public final static Logger log = UsefulTNT.log;
	private static final String logPrefix = UsefulTNT.logPrefix;
	private static final String msgPrefix = UsefulTNT.msgPrefix;

	private UsefulTNT plugin;
	/**
	 * コンストラクタ
	 * @param instance
	 */
	public PrimedTNTManager(UsefulTNT instance){
		plugin = instance;
	}

	/**
	 * 起動中の整地用TNTリスト
	 */
	private final List<PrimedTNT> primedList = new ArrayList<PrimedTNT>();

	/**
	 * 起動中の特殊TNTリストに登録する
	 * @param player 起爆したプレイヤー
	 * @param tntPrimed 対象のTNTエンティティ(TNTPrimed)Entity
	 * @param method 起爆メソッド
	 * @param direction 方向
	 */
	public void addPrimedList(Player player, TNTPrimed tntPrimed, Location loc, ExplosionMethod method, BlockFace direction){

		// リストに登録する
		primedList.add(new
				PrimedTNT(
						player,
						tntPrimed,
						loc,
						method,
						direction ));
	}

	/**
	 * 起動中の特殊TNTリストから削除する
	 * @param tntPrimed 削除対象のTNTエンティティ (TNTPrimed)Entity
	 */
	public void remPrimedList(TNTPrimed tntPrimed){
		// リストから取得
		PrimedTNT primedTNT = getPrimedTNT(tntPrimed);
		// リストにあれば削除
		if (primedTNT != null)
			primedList.remove(primedTNT);
	}

	/**
	 * リストに同じ起動中のTNTエンティティが存在するかチェックして返す
	 * @param tntPrimed チェック対象のTNTエンティティ (TNTPrimed)Entity
	 * @return 存在すればPrimedTNTクラスオブジェクト、無ければnull
	 */
	public PrimedTNT getPrimedTNT(TNTPrimed tntPrimed){
		PrimedTNT primedTNT = null;
		// リストを取得
		for (PrimedTNT primed : primedList){
			if (primed.tntPrimed == tntPrimed){
				primedTNT = primed;
			}
		}
		// 返す
		return primedTNT;
	}

	/**
	 * 点火状態にある特殊なTNTを表すクラス
	 * @author syam
	 */
	public class PrimedTNT {
		// 点火したプレイヤー
		public Player player;
		// 点火後のTNTエンティティ
		public TNTPrimed tntPrimed;
		// 点火前TNTブロックの座標]
		public Location loc;
		// 爆発種類
		public ExplosionMethod method;
		// 爆発の向き
		public BlockFace direction;

		public PrimedTNT(Player player, TNTPrimed primedTNT, Location loc, ExplosionMethod method, BlockFace direction){
			this.player = player;
			this.tntPrimed = primedTNT;
			this.loc = loc;
			this.method = method;
			this.direction = direction;
		}
	}
}
