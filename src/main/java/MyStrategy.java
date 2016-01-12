import javafx.geometry.Point2D;
import model.*;

import static java.lang.StrictMath.*;

import java.util.*;
// version classic published as 13th some text for new branch


public final class MyStrategy implements Strategy {

    public boolean debugPrint = true;

    public double posX = 0;
    public double posY = 0;

    // Settings
    public double danDistanceFactor = 40.0D;
    public double wheelTurnFactor = 1.8D;
    public double getSlowDistance = 1000.0D;
    public double slowEnginePower = 0.8D; // 0.8
    public double brakeDistanceSpeedFactor = 55.0D;
    public double noBrakeSpeed = 23;

    // shooting settings
    public int buggyShootingCalculationTicks = 75;

    // Stuck and rear move
    public int ticksStay = 0;
    public int ticksStayNorm = 50;
    public int rearMoveTicksNorm = 115;
    public int rearMoveTicks = 0;
    public boolean rearMove = false;
    public int noRearMoveTicksNorm =230;
    public int noRearMoveTicks = 0;

    // Behaviours
    public boolean startNoTurns = true;
    public double startNoTurnsDistance = -1;
    public double passedStartDistance = 0;


    @Override
    public void move(Car self, World world, Game game, Move move) {

        // Define tile types and ways
        Map<String, List<Integer>> dirs = new HashMap<String, List<Integer>>();
        dirs.put("DOWN", new ArrayList<Integer>(Arrays.asList(0, 1)));
        dirs.put("LEFT", new ArrayList<Integer>(Arrays.asList(-1, 0)));
        dirs.put("UP", new ArrayList<Integer>(Arrays.asList(0, -1)));
        dirs.put("RIGHT", new ArrayList<Integer>(Arrays.asList(1, 0)));

        Map <TileType, List<String>> tileTypes = new HashMap<TileType, List<String>>();
        tileTypes.put(TileType.VERTICAL, new ArrayList<String>(Arrays.asList("DOWN", "UP")));
        tileTypes.put(TileType.HORIZONTAL, new ArrayList<String>(Arrays.asList("LEFT", "RIGHT")));
        tileTypes.put(TileType.LEFT_TOP_CORNER, new ArrayList<String>(Arrays.asList("DOWN", "RIGHT")));
        tileTypes.put(TileType.RIGHT_TOP_CORNER, new ArrayList<String>(Arrays.asList("DOWN", "LEFT")));
        tileTypes.put(TileType.LEFT_BOTTOM_CORNER, new ArrayList<String>(Arrays.asList("UP", "RIGHT")));
        tileTypes.put(TileType.RIGHT_BOTTOM_CORNER, new ArrayList<String>(Arrays.asList("LEFT", "UP")));
        tileTypes.put(TileType.LEFT_HEADED_T, new ArrayList<String>(Arrays.asList("DOWN", "LEFT", "UP")));
        tileTypes.put(TileType.RIGHT_HEADED_T, new ArrayList<String>(Arrays.asList("DOWN", "UP", "RIGHT")));
        tileTypes.put(TileType.TOP_HEADED_T, new ArrayList<String>(Arrays.asList( "LEFT", "UP", "RIGHT")));
        tileTypes.put(TileType.BOTTOM_HEADED_T, new ArrayList<String>(Arrays.asList("DOWN", "LEFT", "RIGHT")));
        tileTypes.put(TileType.CROSSROADS, new ArrayList<String>(Arrays.asList("DOWN", "LEFT", "UP", "RIGHT")));
        tileTypes.put(TileType.UNKNOWN, new ArrayList<String>(Arrays.asList("DOWN", "LEFT", "UP", "RIGHT")));



        double myPointX =self.getX();
        double myPointY =self.getY();
        int myTileX = (int) Math.floor(myPointX/game.getTrackTileSize());
        int myTileY = (int) Math.floor(myPointY/game.getTrackTileSize());
        int nextWayPointIndex = self.getNextWaypointIndex();

        double nextWayPointX = self.getNextWaypointX();
        double nextWayPointY = self.getNextWaypointY();

        TileType tiles[][] = world.getTilesXY();
        TileType stiles[] = tiles[1];

        int hTilesCount = tiles.length;
        int vTilesCount = stiles.length;

        double pathTileX = 0;
        double pathTileY = 0;

        int[][] visits;
        visits = new int[tiles.length][stiles.length];
        boolean reached = false;
        int front = 0;
        int step = 1;
        visits[(int)nextWayPointX][(int)nextWayPointY] = 1;
        while (reached == false){
            if ((myTileX == nextWayPointX) && (myTileY == nextWayPointY)){
                reached = true;
                System.out.println("I'm on wayPoint");
            }
            //else{
            step++;
            front++;

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
                                    //System.out.println("Tile " + tx + ", " + ty + " does not exist");
                                }
                                else{
                                    if (visits[tx][ty] == 0){
                                        visits[tx][ty] = step;
                                        //System.out.println("New tile " + tx + ", " + ty);
                                        // Check if we reached the aim
                                        if ((tx == myTileX) && (ty == myTileY)){
                                            reached = true;
                                            //System.out.println("The Aim is reached " + tx + ", " + ty);
                                            pathTileX = i;
                                            pathTileY = j;

                                        }
                                    }
                                }
                            }
                        }




                    }
                }
            }
            //}

        }

        // Here we redefine path point depending on car bearing
        double carBearing = self.getAngle();

        List<String> carDirections = new ArrayList<>();
        if (carBearing == 0){
            // east
            carDirections = tileTypes.get(TileType.RIGHT_HEADED_T);
        }
        if ((carBearing > 0) && (carBearing < (PI/2.0D))){
            // south east
            carDirections = tileTypes.get(TileType.LEFT_TOP_CORNER);
        }
        if (carBearing == (PI/2.0D)) {
            // south
            carDirections = tileTypes.get(TileType.BOTTOM_HEADED_T);
        }
        if ((carBearing > (PI/2.0D)) && (carBearing < PI)) {
            // south west
            carDirections = tileTypes.get(TileType.RIGHT_TOP_CORNER);
        }
        if ((carBearing == PI) || (carBearing == (-1.0D * PI))) {
            // west
            carDirections = tileTypes.get(TileType.LEFT_HEADED_T);
        }
        if ((carBearing > (-1.0D * PI)) && (carBearing < (-1.0D * PI / 2.0D))) {
            // north west
            carDirections = tileTypes.get(TileType.RIGHT_BOTTOM_CORNER);
        }
        if (carBearing == (-1.0D * PI / 2.0D)) {
            // north
            carDirections = tileTypes.get(TileType.TOP_HEADED_T);
        }
        if ((carBearing > (-1.0D * PI / 2.0D)) && (carBearing < 0)) {
            // north east
            carDirections = tileTypes.get(TileType.LEFT_BOTTOM_CORNER);
        }
        boolean gotCarDirection = false;

        TileType[][] tilesXY = world.getTilesXY();
        TileType carTileType = tilesXY[myTileX][myTileY];
        int pathFactor = visits[myTileX][myTileY] -1;
        //while (!gotCarDirection) {
            /*System.out.println("Cycle gotCarDirection");
            System.out.println("carBearing" + carBearing);
            System.out.println("carTileType " + carTileType);
            System.out.println("pathFactor " + pathFactor);
            System.out.println("carDirections.size() " +carDirections.size());*/

        for (int q = 0; q < carDirections.size(); q++) {
            String curDirection = carDirections.get(q);//System.out.println("curDirection " + curDirection);
            boolean dirAvailable = false;
            List<String> avDirections;
            avDirections = tileTypes.get(carTileType);
            //System.out.println("avDirections.size() " + avDirections.size());
            for (int e = 0; e < avDirections.size(); e++) {
                //System.out.println("avDirections.get(e) " + avDirections.get(e));
                if (avDirections.get(e) == curDirection){
                    dirAvailable = true;
                }
            }
            if (dirAvailable) {
                //System.out.println("DIRECTION IS AVAILABLE");
                // Print visits
                for (int j = 0; j < vTilesCount; j++) {
                    for (int i = 0; i < hTilesCount; i++) {
                        //System.out.print(visits[i][j] + "   ");
                    }
                    //System.out.print("\n");
                }
                List<Integer> deltas = dirs.get(curDirection);
                int px = myTileX + deltas.get(0);
                int py = myTileY + deltas.get(1);
                if (visits[px][py] == pathFactor){
                    gotCarDirection =true;
                    q = carDirections.size();
                    pathTileX = px;
                    pathTileY = py;
                    //System.out.println("DIRECTION CHOSEN  TICK" + world.getTick());
                }
            }

        }
        //}



        // Here we define the last tile on the direct part of route

        boolean gotLastTile = false;
        String directorBearing = "unknown";

        int dx = 0;
        int dy = 0;

        // Define the line
        if (myTileX == pathTileX){
            // Vertical
            // Define up or down
            if (myTileY > pathTileY ){
                // Up
                dy = -1;
                directorBearing = "UP";
            }
            else{
                // Down
                dy = 1;
                directorBearing = "DOWN";
            }
        }
        else{
            // Horizontal
            // Define left or right
            if (myTileX > pathTileX ){
                // Left
                dx = - 1;
                directorBearing = "LEFT";
            }
            else{
                // Right
                dx = 1;
                directorBearing = "RIGHT";
            }
        }
        int nstep = visits[(int)pathTileX][(int)pathTileY] - 1;
        //TileType[][] tilesXY = world.getTilesXY();
        TileType thisTileType = tilesXY[(int)pathTileX][(int)pathTileY];
        List<String> fDirections;
        fDirections = tileTypes.get(thisTileType);
        gotLastTile = true;
        for (int n = 0; n < fDirections.size(); n++) {
            if (fDirections.get(n) == directorBearing){
                gotLastTile = false;
            }
        }

        while ((gotLastTile == false) && (nstep > 1)){
            int nx = (int)pathTileX + dx;
            int ny = (int)pathTileY + dy;
            if ((nx < hTilesCount) && (ny < vTilesCount) && (nx >= 0) && (ny >= 0)) {
                if (visits[nx][ny] == nstep) {
                    pathTileX = nx;
                    pathTileY = ny;
                    thisTileType = tilesXY[nx][ny];
                    gotLastTile = true;
                    if (thisTileType != TileType.EMPTY) {
                        List<String> directions;
                        directions = tileTypes.get(thisTileType);
                        for (int n = 0; n < directions.size(); n++) {
                            if (directions.get(n) == directorBearing){
                                gotLastTile = false;
                            }
                        }
                    }

                } else {
                    gotLastTile = true;
                }
            } else{
                gotLastTile = true;
            }
            nstep = visits[(int)pathTileX][(int)pathTileY] - 1;
        }


        // Here ghost appears

        double ghostPointX = (pathTileX+0.5D) * game.getTrackTileSize();
        double ghostPointY = (pathTileY+0.5D) * game.getTrackTileSize();
        int ghostTileX = (int) Math.floor(ghostPointX/game.getTrackTileSize());
        int ghostTileY = (int) Math.floor(ghostPointY/game.getTrackTileSize());

        int ghostNextWayPointIndex = self.getNextWaypointIndex();

        if ((ghostTileX == self.getNextWaypointX()) && (ghostTileY == self.getNextWaypointY())){
            ghostNextWayPointIndex = nextWayPointIndex + 1;
            if (ghostNextWayPointIndex == world.getWaypoints().length) {
                ghostNextWayPointIndex = 1;
            }
        }
        int ghostNextWayPointX = world.getWaypoints()[ghostNextWayPointIndex][0];
        int ghostNextWayPointY = world.getWaypoints()[ghostNextWayPointIndex][1];

        double ghostPathTileX = 0;
        double ghostPathTileY = 0;

        int[][] bisits;
        bisits = new int[tiles.length][stiles.length];
        boolean breached = false;
        int bfront = 0;
        int bstep = 1;
        bisits[ghostNextWayPointX][ghostNextWayPointY] = 1;

        String mDirection = "unknown";

        while (breached == false){

            bstep++;
            bfront++;

            for (int i = 0; i < hTilesCount; i++) {
                for (int j = 0; j < vTilesCount; j++) {

                    if (bisits[i][j] == bfront){
                        //TileType[][] tilesXY = world.getTilesXY();
                        //TileType thisTileType = tilesXY[i][j];
                        thisTileType = tilesXY[i][j];
                        if (thisTileType != TileType.EMPTY){
                            List<String> directions;
                            directions = tileTypes.get(thisTileType);
                            for (int k = 0; k < directions.size(); k++) {
                                List<Integer> deltas = dirs.get(directions.get(k));
                                int tx = i + deltas.get(0);
                                int ty = j + deltas.get(1);
                                if ((tx < 0) || (tx == hTilesCount) || (ty < 0) || (ty == vTilesCount)){
                                    // Tile does not exist, do nothing
                                    //System.out.println("Tile " + tx + ", " + ty + " does not exist");
                                }
                                else{
                                    if (bisits[tx][ty] == 0){
                                        bisits[tx][ty] = bstep;
                                        //System.out.println("New tile " + tx + ", " + ty);
                                        // Check if we reached the aim
                                        if ((tx == ghostTileX) && (ty == ghostTileY)){
                                            breached = true;
                                            //System.out.println("The Aim is reached " + tx + ", " + ty);
                                            ghostPathTileX = i;
                                            ghostPathTileY = j;

                                        }
                                    }
                                }
                            }
                        }




                    }
                }
            }


        }


        // Longer director
        if ((myTileX == ghostPathTileX) || (myTileY == ghostPathTileY)){
            pathTileX = ghostPathTileX;
            pathTileY = ghostPathTileY;
        }




        // end ghost code
        double pathPointX = (pathTileX+0.5D) * game.getTrackTileSize();
        double pathPointY = (pathTileY+0.5D) * game.getTrackTileSize();

        if (pathTileX == myTileX){
            // vertical
            if (pathTileY > myTileY){
                // moving down
                pathPointY = pathPointY - (game.getTrackTileSize()/2.0D);
                mDirection = "down no ghost";
            }
            else{
                // moving up
                pathPointY = pathPointY + (game.getTrackTileSize()/2.0D);
                mDirection = "up no ghost";
            }
        }

        if (pathTileY == myTileY){
            // horizontal
            if (pathTileX > myTileX){
                // moving right
                pathPointX = pathPointX - (game.getTrackTileSize()/2.0D);
                mDirection = "right no ghost";
            }
            else{
                // moving left
                pathPointX = pathPointX + (game.getTrackTileSize()/2.0D);
                mDirection = "left no ghost";
            }
        }






        double distance = self.getDistanceTo(pathPointX,pathPointY);

        double dicsreteDistance = game.getCarHeight()*50;

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        double danDistance = danDistanceFactor * speedModule; // 55

        int realMyTileX = myTileX;
        int realMyTileY = myTileY;
        if (distance < danDistance ){


            // Trying to hose route depending on car bearing even for ghost
            TileType ghostCarTileType = tilesXY[ghostTileX][ghostTileY];
            // TODO We've decided that ghostCarDirection equals to carDirection, although it is not
            int ghostPathFactor = bisits[ghostTileX][ghostTileY] -1;
            boolean gotCarDirectionForGhost = false;
            //while (!gotCarDirectionForGhost) {
                /*System.out.println("Cycle gotCarDirection FOR GHOST");
                System.out.println("carBearing" + carBearing);
                System.out.println("ghostCarTileType " + ghostCarTileType);
                System.out.println("ghostPathFactor " + ghostPathFactor);
                System.out.println("carDirections.size() " +carDirections.size());*/

            for (int q = 0; q < carDirections.size(); q++) {
                String curDirection = carDirections.get(q);//System.out.println("curDirection " + curDirection);
                boolean dirAvailable = false;
                List<String> avDirections;
                avDirections = tileTypes.get(ghostCarTileType);
                //System.out.println("avDirections.size() " + avDirections.size());
                for (int e = 0; e < avDirections.size(); e++) {
                    //System.out.println("avDirections.get(e) " + avDirections.get(e));
                    if (avDirections.get(e) == curDirection){
                        dirAvailable = true;
                        e = avDirections.size();
                    }
                }
                if (dirAvailable) {
                    //System.out.println("DIRECTION IS AVAILABLE");
                    //System.out.println("ghostTileX" + ghostTileX);
                    //System.out.println("ghostTileY" + ghostTileY);
                    // Print visits
                    //System.out.println("bizits________________________________________________");
                    for (int j = 0; j < vTilesCount; j++) {
                        for (int i = 0; i < hTilesCount; i++) {
                            //System.out.print(bisits[i][j] + "   ");
                        }
                        //System.out.print("\n");
                    }
                    List<Integer> deltas = dirs.get(curDirection);
                    int px = ghostTileX + deltas.get(0);
                    int py = ghostTileY + deltas.get(1);
                    if (bisits[px][py] == ghostPathFactor){
                        gotCarDirectionForGhost =true;
                        q = carDirections.size();
                        ghostPathTileX = px;
                        ghostPathTileY = py;
                        //System.out.println("GHOST DIRECTION CHOSEN  TICK" + world.getTick());
                    }
                }

            }
            //}

            // TODO: dedededebag mode
            System.out.println("TICK " + world.getTick());










            // END Trying to hose route depending on car bearing even for ghost






            pathPointX = (ghostPathTileX+0.5D) * game.getTrackTileSize();
            pathPointY = (ghostPathTileY+0.5D) * game.getTrackTileSize();

            if (ghostPathTileX == ghostTileX){
                // vertical
                if (ghostPathTileY > ghostTileY){
                    // moving down
                    //pathPointY = pathPointY - (game.getTrackTileSize()/2.0D);  // TODO BAse version
                    mDirection = "down";
                }
                else{
                    // moving up
                    //pathPointY = pathPointY + (game.getTrackTileSize()/2.0D);
                    mDirection = "up";

                }
            }

            if (ghostPathTileY == ghostTileY){
                // horizontal
                if (ghostPathTileX > ghostTileX){
                    // moving right
                    //pathPointX = pathPointX - (game.getTrackTileSize()/2.0D);
                    mDirection = "right";
                }
                else{
                    // moving left
                    //pathPointX = pathPointX + (game.getTrackTileSize()/2.0D);
                    mDirection = "left";
                }
            }


            distance = self.getDistanceTo(pathPointX,pathPointY);

            realMyTileX = ghostTileX;
            realMyTileY = ghostTileY;






        }







        // Collecting bonuses

        Bonus[] Bonuses = world.getBonuses();
        for (int i = 0; i < Bonuses.length; i++) {
            if ((world.getTick()>800)
                    ||(Bonuses[i].getType() == BonusType.PURE_SCORE)
                    || (Bonuses[i].getType() == BonusType.NITRO_BOOST)
                    || (Bonuses[i].getType() == BonusType.AMMO_CRATE)
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

        /*System.out.println(mDirection + "  from " +self.getX() + ", " + self.getY() + "  to  "
                +  pathPointX + ", " + pathPointY);
        System.out.println("myTileX" + myTileX);
        System.out.println("myTileY" + myTileY);
        System.out.println("ghostTileX" + ghostTileX);
        System.out.println("ghostTileY" + ghostTileY);
        System.out.println("ghostTileY" + self.getNextWaypointX());
        System.out.println("ghostTileY" + self.getNextWaypointY());
        System.out.println("pathTileX" +pathTileX);
        System.out.println("pathTileY" + pathTileY);
        System.out.println("ghostPathTileX" + ghostPathTileX);
        System.out.println("ghostPathTileY" + ghostPathTileY);
        System.out.println("_________________________________");*/


        if (mDirection == "unknown"){
            System.out.println("DIRECTION IS UNKNOWN");
            System.out.println("myTileX" + myTileX);
            System.out.println("myTileY" + myTileY);
            System.out.println("ghostTileX" + ghostTileX);
            System.out.println("ghostTileY" + ghostTileY);
            System.out.println("ghostTileY" + self.getNextWaypointX());
            System.out.println("ghostTileY" + self.getNextWaypointY());
            System.out.println("pathTileX" +pathTileX);
            System.out.println("pathTileY" + pathTileY);
            System.out.println("ghostPathTileX" + ghostPathTileX);
            System.out.println("ghostPathTileY" + ghostPathTileY);
            System.out.println("_________________________________");
            // Print visits
            for (int j = 0; j < vTilesCount; j++) {
                for (int i = 0; i < hTilesCount; i++) {
                    //System.out.print(visits[i][j] + "   ");
                }
                //System.out.print("\n");
            }
            //System.out.println("_________________________________");
            for (int j = 0; j < vTilesCount; j++) {
                for (int i = 0; i < hTilesCount; i++) {
                    //System.out.print(bisits[i][j] + "   ");
                }
                //System.out.print("\n");
            }
            //System.out.println("_________________________________");
        }


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
        MySettings mySettings = new MySettings();
        Context context = new Context(game, world, self, mySettings);
        PathPoint pathPoint = new PathPoint(pathPointX, pathPointY, context);
        double sideSpeedFactor = 8.0D;
        CarModel carModel = new CarModel(context);
        double perpendicularSpeedFactor = -1.0D*carModel.getPerpendicularSpeed(pathPoint)/sideSpeedFactor;

        double angleToPathPoint = self.getAngleTo(pathPointX, pathPointY);
        double wheelTurn = angleToPathPoint * wheelTurnFactor * angleMult + perpendicularSpeedFactor;


        if (startNoTurnsDistance<0){
            startNoTurnsDistance = self.getDistanceTo(pathPointX, pathPointY)-1250;
        }
        if (startNoTurns){
            passedStartDistance = passedStartDistance + self.getDistanceTo(posX, posY);
            if (passedStartDistance > startNoTurnsDistance) startNoTurns = false;
        }
        if (!startNoTurns) {
            move.setWheelTurn(wheelTurn);
        }








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


        if (speedModule > distance / brakeDistanceSpeedFactor ) {  // 50
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

        }

        double angleToChosenPoint = Math.abs(self.getAngleTo(pathPointX, pathPointY));
        if (    (distance > 3200.0D)
                && (world.getTick() > 200)
                && (angleToChosenPoint < (0.075D * PI))
                && (carModel.getPerpendicularSpeed(pathPoint)<3)){
            System.out.println("Using nitro, angle to "+pathPointX+ ", "+pathPointY+" is "+angleToChosenPoint);
            move.setUseNitro(true);
            //System.out.println(distance);
        }



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

///////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////
class MySettings{
    public double directorCarCircleVolume;
    public int directorLookTileDistance;
    MySettings(){
        //directorCarCircleVolume = newDirectorCarCircleVolume;
        //directorLookTileDistance = newDirectorLookTileDistance;
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
    /*public boolean circleCarCollisionInTicks(int ticks){
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
    }*/
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
        double scal = speedX*xm + speedY*ym;
        double bModule = hypot(xm, ym);
        double result = scal / bModule;

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
