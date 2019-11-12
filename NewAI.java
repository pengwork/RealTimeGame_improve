/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

/**
 *
 * @author santi
 */
public class NewAI extends AbstractionLayerAI {
    Random r = new Random();
    protected UnitTypeTable utt;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;

    // Strategy implemented by this class:
    // If we have more than 1 "Worker": send the extra workers to attack to the nearest enemy unit
    // If we have a base: train workers non-stop
    // If we have a worker: do this if needed: build base, harvest resources
    public NewAI(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }


    public NewAI(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable a_utt)
    {
        utt = a_utt;
        if (utt!=null) {
            workerType = utt.getUnitType("Worker");
            baseType = utt.getUnitType("Base");
            barracksType = utt.getUnitType("Barracks");
            rangedType = utt.getUnitType("Ranged");
        }
    }


    public AI clone() {
        return new WorkerRush(utt, pf);
    }

    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        PlayerAction pa = new PlayerAction();
//        System.out.println("NewAI for player " + player + " (cycle " + gs.getTime() + ")");

        // behavior of bases:
        for(Unit u:pgs.getUnits()) {
            if (u.getType()==baseType &&
                    u.getPlayer() == player &&
                    gs.getActionAssignment(u)==null) {
                baseBehavior(u,p,pgs);
            }
        }
        // behavior of barracks:
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
            }
        }

        // behavior of melee units:
        for(Unit u:pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest &&
                    u.getPlayer() == player &&
                    gs.getActionAssignment(u)==null) {
                meleeUnitBehavior(u,p,gs);
            }
        }

        // behavior of workers:
        List<Unit> workers = new LinkedList<Unit>();
        for(Unit u:pgs.getUnits()) {
            if (u.getType().canHarvest &&
                    u.getPlayer() == player) {
                workers.add(u);
            }
        }
        workersBehavior(workers,p,gs);


        return translateActions(player,gs);
    }

    // WR 对比 RR 这里区别在于 WR一直产生worker， 而RR只产生一个worker
    public void baseBehavior(Unit u,Player p, PhysicalGameState pgs) {
        int nworkers = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == workerType
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
        }
        if (p.getResources()>=workerType.cost && nworkers < 5)
            train(u, workerType);
    }

    public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        // 与WD的区别是，不去攻击对方距离自己的base近的结点来实现defense
        for(Unit u2:pgs.getUnits()) {
            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy==null || d<closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy!=null) {
            attack(u,closestEnemy);
        }
    }

    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        if (p.getResources() >= rangedType.cost) {
            train(u, rangedType);
        }
    }
    public void workersBehavior(List<Unit> workers,Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int nbases = 0;
        int nbarracks = 0;
        int resourcesUsed = 0;
        List<Unit> hh = new LinkedList<>();
        Unit harvestWorker1 = null;
        List<Unit> freeWorkers = new LinkedList<Unit>();
        freeWorkers.addAll(workers);

        if (workers.isEmpty()) return;

        // 计算base 数目
        // 没有的话新建base
        // 和其他rush的区别 是这个部分不计算barrack
        for(Unit u2:pgs.getUnits()) {
            if (u2.getType() == baseType &&
                    u2.getPlayer() == p.getID()) nbases++;
            if(u2.getType() == barracksType &&
                    u2.getPlayer() == p.getID()){
                nbarracks++;
            }
        }
        List<Integer> reservedPositions = new LinkedList<Integer>();
        if (nbases==0 && !freeWorkers.isEmpty()) {
            // build a base:
            if (p.getResources()>=baseType.cost + resourcesUsed) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
                resourcesUsed+=baseType.cost;
            }
        }
        if (nbarracks == 0 && !freeWorkers.isEmpty()){
            //建立一个兵营
            if (p.getResources() >= barracksType.cost + resourcesUsed){
                Unit u = freeWorkers.remove( 0);//返回从freeworkers 中移除成为执行建立兵营任务的 worker
                buildIfNotAlreadyBuilding(u,barracksType, u.getX(), u.getY(), reservedPositions, p, pgs);
                resourcesUsed += barracksType.cost;
            }
        }

        // 增加一个harvest，从而产生积累而非一直消耗
        if (freeWorkers.size()>0)
            harvestWorker1 = freeWorkers.remove(0);
        hh.add(harvestWorker1);
        if (freeWorkers.size()>0)
            harvestWorker1 = freeWorkers.remove(0);
        hh.add(harvestWorker1);


        // harvest with the harvest worker:

        for(Unit harvestWorker: hh){
            if (harvestWorker!=null) {
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for(Unit u2:pgs.getUnits()) {
                if (u2.getType().isResource) {
                    int d = Math.abs(u2.getX() - harvestWorker.getX()) + Math.abs(u2.getY() - harvestWorker.getY());
                    if (closestResource==null || d<closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for(Unit u2:pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                    int d = Math.abs(u2.getX() - harvestWorker.getX()) + Math.abs(u2.getY() - harvestWorker.getY());
                    if (closestBase==null || d<closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource!=null && closestBase!=null) {
                AbstractAction aa = getAbstractAction(harvestWorker);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.target != closestResource || h_aa.base!=closestBase) harvest(harvestWorker, closestResource, closestBase);
                } else {
                    harvest(harvestWorker, closestResource, closestBase);
                }
            }
        }}

        for(Unit u:freeWorkers) meleeUnitBehavior(u, p, gs);

    }


    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }
}
