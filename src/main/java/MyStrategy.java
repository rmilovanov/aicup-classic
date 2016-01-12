import com.sun.javafx.geom.Vec2d;
import model.*;

import static java.lang.StrictMath.*;

import java.util.*;

// modern version to upload on github


public final class MyStrategy implements Strategy {

    public double posX = 0;
    public double posY = 0;

    // Settings
    public double danDistanceFactor = 40.0D;
    public double wheelTurnFactor = 1.7D;
    public double getSlowDistance = 1000.0D;
    public double slowEnginePower = 1.0D; // 0.8
    public double brakeDistanceSpeedFactor = 50.0D;
    public double sideSpeedFactor = 8.0D;
    public double noBrakeSpeedConstant = 8.0D;

    // shooting settings
    public int buggyShootingCalculationTicks = 75;

    // director settings
    public int directorCalculatingTime = 5000; // 5000
    public double directorAccuracy = 5.0D; // 5.0D
    public double directorCarCircleVolume = 3.0D; // 3.0D
    public int directorLookTileDistance = 150;

    // Stuck and rear move
    public int ticksStay = 0;
    public int ticksStayNorm = 50;
    public int rearMoveTicksNorm = 115;
    public int rearMoveTicks = 0;
    public boolean rearMove = false;
    public int noRearMoveTicksNorm =230;
    public int noRearMoveTicks = 0;

    @Override
    public void move(Car self, World world, Game game, Move move) {

        MySettings mySettings = new MySettings(directorCarCircleVolume, directorLookTileDistance);
        Context context = new Context(game, world, self, mySettings);
        Debugger debugger = new Debugger(true);




        double myPointX =self.getX();
        double myPointY =self.getY();
        int myTileX = (int) Math.floor(myPointX/game.getTrackTileSize());
        int myTileY = (int) Math.floor(myPointY/game.getTrackTileSize());

        int nextWayPointX = self.getNextWaypointX();
        int nextWayPointY = self.getNextWaypointY();

        double nextWayPointXPos = (nextWayPointX+0.5D)*game.getTrackTileSize();
        double nextWayPointYPos = (nextWayPointY+0.5D)*game.getTrackTileSize();
        System.out.println("Distance: " + self.getDistanceTo(nextWayPointXPos, nextWayPointYPos));
        boolean wpUpdated = false;
        if (self.getDistanceTo(nextWayPointXPos, nextWayPointYPos) < 1200){

            int nextWayPointIndex = self.getNextWaypointIndex()+1;
            if (nextWayPointIndex == world.getWaypoints().length) nextWayPointIndex = 0;
            nextWayPointX = world.getWaypoints()[nextWayPointIndex][0];
            nextWayPointY = world.getWaypoints()[nextWayPointIndex][1];
            wpUpdated = true;
            System.out.println("WayPoint updated to: " + nextWayPointIndex+ " ("
                    +nextWayPointX+", "+ nextWayPointY);

        }


        TrackRecord trackRecord = new TrackRecord(context);


        // creating trackRecord

        PathTile wayPointTile = new PathTile(nextWayPointX, nextWayPointY, context);
        int [][] visits = routeMatrix.getMatrixForPoint(wayPointTile, context, wpUpdated);
        /*if (wpUpdated){
            trackRecord.buildUpdated(visits, self);
        }
        else {
            trackRecord.build(visits, self);
        }*/
        trackRecord.build(visits, self);
        //PathPoint pathPoint = Director.definePoint(trackRecord, self, context, directorCalculatingTime, directorAccuracy, debugger, directorLookTileDistance); // 5000 5
        PathPoint pathPoint = Director.choosePoint(trackRecord, self, context, directorCalculatingTime, directorAccuracy, debugger, 10);
        //PathPoint[] points = Director.definePoints(trackRecord, self, context, directorCalculatingTime, directorAccuracy, debugger, 3);
        List<PathPoint> points = Director.definePoints(trackRecord, self, context, directorCalculatingTime, directorAccuracy, debugger, 10);
        for (int i = 0; i < points.size(); i++) {
            PathTile chosenTile = points.get(i).getPathTile();
            System.out.println("Director point "+i+":  "+chosenTile.getXpos()+", "+chosenTile.getYpos());
        }

        System.out.println("wpX: " + nextWayPointX
                + "  wpY: " + nextWayPointY
                + "  ppx: " + pathPoint.getX()
                + "  ppy: " + pathPoint.getY());
        System.out.println("myX: " + self.getX()
                + "  myY: " + self.getY()
                + "  myTile: " + myTileX
                + "," + myTileY);

        for (int i = 0; i < trackRecord.length(); i++) {
            System.out.print(trackRecord.getTile(i).getXpos()+":"+trackRecord.getTile(i).getYpos()+"  ");
        }
        System.out.print("\n");
        Debugger.printMatrix(visits);



        double pathPointX = pathPoint.getX();
        double pathPointY = pathPoint.getY();

        double distance = self.getDistanceTo(pathPointX,pathPointY);

        //double dicsreteDistance = game.getCarHeight()*50;

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        double danDistance = danDistanceFactor * speedModule; // 55

        /*if (context.circleCarCollisionInTicks(15)){
            if (speedModule > 1) move.setBrake(true);
        }*/


        if (distance < danDistance ){
            if (speedModule > noBrakeSpeedConstant) move.setBrake(true);
        }



        TileType[][] tilesXY = world.getTilesXY();
        PathPoint myPoint = new PathPoint(self.getX(), self.getY(), context);
        PathTile myTile = myPoint.getPathTile();
        int realMyTileX = myTile.getXpos();
        int realMyTileY = myTile.getYpos();

        // Collecting bonuses

        Bonus[] Bonuses = world.getBonuses();
        for (int i = 0; i < Bonuses.length; i++) {
            if ((Bonuses[i].getType() == BonusType.PURE_SCORE)
                    || (Bonuses[i].getType() == BonusType.NITRO_BOOST)
                    || (Bonuses[i].getType() == BonusType.AMMO_CRATE)
                    || (Bonuses[i].getType() == BonusType.OIL_CANISTER)
                    || ((Bonuses[i].getType() == BonusType.REPAIR_KIT) && (self.getDurability() < 0.66D))){
                int bonusTileX = (int) Math.floor(Bonuses[i].getX()/game.getTrackTileSize());
                int bonusTileY = (int) Math.floor(Bonuses[i].getY()/game.getTrackTileSize());
                TileType bonusTileType = tilesXY[bonusTileX][bonusTileY];
                if (!((bonusTileType == TileType.LEFT_TOP_CORNER
                        || bonusTileType == TileType.RIGHT_TOP_CORNER
                        || bonusTileType == TileType.LEFT_BOTTOM_CORNER)
                        || bonusTileType == TileType.RIGHT_BOTTOM_CORNER)) {
                    int realPathTileX = (int) Math.floor(pathPointX / game.getTrackTileSize());
                    int realPathTileY = (int) Math.floor(pathPointY / game.getTrackTileSize());
                    if (bonusTileX == realPathTileX) {
                        if (realPathTileY > realMyTileY) {
                            // we're moving down
                            if ((bonusTileY <= realPathTileY) && (bonusTileY > realMyTileY)) {
                                pathPointX = Bonuses[i].getX();
                                pathPointY = Bonuses[i].getY();
                            }
                        }
                        if (realPathTileY < realMyTileY) {
                            // we're moving up
                            if ((bonusTileY >= realPathTileY) && (bonusTileY < realMyTileY)) {
                                pathPointX = Bonuses[i].getX();
                                pathPointY = Bonuses[i].getY();
                            }
                        }

                    }
                    if (bonusTileY == realPathTileY) {
                        if (realPathTileX > realMyTileX) {
                            // we're moving right
                            if ((bonusTileX <= realPathTileX) && (bonusTileX > realMyTileX)) {
                                pathPointX = Bonuses[i].getX();
                                pathPointY = Bonuses[i].getY();
                            }
                        }
                        if (realPathTileX < realMyTileX) {
                            // we're moving left
                            if ((bonusTileX >= realPathTileX) && (bonusTileX < realMyTileX)) {
                                pathPointX = Bonuses[i].getX();
                                pathPointY = Bonuses[i].getY();
                            }
                        }

                    }
                }
            }
        }



        pathPoint.setCoordinates(pathPointX, pathPointY);

        double speedX = self.getSpeedX();
        double speedY = self.getSpeedY();
        double distX = pathPointX - self.getX();
        double distY = pathPointY - self.getY();

        double angleMult = 1.0D;

        if (((speedX * distX) < 0) || ((speedY * distY) < 0)){
            //angleMult = abs(speedX * speedY);
            //System.out.println(angleMult);
        }

        //System.out.println(speedModule);
        CarModel carModel = new CarModel(context);
        double perpendicularSpeedFactor = -1.0D*carModel.getPerpendicularSpeed(pathPoint)/sideSpeedFactor;


        double angleToPathPoint = self.getAngleTo(pathPointX, pathPointY);
        //double wheelTurn = angleToPathPoint * wheelTurnFactor * angleMult * speedModule / PI;
        double wheelTurn = angleToPathPoint * wheelTurnFactor * angleMult + perpendicularSpeedFactor;

        move.setWheelTurn(wheelTurn);
        System.out.println("curWheelTurn: " + self.getWheelTurn()+ "  wheelTurn: "+wheelTurn);










        // check if we got stuck
        if ((Math.abs(self.getX() - posX)<2) && (Math.abs(self.getY() - posY)<2)){
            // we got stuck, let's move rear
            ticksStay++;
            if (world.getTick()>200) {
                if ((ticksStay > ticksStayNorm)) {
                    rearMove = true;
                    //System.out.println("Got stuck");
                    ticksStay = 0;
                }
            }
            else{
                ticksStay = 0;
            }

        }

        // check if we have to move back
        if (rearMove){
            rearMoveTicks++;
            if (rearMoveTicks>rearMoveTicksNorm){
                rearMove = false;
                ticksStay = 0;
                rearMoveTicks = 0;
                noRearMoveTicks = 0;
            }
            if (noRearMoveTicks < noRearMoveTicksNorm){
                rearMove = false;
            }
        }

        if (rearMove) {
            //double p = (rearMoveTicksNorm - rearMoveTicks)/rearMoveTicksNorm;
            double p = 0.75D;
            double pp = (-1.0D) * p;
            move.setEnginePower(pp);
            //System.out.println("Rear move power: " + p);
            //System.out.println("Rear move power: " + pp);
            move.setWheelTurn(-1.0D * angleToPathPoint);
        }
        else {
            //double enginePower = distance * 10 / dicsreteDistance;
            //double enginePower = (distance / 1) / speedModule;
            double enginePower = 1;
            distance = self.getDistanceTo(pathPointX,pathPointY);
            if (distance < getSlowDistance) enginePower = slowEnginePower;  // 1150 0.8
            // constant 1 : 4900 : 4900        03: 3000
            // 1000 : 4600 : 4100 : 4100       03: 2800 : 2100 : 2000
            // 1100                            03: 2000
            // 1200 : 4500 : 4300              03: 2100

            move.setEnginePower(enginePower);
        }



        /*if (speedModule > distance / brakeDistanceSpeedFactor ) {  // 50
            if (speedModule > noBrakeSpeed) {
                TileType pathTileType = tilesXY[(int)pathTileX][(int)pathTileY];
                if (((pathTileType == TileType.HORIZONTAL) || (pathTileType == TileType.VERTICAL)) &&
                        (self.getEnginePower() < 1.1D)){
                    //donothing
                }
                else{
                    move.setBrake(true);
                }

                //System.out.println("Setting break speed" +  speedModule +  "   distance " + distance);
            }

        }*/

        double angleToChosenPoint = Math.abs(self.getAngleTo(pathPointX, pathPointY));
        if ((distance > 3200.0D)
                && (world.getTick() > 200)
                && (angleToChosenPoint < (0.075D * PI))){
            System.out.println("Using nitro, angle to "+pathPointX+ ", "+pathPointY+" is "+angleToChosenPoint);
            move.setUseNitro(true);
            //System.out.println(distance);
        }


        System.out.println("Speed: " + speedModule+ "  Perpendicular speed:  "+carModel.getPerpendicularSpeed(pathPoint));

        posX = self.getX();
        posY = self.getY();
        noRearMoveTicks++;


        ///       SHOOTING

        Car[] cars = world.getCars();
        if (self.getType()== CarType.JEEP) {
            for (int i = 0; i < cars.length; i++) {
                Player[] players = world.getPlayers();
                int thisPlayerId = (int) cars[i].getPlayerId();
                Player thisPlayer = players[0];
                for (int j = 0; j < players.length; j++) {
                    if (players[j].getId() == thisPlayerId) {
                        thisPlayer = players[j];
                    }
                }


                if (thisPlayer.isMe() || cars[i].isTeammate()
                        || cars[i].isFinishedTrack()
                        || (cars[i].getDurability() == 0.0D)) {
                    // Do Nothing, it's me
                } else {
                    double angleToEnemy = abs(self.getAngleTo(cars[i].getX(), cars[i].getY()));
                    double distanceToEnemy = self.getDistanceTo(cars[i].getX(), cars[i].getY());
                    if ((angleToEnemy < (0.075D * PI))
                            && (self.getProjectileCount() > 0)
                            && (distanceToEnemy < (game.getCarHeight() * 5.0D))) {
                    /*System.out.println("Enemy detected");
                    System.out.println("angleToEnemy : " + angleToEnemy);
                    System.out.println("maxAngle : " + 0.075D * PI);
                    System.out.println("self.getProjectileCount() : " + self.getProjectileCount());*/
                        move.setThrowProjectile(true);
                    }
                }
            }
        }
        else{
            // SHOOTING FOR BUGGI
            for (int i = 0; i < cars.length; i++) {
                // if car is disabled or has finished or teammate go to next car
                Car thisCar = cars[i];
                Player[] players = world.getPlayers();
                int thisPlayerId = (int) thisCar.getPlayerId();
                Player thisPlayer = players[0];
                for (int j = 0; j < players.length; j++) {
                    if (players[j].getId() == thisPlayerId) {
                        thisPlayer = players[j];
                    }
                }
                if (thisCar.isFinishedTrack() || thisCar.isTeammate()
                        || thisCar.getDurability() == 0.0
                        || thisPlayer.isMe()){
                    // do nothing
                }
                else{
                    // calculate shooting for central missile
                    for (int s = 0; s < buggyShootingCalculationTicks; s++) {
                        double enemyXPoint = thisCar.getX() + (thisCar.getSpeedX()*s);
                        double enemyYPoint = thisCar.getY() + (thisCar.getSpeedY()*s);
                        double enemyRadius = thisCar.getWidth()/ 2.0D;
                        double missileSpeed = game.getWasherInitialSpeed();
                        double missileRadius = game.getWasherRadius();
                        double collisionDistance = enemyRadius + missileRadius;



                        double misXFactor = 0;
                        double misYFactor = 0;
                        double turretAngle = self.getAngle();
                        if (Math.abs(turretAngle) < (PI / 2.0D)){
                            misXFactor = Math.cos(turretAngle);
                            misYFactor = Math.sin(turretAngle);
                        }
                        else{
                            misXFactor = (-1.0D) * Math.cos(PI - turretAngle);
                            misYFactor = Math.sin(turretAngle);
                        }
                        double missileX = self.getX() + (misXFactor * missileSpeed * s);
                        double missileY = self.getY() + (misYFactor * missileSpeed * s);

                        double objectsDistance = Math.sqrt( Math.pow(enemyXPoint-missileX, 2) + Math.pow(enemyYPoint-missileY, 2) );
                        if (objectsDistance < collisionDistance){
                            move.setThrowProjectile(true);
                        }
                    }
                }
            }

        }
        ///       OIL


        for (int i = 0; i < cars.length; i++) {
            Player[] players = world.getPlayers();
            int thisPlayerId = (int)cars[i].getPlayerId();
            Player thisPlayer = players[0];
            for (int j = 0; j < players.length; j++) {
                if (players[j].getId() == thisPlayerId){
                    thisPlayer = players[j];
                }
            }


            if (thisPlayer.isMe()){
                // Do Nothing, it's me
            }
            else{
                double angleToEnemy = abs(self.getAngleTo(cars[i].getX(), cars[i].getY()));
                double distanceToEnemy = self.getDistanceTo(cars[i].getX(), cars[i].getY());
                if ((angleToEnemy > ((1 - 0.075D) * PI))
                        && (self.getOilCanisterCount()>0)
                        && (distanceToEnemy < (game.getCarHeight() * 7.0D))){
                    /*System.out.println("Enemy behind, drop oil ");
                    System.out.println("angleToEnemy : " + angleToEnemy);
                    System.out.println("minAngle : " + (1 - 0.075D) * PI);
                    System.out.println("self.getOilCanisterCount() : " + self.getOilCanisterCount());*/
                    move.setSpillOil(true);
                }
            }
        }

        ///// EnD CODE
    }



}

class Context{
    public Game game;
    public World world;
    public Car car;
    public MySettings settings;
    Context(Game newGame, World newWorld, Car newCar, MySettings newSettings) {
        game = newGame;
        world = newWorld;
        car = newCar;
        settings = newSettings;
    }
    public Direction getCarDirection(){
        double bearing = car.getAngle();
        Direction result = Direction.RIGHT;
        if ( (bearing > ((-1.0D) * PI /4.0D)) && (bearing <= (PI /4.0D)) ){
            result = Direction.RIGHT;
        }
        if ( ((bearing > (PI /4.0D) && (bearing <= (3.0D * PI/4.0D)))) ){
            result = Direction.DOWN;
        }
        if ( ((bearing > (3.0D * PI/4.0D)) || (bearing <= (-3.0D * PI/4.0D))) ){
            result = Direction.LEFT;
        }
        if ( ((bearing > (-3.0D * PI/4.0D)) && (bearing <= ((-1.0D) * PI /4.0D))) ){
            result = Direction.UP;
        }
        return result;
    }
    public int[] getXYTileDeltas(Direction dir){
        int[] result = {0, 0};
        int[] right = {1, 0};
        int[] down = {0, 1};
        int[] left = {-1, 0};
        int[] up = {0, -1};
        if (dir == Direction.RIGHT) result = right;
        if (dir == Direction.LEFT) result = left;
        if (dir == Direction.DOWN) result = down;
        if (dir == Direction.UP) result = up;
        return result;
    }
    public PathPoint getCarFacePoint(){
        double carX = car.getX();
        double carY = car.getY();
        double bumperDist = car.getHeight()/2.0D;

        double bumperXFactor = 0;
        double bumperYFactor = 0;
        double carAngle = car.getAngle();
        if (Math.abs(carAngle) < (PI / 2.0D)){
            bumperXFactor = Math.cos(carAngle);
            bumperYFactor = Math.sin(carAngle);
        }
        else{
            bumperXFactor = (-1.0D) * Math.cos(PI - carAngle);
            bumperYFactor = Math.sin(carAngle);
        }
        double faceX = carX + (bumperXFactor * bumperDist);
        double faceY = carY + (bumperYFactor * bumperDist);
        return new PathPoint(faceX, faceY, this);
    }
    public double distanceBetweenPoints (PathPoint pointOne, PathPoint pointTwo){
        return Math.sqrt( Math.pow(pointOne.getX()-pointTwo.getX(), 2) + Math.pow(pointOne.getY()-pointTwo.getY(), 2) );
    }
    public boolean circleCarCollisionInTicks(int ticks){
        boolean result = false;
        PathPoint carPoint = new PathPoint(car.getX(), car.getY(), this);
        double speedX = car.getSpeedX();
        double speedY = car.getSpeedY();

        for (int i = 0; i < ticks; i++) {
            double carX = car.getX()+speedX*i;
            double carY = car.getY()+speedY*i;
            carPoint.setCoordinates(carX, carY);
            PathTile calcTile = carPoint.getPathTile();
            if (calcTile.isValid()) {
                TileBounds tileBounds = Bounder.getBounds(calcTile, this);
                WorldCircle carCircle = new WorldCircle(car.getX(), car.getY(), car.getWidth() / 3.0D);

                if (!tileBounds.circleInside(carCircle) || calcTile.getTileType() == TileType.EMPTY) {
                    result = true;
                    i = ticks;
                }
            }
            else{
                result = true;
            }
        }
        return result;
    }
}
class CarModel{
    private double x;
    private double y;
    private double speedX;
    private double speedY;
    private double bearing;
    private double angleSpeed;
    private double wheelTurn;
    private Context c;
    /*CarModel(double nx, double ny, double newBearing, Context context){
        x = nx; y = ny; bearing = newBearing; c = context;
    }*/
    CarModel(Context context){
        c = context;
        Car car = c.car;
        x = car.getX(); y = car.getY();
        speedX = c.car.getSpeedX();
        speedY = c.car.getSpeedY();
        bearing = car.getAngle();
        angleSpeed = car.getAngularSpeed();
        wheelTurn = car.getWheelTurn();
    }
    public PathTile getPathTile(){
        return getPathPoint().getPathTile();
    }
    public PathPoint getPathPoint(){
        return new PathPoint(x, y, c);
    }
    public Direction[] getDirections(){
        Direction[] result = {Direction.UP, Direction.DOWN, Direction.RIGHT, Direction.LEFT};
        double upTime = getTimeToTurn(Direction.UP);
        double downTime = getTimeToTurn(Direction.DOWN);
        double rightTime = getTimeToTurn(Direction.RIGHT);
        double leftTime = getTimeToTurn(Direction.LEFT);
        Double[] times = {upTime, downTime, rightTime, leftTime};
        Debugger.print("before sort up down right left: "+upTime+", "+ downTime+", "+ rightTime+", "+ leftTime);
        for (int j = 3; j > 0; j--) {
            for (int i = j; i > 0; i--) {
                if (times[i] < times[i -1]) {
                    double buffer = times[i];
                    times[i] = times[i - 1];
                    times[i - 1] = buffer;
                    Direction dBuffer = result[i];
                    result[i] = result[i - 1];
                    result[i - 1] = dBuffer;
                }
            }
        }
        return result;
    }
    public double getTimeToTurn(Direction tDirection){
        PathPoint targetPoint = getPathTile().getNextTile(tDirection).getCenter();
        double sideSpeedCompensationTime = getPerpendicularSpeed(targetPoint)/10.0D;

        double directionAngle = TurnsAngles.getAbsoluteAngle(tDirection);
        double directionLeftAngle = TurnsAngles.getAbsoluteLeftAngle(tDirection);
        double result = Math.abs(bearing + (wheelTurn/2.0D) - directionAngle - sideSpeedCompensationTime);
        double leftTime = Math.abs(bearing + (wheelTurn/2.0D) - directionLeftAngle + sideSpeedCompensationTime);
        if (leftTime < result) result = leftTime;
        return result;
    }
    public double getTimeToTurnToPoint(PathPoint point){
        double sideSpeedCompensationTime = getPerpendicularSpeed(point)/150.0D;
        double angleToPoint = getAngleTo(point);
        double result = Math.abs(bearing + (wheelTurn/2.0D) - angleToPoint - sideSpeedCompensationTime);
        double leftTime = Math.abs(bearing + (wheelTurn/2.0D) - angleToPoint + sideSpeedCompensationTime);
        if (leftTime < result) result = leftTime;
        return result;
    }

    public double getAngleTo(PathPoint pathPoint){
        PathPoint carPoint = new PathPoint(x, y, c);
        double objAbsBearing = TurnsAngles.getBearingFromTo(carPoint, pathPoint);
        return objAbsBearing - bearing;
    }
    public double getAbsoluteAngleFromCar(PathPoint pathPoint){
        PathPoint carPoint = new PathPoint(x, y, c);
        return TurnsAngles.getBearingFromTo(carPoint, pathPoint);
    }
    public double getPerpendicularSpeed(PathPoint pathPoint){
        double angleTo = getAngleTo(pathPoint);
        double absoluteAngleToPoint = getAbsoluteAngleFromCar(pathPoint);
        double rightPerpendicularBearing = absoluteAngleToPoint + (0.5D*PI);
        double xm = cos(rightPerpendicularBearing);
        double ym = sin(rightPerpendicularBearing);
        //double sx = speedX/xm;
        //double sy = speedY/ym;
        double scal = speedX*xm + speedY*ym;
        double bModule = hypot(xm, ym);

        //double result = sx + sy;
        double result = scal / bModule;
        System.out.println("carBearing: "+Debugger.piAngle(bearing)+"*PI  point Bearing: "+Debugger.piAngle(absoluteAngleToPoint)+"*PI  angleToPoint: "+angleTo);
        System.out.println("Perpendicular: "+Debugger.piAngle(rightPerpendicularBearing));
        System.out.println("cos: "+xm+" sin: "+ym);
        System.out.println("speedX: "+speedX+" speedY: "+speedY);
        //System.out.println("sx: "+sx+" sy: "+sy);
        return result;
    }
}

class TurnsAngles{
    public static double getAbsoluteAngle(Direction dir){
        double result = 0;
        if (dir==Direction.RIGHT) result = 0;
        if (dir==Direction.DOWN) result = 0.5*PI;
        if (dir==Direction.UP) result = 1.5*PI;
        if (dir==Direction.LEFT) result = PI;
        return result;
    }
    public static double getAbsoluteLeftAngle(Direction dir){
        double result = 0;
        if (dir==Direction.RIGHT) result = 0;
        if (dir==Direction.DOWN) result = -1.5*PI;
        if (dir==Direction.UP) result = -0.5*PI;
        if (dir==Direction.LEFT) result = -PI;
        return result;
    }
    public static double normaliseAngle(double angle){
        double result = angle;
        if (angle < 0){
            result = 2*PI - angle;
        }
        return result;
    }
    public static double getBearingFromTo(PathPoint from, PathPoint to){
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);
        double hyp = hypot(adx, ady);
        double normAngle = Math.acos((pow(adx, 2)+pow(hyp, 2)-pow(ady, 2))/(2*adx*hyp));
        double resultAngle = normAngle;
        if (dx < 0) resultAngle = PI-normAngle;
        if (dy < 0) resultAngle = -1 * resultAngle;
        return resultAngle;
    }
}

class PathTile{
    private int x;
    private int y;
    private Game game;
    private World world;
    private Context c;
    private Map<TileType, ArrayList<Direction>> tileTypes;

    PathTile(int asX, int asY, Context context){
        setCoordinates(asX, asY);
        c = context;
        game = c.game;
        world = c.world;

        tileTypes = new HashMap<>();
        tileTypes.put(TileType.VERTICAL, new ArrayList<>(Arrays.asList(Direction.DOWN, Direction.UP)));
        tileTypes.put(TileType.HORIZONTAL, new ArrayList<Direction>(Arrays.asList(Direction.LEFT, Direction.RIGHT)));
        tileTypes.put(TileType.LEFT_TOP_CORNER, new ArrayList<Direction>(Arrays.asList(Direction.DOWN, Direction.RIGHT)));
        tileTypes.put(TileType.RIGHT_TOP_CORNER, new ArrayList<Direction>(Arrays.asList(Direction.DOWN, Direction.LEFT)));
        tileTypes.put(TileType.LEFT_BOTTOM_CORNER, new ArrayList<Direction>(Arrays.asList(Direction.UP, Direction.RIGHT)));
        tileTypes.put(TileType.RIGHT_BOTTOM_CORNER, new ArrayList<Direction>(Arrays.asList(Direction.LEFT, Direction.UP)));
        tileTypes.put(TileType.LEFT_HEADED_T, new ArrayList<Direction>(Arrays.asList(Direction.DOWN, Direction.LEFT, Direction.UP)));
        tileTypes.put(TileType.RIGHT_HEADED_T, new ArrayList<Direction>(Arrays.asList(Direction.DOWN, Direction.UP, Direction.RIGHT)));
        tileTypes.put(TileType.TOP_HEADED_T, new ArrayList<Direction>(Arrays.asList(Direction.LEFT, Direction.UP, Direction.RIGHT)));
        tileTypes.put(TileType.BOTTOM_HEADED_T, new ArrayList<Direction>(Arrays.asList(Direction.DOWN, Direction.LEFT, Direction.RIGHT)));
        tileTypes.put(TileType.CROSSROADS, new ArrayList<Direction>(Arrays.asList(Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT)));
        tileTypes.put(TileType.UNKNOWN, new ArrayList<Direction>(Arrays.asList(Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT)));
    }

    public void setCoordinates(int asX, int asY){
        x = asX;
        y = asY;
    }
    public PathTile getNextTile(Direction direction){
        int nx = x + c.getXYTileDeltas(direction)[0];
        int ny = y + c.getXYTileDeltas(direction)[1];
        return new PathTile(x, y, c);
    }

    public int getXpos(){
        return x;
    }
    public int getYpos(){
        return y;
    }
    public PathPoint getCenter(){
        double px = (x+0.5D)*game.getTrackTileSize();
        double py = (y+0.5D)*game.getTrackTileSize();
        return new PathPoint(px, py, c);
    }
    public TileType getTileType(){
        return world.getTilesXY()[x][y];
    }
    public List<Direction> getDirections(){
        return tileTypes.get(getTileType());
    }
    public boolean isDirectionAvailable(Direction someDirection){
        boolean result = false;
        if (getTileType()!=TileType.EMPTY) {
            List dirs = getDirections();
            //System.out.println("TileType: "+getTileType()+"  getDirections: "+getDirections()+" tile coordinates: "+x + ", "+y);
            int size = dirs.size();

            for (int i = 0; i < size; i++) {
                if (dirs.get(i) == someDirection) {
                    result = true;
                    i = size;
                }
            }
        }
        return result;
    }
    boolean isValid(){
        TileType tiles[][] = c.world.getTilesXY();
        TileType stiles[] = tiles[1];
        int hTilesCount = tiles.length;
        int vTilesCount = stiles.length;
        boolean result = true;
        if ((x < 0) || (x >= hTilesCount) ||(y < 0) || (y >= vTilesCount)) {
            result = false;
        }else {
            if (getTileType() == TileType.EMPTY) result = false;
        }
        return result;
    }
}

class PathPoint{
    private double x;
    private double y;
    private Game game;
    private Context c;
    PathPoint(double asX, double asY, Context context){
        c = context;
        game = c.game;
        setCoordinates(asX, asY);
    }

    public void setCoordinates(double asX, double asY){
        x = asX;
        y = asY;
    }

    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    public PathTile getPathTile(){
        int tileX = (int) Math.floor(x/game.getTrackTileSize());
        int tileY = (int) Math.floor(y/game.getTrackTileSize());
        return new PathTile(tileX, tileY, c);
    }
    public PathPoint getBearingDistantPoint(double bearingAngle, double distance){
        double xFactor = 0;
        double yFactor = 0;
        if (Math.abs(bearingAngle) < (PI / 2.0D)){
            xFactor = Math.cos(bearingAngle);
            yFactor = Math.sin(bearingAngle);
        }
        else{
            xFactor = (-1.0D) * Math.cos(PI - bearingAngle);
            yFactor = Math.sin(bearingAngle);
        }
        double pointX = x + (xFactor * distance);
        double pointY = y + (yFactor * distance);
        return new PathPoint(pointX, pointY, c);
    }
}

class TrackRecord{
    private PathTile[] tiles;
    private int elsCount;
    private Game game;
    private Context c;
    TrackRecord(Context context){
        elsCount = 0;
        tiles = new PathTile[300];
        game = context.game;
        c = context;
    }
    public void addTile(int posX, int posY){
        tiles[elsCount] = new PathTile(posX, posY, c);
        elsCount++;
    }
    public int length(){
        return elsCount;
    }
    public PathTile getTile(int index){
        return tiles[index];
    }
    public void build(int[][] matrix, Car car){
        PathPoint carPoint = new PathPoint(car.getX(), car.getY(), c);
        PathTile carTile = carPoint.getPathTile();
        int cx = carTile.getXpos();
        int cy = carTile.getYpos();
        int step = matrix[cx][cy]-1;
        CarModel carModel = new CarModel(c);
        Direction[] carDirections = carModel.getDirections();
        while (step > 0){
            for (int i = 0; i < carDirections.length; i++) {
                Direction carDirection = carDirections[i];
                //System.out.println("CarTile: "+ carTile.getXpos()+", "+carTile.getYpos()+" "+carTile.getTileType()+"  carDirection: "+carDirection);
                int calcX = cx + c.getXYTileDeltas(carDirection)[0];
                int calcY = cy + c.getXYTileDeltas(carDirection)[1];
                PathTile sugTile = new PathTile(calcX, calcY, c);
                if (sugTile.isValid() && carTile.isDirectionAvailable(carDirection)){
                    if (matrix[calcX][calcY]==step){
                        cx = calcX; cy = calcY;
                        this.addTile(cx, cy);
                        carTile.setCoordinates(cx, cy);
                        // TODO Here we should calculate new car bearing
                        i = carDirections.length;
                    }
                }
            }
            step--;
        }
    }

    public void buildOld(int[][] matrix, Car car){
        PathPoint carPoint = new PathPoint(car.getX(), car.getY(), c);
        PathTile carTile = carPoint.getPathTile();
        int cx = carTile.getXpos();
        int cy = carTile.getYpos();
        int step = matrix[cx][cy]-1;
        Direction carDirection = c.getCarDirection();
        while (step > 0) {

            // TODO: THIS IS FUCKING SHIT!!!!, REORGANIZE! It should depend on car dir priority;
            System.out.println("CarTile: "+ carTile.getXpos()+", "+carTile.getYpos()+" "+carTile.getTileType()+"  carDirection: "+carDirection);
            int calcX = cx + c.getXYTileDeltas(carDirection)[0];
            int calcY = cy + c.getXYTileDeltas(carDirection)[1];
            PathTile sugTile = new PathTile(calcX, calcY, c);
            if (sugTile.isValid()) {
                if (carTile.isDirectionAvailable(carDirection) &&
                        (matrix[cx + c.getXYTileDeltas(carDirection)[0]][cy + c.getXYTileDeltas(carDirection)[1]] == step)) {


                    System.out.println("Coordinates: " + cx + ", " + cy + "  deltas: "
                            + c.getXYTileDeltas(carDirection)[0] + ", "
                            + c.getXYTileDeltas(carDirection)[1]);
                    cx = cx + c.getXYTileDeltas(carDirection)[0];
                    cy = cy + c.getXYTileDeltas(carDirection)[1];
                    System.out.println("New coordinates: " + cx + ", " + cy);
                    this.addTile(cx, cy);
                    carTile.setCoordinates(cx, cy);
                    System.out.println("In building trackRecord new carTile: " + cx + ", " + cy);

                } else {
                    List<Direction> dirs = carTile.getDirections();
                    System.out.println("Calculating carTile " + carTile.getXpos()
                            + ", " + carTile.getYpos()
                            + " type: " + carTile.getTileType());
                    for (int i = 0; i < dirs.size(); i++) {

                        System.out.println("Direction: " + dirs.get(i));
                        cx = carTile.getXpos() + c.getXYTileDeltas(dirs.get(i))[0];
                        cy = carTile.getYpos() + c.getXYTileDeltas(dirs.get(i))[1];
                        PathTile calcTile = new PathTile(cx, cy, c);
                        if (calcTile.isValid()) {
                            if (matrix[cx][cy] == step && calcTile.isValid()) {
                                this.addTile(cx, cy);
                                carTile.setCoordinates(cx, cy);
                                System.out.println("In building trackRecord NOTCARDIR new carTile: " + cx + ", " + cy);
                                i = dirs.size();
                            }
                        }
                    }
                }
            }
            else{
                List<Direction> dirs = carTile.getDirections();
                System.out.println("Calculating carTile " + carTile.getXpos()
                        + ", " + carTile.getYpos()
                        + " type: " + carTile.getTileType());
                for (int i = 0; i < dirs.size(); i++) {

                    System.out.println("Direction: " + dirs.get(i));
                    cx = carTile.getXpos() + c.getXYTileDeltas(dirs.get(i))[0];
                    cy = carTile.getYpos() + c.getXYTileDeltas(dirs.get(i))[1];
                    PathTile calcTile = new PathTile(cx, cy, c);
                    if (calcTile.isValid()) {
                        if (matrix[cx][cy] == step ) {
                            this.addTile(cx, cy);
                            carTile.setCoordinates(cx, cy);
                            System.out.println("In building trackRecord NOTCARDIR new carTile: " + cx + ", " + cy);
                            i = dirs.size();
                        }
                    }
                }
            }
            step--;
        }

    }
}
class DirectorResult{
    public boolean status;
    public PathPoint resultObject;
    DirectorResult(boolean newStatus, PathPoint newObject){
        status = newStatus;
        resultObject = newObject;
    }
}

class Director {
    public static PathPoint choosePoint(TrackRecord tRecord, Car car, Context context, int ticks, double accuracy, Debugger debugger, int maxPoints){
        List<PathPoint> points = definePoints(tRecord, car, context, ticks, accuracy, debugger, maxPoints);
        double[] times = new double[maxPoints];
        double minTime = 200;
        if (points.size()==0){
            points.add(tRecord.getTile(0).getCenter());
        }
        PathPoint chosenPoint = points.get(0);
        CarModel carModel = new CarModel(context);
        for (PathPoint point:points ) {
            double time = carModel.getTimeToTurnToPoint(point);
            if (time < minTime){
                chosenPoint = point;
                minTime = time;
            }
        }
        chosenPoint = points.get(0);
        return chosenPoint;
    }
    public static List <PathPoint> definePoints(TrackRecord tRecord, Car car, Context context, int ticks, double accuracy, Debugger debugger, int maxPoints){
        List <PathPoint> result = new ArrayList<>();
        int lastTile = 200;
        for (int i = 0; i < maxPoints; i++) {
            DirectorResult res  = definePoint(tRecord, car, context, ticks, accuracy, debugger, lastTile);
            if (res.status) {
                result.add(res.resultObject);
                for (int j = 0; j < tRecord.length(); j++) {
                    PathPoint thisPoint = res.resultObject;
                    PathTile thisTile = thisPoint.getPathTile();
                    PathTile recordTile = tRecord.getTile(j);
                    if ((thisTile.getXpos() == recordTile.getXpos()) && (thisTile.getYpos() == recordTile.getYpos())) {
                        lastTile = j - 1;
                        if (lastTile < 0) lastTile = 0;
                    }
                }
            }
        }
        return result;
    }
    public static DirectorResult definePoint (TrackRecord tRecord, Car car, Context context, int ticks, double accuracy, Debugger debugger, int maxTile){
        Game game = context.game;

        PathPoint faceBumper = context.getCarFacePoint();

        PathPoint resultPoint = faceBumper.getBearingDistantPoint(car.getAngle(), 100.0D);

        if (maxTile > tRecord.length()-1) maxTile = tRecord.length()-1;
        int tileFar = maxTile;
        int directorLookTileDistance = context.settings.directorLookTileDistance;
        if (directorLookTileDistance < (tileFar+1)){
            tileFar = directorLookTileDistance - 1;
        }
        boolean gotPoint = false;
        while (!gotPoint && (tileFar > -1)){

            String choice = "unknown";

            PathTile curTile = tRecord.getTile(tileFar);
            PathPoint center = new PathPoint((curTile.getXpos()+0.5D)*game.getTrackTileSize(),
                    (curTile.getYpos()+0.5D)*game.getTrackTileSize(), context);



            PathPoint leftEnd = new PathPoint((curTile.getXpos())*game.getTrackTileSize(),
                    (curTile.getYpos()+0.5D)*game.getTrackTileSize(), context);
            PathPoint rightEnd  = new PathPoint((curTile.getXpos())*game.getTrackTileSize() + game.getTrackTileSize(),
                    (curTile.getYpos()+0.5D)*game.getTrackTileSize(), context);
            PathPoint topEnd = new PathPoint((curTile.getXpos()+0.5D)*game.getTrackTileSize(),
                    (curTile.getYpos())*game.getTrackTileSize(), context);
            PathPoint bottomEnd = new PathPoint((curTile.getXpos()+0.5D)*game.getTrackTileSize(),
                    (curTile.getYpos())*game.getTrackTileSize()+ game.getTrackTileSize(), context);
            if (curTile.getTileType() == TileType.RIGHT_TOP_CORNER){
                bottomEnd = new PathPoint((curTile.getXpos()+0.2D)*game.getTrackTileSize(),
                        (curTile.getYpos())*game.getTrackTileSize()+ game.getTrackTileSize(), context);
                leftEnd = new PathPoint((curTile.getXpos())*game.getTrackTileSize(),
                        (curTile.getYpos()+0.8D)*game.getTrackTileSize(), context);
            }
            if (curTile.getTileType() == TileType.LEFT_TOP_CORNER){
                bottomEnd = new PathPoint((curTile.getXpos()+0.8D)*game.getTrackTileSize(),
                        (curTile.getYpos())*game.getTrackTileSize()+ game.getTrackTileSize(), context);
                rightEnd = new PathPoint((curTile.getXpos())*game.getTrackTileSize(),
                        (curTile.getYpos()+0.2D)*game.getTrackTileSize(), context);
            }
            if (curTile.getTileType() == TileType.LEFT_BOTTOM_CORNER){
                topEnd = new PathPoint((curTile.getXpos()+0.8D)*game.getTrackTileSize(),
                        (curTile.getYpos())*game.getTrackTileSize()+ game.getTrackTileSize(), context);
                rightEnd = new PathPoint((curTile.getXpos())*game.getTrackTileSize(),
                        (curTile.getYpos()+0.2D)*game.getTrackTileSize(), context);
            }
            if (curTile.getTileType() == TileType.RIGHT_BOTTOM_CORNER){
                topEnd = new PathPoint((curTile.getXpos()+0.2D)*game.getTrackTileSize(),
                        (curTile.getYpos())*game.getTrackTileSize()+ game.getTrackTileSize(), context);
                leftEnd = new PathPoint((curTile.getXpos())*game.getTrackTileSize(),
                        (curTile.getYpos()+0.2D)*game.getTrackTileSize(), context);
            }



            PathPoint visiblePoint = leftEnd; choice = "leftEnd";
            //double minDist = car.getDistanceTo(leftEnd.getX(),leftEnd.getY());
            double minDist = context.distanceBetweenPoints(faceBumper, leftEnd);
            //double distRight = car.getDistanceTo(rightEnd.getX(),rightEnd.getY());
            double distRight = context.distanceBetweenPoints(faceBumper, rightEnd);
            if (distRight < minDist){
                visiblePoint = rightEnd; choice = "rightEnd";
                minDist = distRight;
            }
            //double distTop = car.getDistanceTo(topEnd.getX(),topEnd.getY());
            double distTop = context.distanceBetweenPoints(faceBumper, topEnd);
            if (distTop < minDist){
                visiblePoint = topEnd; choice = "topEnd";
                minDist = distTop;
            }
            //double distBottom = car.getDistanceTo(bottomEnd.getX(),bottomEnd.getY());
            double distBottom = context.distanceBetweenPoints(faceBumper, bottomEnd);
            if (distBottom < minDist){
                visiblePoint = bottomEnd; choice = "bottomEnd";

            }
            //visiblePoint = center; choice = "center";
            System.out.println("Choosing trackPoint if tRecord("+ tileFar+")=("+curTile.getXpos()
                    +","+curTile.getYpos()+" visPoint: " + choice
                    + " ("+visiblePoint.getX()+","+visiblePoint.getY()+")");

            gotPoint = true;
            for (int i = 0; i < ticks; i++) {
                //double distance = car.getDistanceTo(visiblePoint.getX(),visiblePoint.getX());
                double distance = context.distanceBetweenPoints(faceBumper, visiblePoint);
                double vectorX = (visiblePoint.getX()-faceBumper.getX())/distance;
                double vectorY = (visiblePoint.getY()-faceBumper.getY())/distance;
                double calcPosX = faceBumper.getX() + (i * accuracy * vectorX);
                double calcPosY = faceBumper.getY() + (i * accuracy * vectorY);

                int calcTileX = (int) Math.floor(calcPosX/game.getTrackTileSize());
                int calcTileY = (int) Math.floor(calcPosY/game.getTrackTileSize());


                PathTile calcTile = new PathTile(calcTileX, calcTileY, context);
                TileBounds tileBounds = Bounder.getBounds(calcTile, context);

                double directorCarCircleVolume = context.settings.directorCarCircleVolume;
                WorldCircle carCircle = new WorldCircle(calcPosX, calcPosY, car.getWidth()/directorCarCircleVolume);
                /*System.out.println("Circle "+carCircle.northEnd()+ " "
                        +carCircle.eastEnd() +" "
                        +carCircle.southEnd()+" "
                        +carCircle.westEnd()+" "
                        +" tile:"+calcTileX+ ","+calcTileY);
                System.out.println("Bounds "+tileBounds.getTopBound()+" "
                        +tileBounds.getRightBound()+" "
                        +tileBounds.getBottomBound()+" "
                        +tileBounds.getLeftBound()+" ");*/
                if (!tileBounds.circleInside(carCircle) || calcTile.getTileType()== TileType.EMPTY) {
                    gotPoint = false;
                    i = ticks;
                }
                if ((calcTileX == curTile.getXpos()) && (calcTileY == curTile.getYpos())){
                    i = ticks;
                }


            }
            if (gotPoint){

                resultPoint = visiblePoint;
                Debugger.print("chosen trackRecord:"+tileFar);
                debugger.toSlot(1, "chosen end: " + choice);
                CarModel carModel = new CarModel(context);
                Direction[] carDirs = carModel.getDirections();
                debugger.toSlot(2, "carDirections: "+carDirs[0]+", "
                        +carDirs[1]+", "
                        +carDirs[2]+", "
                        +carDirs[3]+" bearing: "+context.car.getAngle());
            }
            else{
                if (tileFar==0) {
                    resultPoint = tRecord.getTile(tileFar).getCenter();
                    //debugger.toSlot(1, "nothing chosen, going to trackRecord[0], center ");
                    debugger.toSlot(1, "nothing chosen, going to look from center ");
                    //resultPoint = definePointFromCarCenter(tRecord, car, context, ticks, accuracy, debugger);
                }

            }

            tileFar--;
        }

        return new DirectorResult(gotPoint, resultPoint);
    }
    public static PathPoint definePointFromCarCenter(TrackRecord tRecord, Car car, Context context, int ticks, double accuracy, Debugger debugger){
        Game game = context.game;

        PathPoint resultPoint = new PathPoint(car.getX(), car.getY(), context);
        int tileFar = tRecord.length()-1;
        boolean gotPoint = false;
        while (!gotPoint && (tileFar > -1)){

            String choice = "unknown";

            PathTile curTile = tRecord.getTile(tileFar);
            PathPoint center = new PathPoint((curTile.getXpos()+0.5D)*game.getTrackTileSize(),
                    (curTile.getYpos()+0.5D)*game.getTrackTileSize(), context);
            PathPoint leftEnd = new PathPoint((curTile.getXpos())*game.getTrackTileSize(),
                    (curTile.getYpos()+0.5D)*game.getTrackTileSize(), context);
            PathPoint rightEnd  = new PathPoint((curTile.getXpos())*game.getTrackTileSize() + game.getTrackTileSize(),
                    (curTile.getYpos()+0.5D)*game.getTrackTileSize(), context);
            PathPoint topEnd = new PathPoint((curTile.getXpos()+0.5D)*game.getTrackTileSize(),
                    (curTile.getYpos())*game.getTrackTileSize(), context);
            PathPoint bottomEnd = new PathPoint((curTile.getXpos()+0.5D)*game.getTrackTileSize(),
                    (curTile.getYpos())*game.getTrackTileSize()+ game.getTrackTileSize(), context);

            PathPoint visiblePoint = leftEnd; choice = "leftEnd";
            double minDist = car.getDistanceTo(leftEnd.getX(),leftEnd.getY());
            double distRight = car.getDistanceTo(rightEnd.getX(),rightEnd.getY());
            if (distRight < minDist){
                visiblePoint = rightEnd; choice = "rightEnd";
                minDist = distRight;
            }
            double distTop = car.getDistanceTo(topEnd.getX(),topEnd.getY());
            if (distTop < minDist){
                visiblePoint = topEnd; choice = "topEnd";
                minDist = distTop;
            }
            double distBottom = car.getDistanceTo(bottomEnd.getX(),bottomEnd.getY());
            if (distBottom < minDist){
                visiblePoint = bottomEnd; choice = "bottomEnd";

            }
            //visiblePoint = center; choice = "center";
            System.out.println("Choosing trackPoint if tRecord("+ tileFar+")=("+curTile.getXpos()
                    +","+curTile.getYpos()+" visPoint: " + choice
                    + " ("+visiblePoint.getX()+","+visiblePoint.getY()+")");

            gotPoint = true;
            for (int i = 0; i < ticks; i++) {
                double distance = car.getDistanceTo(visiblePoint.getX(),visiblePoint.getX());
                double vectorX = (visiblePoint.getX()-car.getX())/distance;
                double vectorY = (visiblePoint.getY()-car.getY())/distance;
                double calcPosX = car.getX() + (i * accuracy * vectorX);
                double calcPosY = car.getY() + (i * accuracy * vectorY);

                int calcTileX = (int) Math.floor(calcPosX/game.getTrackTileSize());
                int calcTileY = (int) Math.floor(calcPosY/game.getTrackTileSize());


                PathTile calcTile = new PathTile(calcTileX, calcTileY, context);
                TileBounds tileBounds = Bounder.getBounds(calcTile, context);
                WorldCircle carCircle = new WorldCircle(calcPosX, calcPosY, car.getWidth()/3.0D);
                /*System.out.println("Circle "+carCircle.northEnd()+ " "
                        +carCircle.eastEnd() +" "
                        +carCircle.southEnd()+" "
                        +carCircle.westEnd()+" "
                        +" tile:"+calcTileX+ ","+calcTileY);
                System.out.println("Bounds "+tileBounds.getTopBound()+" "
                        +tileBounds.getRightBound()+" "
                        +tileBounds.getBottomBound()+" "
                        +tileBounds.getLeftBound()+" ");*/
                if (!tileBounds.circleInside(carCircle) || calcTile.getTileType()== TileType.EMPTY) {
                    gotPoint = false;
                    i = ticks;
                }
                if ((calcTileX == curTile.getXpos()) && (calcTileY == curTile.getYpos())){
                    i = ticks;
                }


            }
            if (gotPoint){

                resultPoint = visiblePoint;
                Debugger.print("chosen trackRecord:"+tileFar);
                debugger.toSlot(1, "chosen end: " + choice);
            }
            else{
                if (tileFar==0) {
                    resultPoint = tRecord.getTile(tileFar).getCenter();
                    debugger.toSlot(1, "nothing chosen, going to trackRecord[0], center ");
                }

            }

            tileFar--;
        }
        return resultPoint;
    }
}

class WorldCircle{
    private double x;
    private double y;
    private double radius;
    WorldCircle(double cx, double cy, double rad){
        x = cx;
        y = cy;
        radius = rad;
    }
    public boolean gotCrossingWith(double ox, double oy, double orad){
        double centersDistance = Math.sqrt( Math.pow(ox - x, 2) + Math.pow(oy - y, 2) );
        double collisionDistance = orad + radius;
        boolean crossing = false;
        if (centersDistance <= collisionDistance){
            crossing = true;
        }
        return crossing;
    }
    public double northEnd(){
        return y - radius;
    }
    public double southEnd(){
        return y + radius;
    }
    public double westEnd(){
        return x - radius;
    }
    public double eastEnd(){
        return x + radius;
    }
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    public double getRadius(){
        return radius;
    }
}

class TileBounds{
    private double top;
    private double bottom;
    private double left;
    private double right;
    private int circlesCount;
    private WorldCircle circles[];
    private Game game;
    private Context c;
    TileBounds(Context context){
        circles = new WorldCircle[4];
        top = -1.0D;
        left = -1.0D;
        bottom = 80000.0D;
        right = 80000.0D;
        c = context;
        game = c.game;
    }
    public boolean circleInside(WorldCircle oCircle){
        boolean inside = true;
        if (
                (oCircle.northEnd()     <= top)   ||
                        (oCircle.southEnd()     >= bottom) ||
                        (oCircle.westEnd()      <= left) ||
                        (oCircle.eastEnd()      >= right)
                ){
            inside = false;
        }
        for (int i = 0; i < circlesCount; i++) {
            if (oCircle.gotCrossingWith(circles[i].getX(), circles[i].getY(),circles[i].getRadius() )){
                inside = false;
            }
        }
        return inside;
    }
    public void setTopBound(PathTile someTile){
        top = someTile.getYpos() * game.getTrackTileSize() + game.getTrackTileMargin();
    }
    public void setBottomBound(PathTile someTile){
        bottom = someTile.getYpos() * game.getTrackTileSize() + game.getTrackTileSize() - game.getTrackTileMargin();
    }
    public void setLeftBound(PathTile someTile){
        left = someTile.getXpos() * game.getTrackTileSize() + game.getTrackTileMargin();
    }
    public void setRightBound(PathTile someTile){
        right = someTile.getXpos() * game.getTrackTileSize() + game.getTrackTileSize() - game.getTrackTileMargin();
    }
    public void setTopLeftBound(PathTile someTile){
        WorldCircle topLeft = new WorldCircle(
                someTile.getXpos() * game.getTrackTileSize(),
                someTile.getYpos() * game.getTrackTileSize(),
                game.getTrackTileMargin());
        addCircle(topLeft);
    }
    public void setTopRightBound(PathTile someTile){
        WorldCircle topRight = new WorldCircle(
                someTile.getXpos() * game.getTrackTileSize() + game.getTrackTileSize(),
                someTile.getYpos() * game.getTrackTileSize(),
                game.getTrackTileMargin());
        addCircle(topRight);
    }
    public void setBottomLeftBound(PathTile someTile){
        WorldCircle bottomLeft = new WorldCircle(
                someTile.getXpos() * game.getTrackTileSize(),
                someTile.getYpos() * game.getTrackTileSize() + game.getTrackTileSize(),
                game.getTrackTileMargin());
        addCircle(bottomLeft);
    }
    public void setBottomRightBound(PathTile someTile){
        WorldCircle bottomRight = new WorldCircle(
                someTile.getXpos() * game.getTrackTileSize() + game.getTrackTileSize(),
                someTile.getYpos() * game.getTrackTileSize() + game.getTrackTileSize(),
                game.getTrackTileMargin());
        addCircle(bottomRight);
    }
    public void addCircle(WorldCircle newCircle){
        circles[circlesCount] = newCircle;
        circlesCount++;
    }
    public double getTopBound(){
        return top;
    }
    public double getBottomBound(){
        return bottom;
    }
    public double getLeftBound(){
        return left;
    }
    public double getRightBound(){
        return right;
    }
    public int getCircleCount(){
        return circlesCount;
    }
}
class Bounder{
    public static TileBounds getBounds(PathTile someTile, Context context){
        TileType tileType = context.world.getTilesXY()[someTile.getXpos()][someTile.getYpos()];

        Map <TileType, List<Bound>> tileTypeBounds = new HashMap<>();
        tileTypeBounds.put(TileType.VERTICAL,
                new ArrayList<>(Arrays.asList(Bound.LEFT, Bound.RIGHT)));
        tileTypeBounds.put(TileType.HORIZONTAL,
                new ArrayList<>(Arrays.asList(Bound.TOP, Bound.BOTTOM)));
        tileTypeBounds.put(TileType.LEFT_TOP_CORNER,
                new ArrayList<>(Arrays.asList(Bound.TOP, Bound.LEFT, Bound.BOTTOMRIGHT)));
        tileTypeBounds.put(TileType.RIGHT_TOP_CORNER,
                new ArrayList<>(Arrays.asList(Bound.TOP, Bound.RIGHT, Bound.BOTTOMLEFT)));
        tileTypeBounds.put(TileType.LEFT_BOTTOM_CORNER,
                new ArrayList<>(Arrays.asList(Bound.BOTTOM, Bound.LEFT, Bound.TOPRIGHT)));
        tileTypeBounds.put(TileType.RIGHT_BOTTOM_CORNER,
                new ArrayList<>(Arrays.asList(Bound.BOTTOM, Bound.RIGHT, Bound.TOPLEFT)));
        tileTypeBounds.put(TileType.LEFT_HEADED_T,
                new ArrayList<>(Arrays.asList(Bound.RIGHT, Bound.TOPLEFT, Bound.BOTTOMLEFT)));
        tileTypeBounds.put(TileType.RIGHT_HEADED_T,
                new ArrayList<>(Arrays.asList(Bound.LEFT, Bound.TOPRIGHT, Bound.BOTTOMRIGHT)));
        tileTypeBounds.put(TileType.TOP_HEADED_T,
                new ArrayList<>(Arrays.asList(Bound.BOTTOM, Bound.TOPRIGHT, Bound.TOPLEFT)));
        tileTypeBounds.put(TileType.BOTTOM_HEADED_T,
                new ArrayList<>(Arrays.asList(Bound.TOP, Bound.BOTTOMRIGHT, Bound.BOTTOMLEFT)));
        tileTypeBounds.put(TileType.CROSSROADS,
                new ArrayList<>(Arrays.asList(Bound.BOTTOMRIGHT, Bound.BOTTOMLEFT, Bound.TOPLEFT, Bound.TOPRIGHT)));
        tileTypeBounds.put(TileType.UNKNOWN,
                new ArrayList<>(Arrays.asList(Bound.BOTTOMRIGHT, Bound.BOTTOMLEFT, Bound.TOPLEFT, Bound.TOPRIGHT)));
        tileTypeBounds.put(TileType.EMPTY,
                new ArrayList<>(Arrays.asList(Bound.BOTTOMRIGHT, Bound.BOTTOMLEFT, Bound.TOPLEFT, Bound.TOPRIGHT)));

        List <Bound> bounds = tileTypeBounds.get(tileType);
        TileBounds result = new TileBounds(context);
        //System.out.println("TileType: " + tileType + " Bounds: " + bounds + " tick " + newWorld.getTick());
        for (Bound bound: bounds) {
            if (bound == Bound.TOP) result.setTopBound(someTile);
            if (bound == Bound.BOTTOM) result.setBottomBound(someTile);
            if (bound == Bound.RIGHT) result.setRightBound(someTile);
            if (bound == Bound.LEFT) result.setLeftBound(someTile);

            if (bound == Bound.TOPRIGHT) result.setTopRightBound(someTile);
            if (bound == Bound.BOTTOMRIGHT) result.setBottomRightBound(someTile);
            if (bound == Bound.TOPLEFT) result.setTopLeftBound(someTile);
            if (bound == Bound.BOTTOMLEFT) result.setBottomLeftBound(someTile);
        }
        return result;
    }
}

enum Bound {TOP, LEFT, BOTTOM, RIGHT, TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT};

class Debugger{
    private String[] slots;
    private boolean active;
    Debugger(boolean newActive){
        slots = new String[10];
        active = newActive;
    }
    public static void printMatrix(int[][] matrix){
        for (int j = 0; j < matrix[0].length; j++) {
            for (int i = 0; i < matrix.length; i++) {
                System.out.print(matrix[i][j] + "   ");
            }
            System.out.print("\n");
        }
    }
    public static void print(String mes){
        System.out.println(mes);
    }
    public void toSlot(int index, String mes){
        if (!Objects.equals(mes, slots[index])){
            slots[index] = mes;
            if (active) System.out.println("Slt:"+index+"  "+mes);
        }
    }
    public static double piAngle(double angle){
        double value = angle/PI;
        int precise = 4;
        precise = 10^precise;
        value = value*precise;
        int i = (int) Math.round(value);
        return (double) i/precise;
    }
}
class routeMatrix{
    public static int[][] getMatrixForPoint(PathTile wayPointTile, Context context, boolean updated){

        Direction carDirection = context.getCarDirection();
        int deltaX = (-1) * context.getXYTileDeltas(carDirection)[0];
        int deltaY = (-1) * context.getXYTileDeltas(carDirection)[1];
        PathPoint carPoint = new PathPoint(context.car.getX(), context.car.getY(), context);
        PathTile carTile = carPoint.getPathTile();
        int carFactTileX = carTile.getXpos() ;
        int carFactTileY = carTile.getYpos() ;
        int carTileX = carFactTileX;
        int carTileY = carFactTileY;
        if (updated) {
            carTileX = context.car.getNextWaypointX();
            carTileY = context.car.getNextWaypointY();
            carTile.setCoordinates(carTileX, carTileY);
        }
        int backTileX = carTile.getXpos() + deltaX;
        int backTileY = carTile.getYpos() + deltaY;

        TileType tiles[][] = context.world.getTilesXY();
        TileType stiles[] = tiles[1];

        int hTilesCount = tiles.length;
        int vTilesCount = stiles.length;

        int[][] visits;
        visits = new int[hTilesCount][vTilesCount];
        boolean reached = false;
        int front = 0;
        int step = 1;
        int nextWayPointX = wayPointTile.getXpos();
        int nextWayPointY = wayPointTile.getYpos();
        visits[nextWayPointX][nextWayPointY] = 1;

        if ((backTileX >= 0) && (backTileY >= 0)
                && (backTileX < hTilesCount) && (backTileY < vTilesCount)) {
            //visits[backTileX][backTileY] = -1;            // Back tile is deprecated
        }

        World world = context.world;

        Map<String, List<Integer>> dirs = new HashMap<>();
        dirs.put("DOWN", new ArrayList<>(Arrays.asList(0, 1)));
        dirs.put("LEFT", new ArrayList<>(Arrays.asList(-1, 0)));
        dirs.put("UP", new ArrayList<>(Arrays.asList(0, -1)));
        dirs.put("RIGHT", new ArrayList<>(Arrays.asList(1, 0)));

        Map <TileType, List<String>> tileTypes = new HashMap<>();
        tileTypes.put(TileType.VERTICAL, new ArrayList<>(Arrays.asList("DOWN", "UP")));
        tileTypes.put(TileType.HORIZONTAL, new ArrayList<>(Arrays.asList("LEFT", "RIGHT")));
        tileTypes.put(TileType.LEFT_TOP_CORNER, new ArrayList<>(Arrays.asList("DOWN", "RIGHT")));
        tileTypes.put(TileType.RIGHT_TOP_CORNER, new ArrayList<>(Arrays.asList("DOWN", "LEFT")));
        tileTypes.put(TileType.LEFT_BOTTOM_CORNER, new ArrayList<>(Arrays.asList("UP", "RIGHT")));
        tileTypes.put(TileType.RIGHT_BOTTOM_CORNER, new ArrayList<>(Arrays.asList("LEFT", "UP")));
        tileTypes.put(TileType.LEFT_HEADED_T, new ArrayList<>(Arrays.asList("DOWN", "LEFT", "UP")));
        tileTypes.put(TileType.RIGHT_HEADED_T, new ArrayList<>(Arrays.asList("DOWN", "UP", "RIGHT")));
        tileTypes.put(TileType.TOP_HEADED_T, new ArrayList<>(Arrays.asList( "LEFT", "UP", "RIGHT")));
        tileTypes.put(TileType.BOTTOM_HEADED_T, new ArrayList<>(Arrays.asList("DOWN", "LEFT", "RIGHT")));
        tileTypes.put(TileType.CROSSROADS, new ArrayList<>(Arrays.asList("DOWN", "LEFT", "UP", "RIGHT")));
        tileTypes.put(TileType.UNKNOWN, new ArrayList<>(Arrays.asList("DOWN", "LEFT", "UP", "RIGHT")));


        int deprecatedStep = -20;
        while (!reached){
            if ((carTileX == nextWayPointX) && (carTileY == nextWayPointY)){
                reached = true;
                System.out.println("I'm on wayPoint");
            }

            step++;
            front++;

            // TODO: debug mode
            if (step>250) {
                System.out.println("fuck steps");
                reached = true;
            }
            for (int i = 0; i < hTilesCount; i++) {
                for (int j = 0; j < vTilesCount; j++) {

                    if (visits[i][j] == front){
                        TileType[][] tilesXY = world.getTilesXY();
                        TileType thisTiletype = tilesXY[i][j];
                        if (thisTiletype != TileType.EMPTY){
                            List<String> directions;
                            directions = tileTypes.get(thisTiletype);
                            for (int k = 0; k < directions.size(); k++) {
                                List<Integer> deltas = dirs.get(directions.get(k));
                                int tx = i + deltas.get(0);
                                int ty = j + deltas.get(1);
                                if ((tx < 0) || (tx == hTilesCount) || (ty < 0) || (ty == vTilesCount)){
                                    // Tile does not exist, do nothing
                                }
                                else{
                                    if (visits[tx][ty] == 0){
                                        visits[tx][ty] = step;
                                        if (updated){
                                            reached = false;
                                            if ((tx==carFactTileX) && (ty==carFactTileY)){
                                                System.out.println("Blocking value setting for "+tx+", "+ty);
                                                visits[tx][ty] = 0;
                                            }
                                        }

                                        if ((tx == carTileX) && (ty == carTileY)){
                                            System.out.println("Got carTile "+tx+", "+ty+" deprecated step: "+deprecatedStep);
                                            reached = true;
                                            if (step == deprecatedStep){
                                                reached = false;
                                                visits[tx][ty] = 0;
                                            }

                                            if (updated){
                                                System.out.println("Changing to  "+carFactTileX+", "+carFactTileY);
                                                deprecatedStep = step;
                                                carTileX = carFactTileX;
                                                carTileY = carFactTileY;
                                                reached = false;
                                                updated = false;
                                            }
                                        }
                                    }
                                }
                            }
                        }




                    }
                }
            }


        }

        return visits;
    }
}
class MySettings{
    public double directorCarCircleVolume;
    public int directorLookTileDistance;
    MySettings(double newDirectorCarCircleVolume, int newDirectorLookTileDistance){
        directorCarCircleVolume = newDirectorCarCircleVolume;
        directorLookTileDistance = newDirectorLookTileDistance;
    }
}