import java.util.*;

public class MyBot {
    static boolean unlogicalBoolean = true;
    static int round = 0;
    public static void firstRound (PlanetWars pw, int PlanetFactor, List<Planet> neutralPlanetsInvading){
        List<Fleet> myFleet = pw.MyFleets();
        boolean farAway;
        // Nehme neutrale Planeten ein, die sich lohnen eingenommen zu werden
        for (Planet d : pw.NeutralPlanets()) {
            boolean underAttack = false;
            // Wird der Planet schon angegriffen, greife ihn nicht an
            for (Fleet f : myFleet) {
                if (f.DestinationPlanet() == d.PlanetID() && f.NumShips() > 0)
                    underAttack = true;
            }
            if(!underAttack && (
                    (d.GrowthRate() >= 5 && d.NumShips() <= PlanetFactor*5) ||
                            (d.GrowthRate() >= 4 && d.NumShips() <= PlanetFactor*4) ||
                            (d.GrowthRate() >= 3 && d.NumShips() <= PlanetFactor*3) ||
                            (d.GrowthRate() >= 2 && d.NumShips() <= PlanetFactor*2) ||
                            (d.NumShips() <= PlanetFactor))) {
                farAway = false;
                int nearestDistance = Integer.MAX_VALUE;
                Planet source = null;
                for (Planet s : pw.MyPlanets()) {
                    int distance = pw.Distance(s.PlanetID(), d.PlanetID());
                    //Suche den nächsten meiner Planeten, der mehr Schiffe besitzt, als der neutrale Planet
                    if (distance < nearestDistance && s.NumShips() > d.NumShips()) {
                        nearestDistance = distance;
                        source = s;
                    }
                }
                if(source != null &&  d!= null) {
                    for (Planet enemy : pw.EnemyPlanets()) {
                        if (pw.Distance(enemy.PlanetID(), d.PlanetID()) < pw.Distance(source.PlanetID(), d.PlanetID()))
                            farAway = true;
                    }
                    boolean alreadyInvading = false;
                    for (Planet i : neutralPlanetsInvading){
                        if (d.PlanetID() == i.PlanetID())
                            alreadyInvading = true;
                    }
                    if (!farAway && source.NumShips()>d.NumShips() && !alreadyInvading) {
                        int numShips = d.NumShips() + 1;
                        sendFleet(pw,source,d,numShips);
                        neutralPlanetsInvading.add(d);
                    }
                }
            }
        }
    }

    public static int shipsForDefending (PlanetWars pw, Planet s, List<Planet> myPlanets, List<Fleet> myFleets, List<Fleet> enemyFleets, boolean support){
        // Werde ich angegriffen so sende nur so viele Schiffe, dass der eigene Planet nicht eingenommen wird
        int invadingEnemyShipWaves = 0;


        int shipsForDefendingMax = 0;
        int shipsForDefending = 0;
        for (Fleet f : enemyFleets){
            if (f.DestinationPlanet() == s.PlanetID() && f.NumShips() > 0){
                invadingEnemyShipWaves++;
            }
        }
        if (invadingEnemyShipWaves != 0) {
            int[] invadingEnemyShipAmounts = new int[invadingEnemyShipWaves];
            int[] invadingEnemyShipTurnsRemaining = new int[invadingEnemyShipWaves];
            int i = 0;
            for (Fleet f : enemyFleets) {
                if (f.DestinationPlanet() == s.PlanetID() && f.NumShips() > 0) {
                    invadingEnemyShipAmounts[i] = f.NumShips();
                    invadingEnemyShipTurnsRemaining[i] = f.TurnsRemaining();
                    i++;
                }
            }

            int maxGrowth = 0;
            int maxSupport = 0;
            for (int j = 0; j<invadingEnemyShipAmounts.length;j++){

                for (Fleet f : myFleets){
                    if (f.NumShips()>0 && f.DestinationPlanet()==s.PlanetID() && f.TurnsRemaining() <= invadingEnemyShipTurnsRemaining[j] && maxSupport<f.TurnsRemaining())
                        shipsForDefending-= f.NumShips();
                }
                maxSupport = invadingEnemyShipTurnsRemaining[j];

                if (maxGrowth<invadingEnemyShipTurnsRemaining[j]) {
                    shipsForDefending += invadingEnemyShipAmounts[j] - ((s.GrowthRate() * (invadingEnemyShipTurnsRemaining[j]- maxGrowth)));
                    maxGrowth = invadingEnemyShipTurnsRemaining[j];

                }
                else {
                    shipsForDefending += invadingEnemyShipAmounts[j];
                }
                if (s.NumShips()<shipsForDefending && support) {
                    shipsForDefending = support(pw, myPlanets, myFleets, enemyFleets, shipsForDefending-s.NumShips(),invadingEnemyShipTurnsRemaining[j] , s);
                }
                if (shipsForDefending > shipsForDefendingMax)
                    shipsForDefendingMax = shipsForDefending;
            }
        }
        return shipsForDefendingMax;
    }


    public static void sendFleet(PlanetWars pw, Planet p1, Planet p2, int amt) {
        if (amt > 0) {
      /*  for(int i = 0; i<amt; i++) {
//lel
            pw.IssueOrder(p1, p2, 1);
            p1.RemoveShips(1);
        } */
            pw.IssueOrder(p1, p2, amt);
            p1.RemoveShips(amt);
        }
    }

    public static void sendFleet(PlanetWars pw, int p1, int p2, int amt) {
        if (amt > 0){
            for(int i = 0; i<amt; i++) {

                pw.IssueOrder(p1, p2, 1);
                pw.GetPlanet(p1).RemoveShips(1);
            }
        }
    }

    // unterstütze eigene Planeten, die mehr Schiffe brauchen um die ankommende gegnerische Flotte zu verteidigen
    public static int support (PlanetWars pw, List<Planet> myPlanets, List<Fleet> myFleets, List<Fleet> enemyFleets, int shipsNeeded, int turnsLeft, Planet destination){

        int noHelpPossible = shipsNeeded;
        boolean helpSuccesful = false;

        List<Planet> myPlanetsReverse = new ArrayList<Planet>();
        for(int i = myPlanets.size()-1; i >= 0; i--){
            myPlanetsReverse.add(myPlanets.get(i));
        }

        for (Planet p : myPlanetsReverse){
            if (p.PlanetID()!=destination.PlanetID() && pw.Distance(p.PlanetID(), destination.PlanetID()) <= turnsLeft) {
                int AmountFleetsAvailable = 0;

                int shipsForDefending = shipsForDefending(pw,p,myPlanets,myFleets,enemyFleets,false);

                AmountFleetsAvailable= p.NumShips()- shipsForDefending;

                if (AmountFleetsAvailable>0) {
                    if (AmountFleetsAvailable > shipsNeeded) {
                        helpSuccesful = true;
                    } else {
                        shipsNeeded = shipsNeeded - AmountFleetsAvailable;
                    }
                }
            }
        }

        if (helpSuccesful) {
            shipsNeeded = noHelpPossible;
            for (Planet p : myPlanetsReverse){
                if (p.PlanetID()!=destination.PlanetID() && pw.Distance(p.PlanetID(), destination.PlanetID()) <= turnsLeft) {
                    int AmountFleetsAvailable = 0;

                    int shipsForDefending = shipsForDefending(pw,p,myPlanets,myFleets,enemyFleets,false);
                    AmountFleetsAvailable= p.NumShips()- shipsForDefending;

                    if (AmountFleetsAvailable>0) {
                        if (AmountFleetsAvailable > shipsNeeded) {
                            sendFleet(pw,p,destination,shipsNeeded);
                            return destination.NumShips();
                        } else {
                            sendFleet(pw,p,destination,AmountFleetsAvailable);
                            shipsNeeded = shipsNeeded - AmountFleetsAvailable;
                        }
                    }
                }
            }

        }

        return noHelpPossible+destination.NumShips();
    }



    public static void DoTurn(PlanetWars pw) {
        round++;
        List<Planet> myPlanets = pw.MyPlanets();
        List<Planet> enemyPlanets = pw.EnemyPlanets();
        List<Planet> neutralPlanetsInvading = new ArrayList<Planet>();
        List<Fleet> myFleets = pw.MyFleets();
        List<Fleet> enemyFleets = pw.EnemyFleets();
        boolean firstRound = true;

        Collections.sort(enemyFleets, new Comparator<Fleet>() {
            @Override
            public int compare(Fleet first, Fleet second) {
                return first.TurnsRemaining() - second.TurnsRemaining();
            }
        });

        Collections.sort(myFleets, new Comparator<Fleet>() {
            @Override
            public int compare(Fleet first, Fleet second) {
                return first.TurnsRemaining() - second.TurnsRemaining();
            }
        });

        Collections.sort(myPlanets, new Comparator<Planet>() {
            @Override
            public int compare(Planet second, Planet first) {
                return second.GrowthRate() - first.GrowthRate();
            }
        });

        // Gehören alle Planeten mir, tue nichts
        if (pw.NotMyPlanets().size() == 0 )
            return;
        //Besitze ich keinen Planeten, tue nichts
        if (myPlanets.size() == 0 )
            return;
        if (round == 1)
            firstRound = true;
        else
            firstRound = false;



        if (firstRound) {
            for (int i = 1; i<9; i++) {
                firstRound(pw, i, neutralPlanetsInvading);
            }
            for (Planet p : myPlanets) {
                if (p.NumShips() >= 80)
                    firstRound(pw, 9, neutralPlanetsInvading);
            }
            for (Planet p : myPlanets) {
                if (p.NumShips() >= 80)
                    firstRound(pw, 10, neutralPlanetsInvading);
            }
            for (Planet p : myPlanets) {
                if (p.NumShips() >= 80)
                    firstRound(pw, 11, neutralPlanetsInvading);
            }
            for (Planet p : myPlanets) {
                if (p.NumShips() >= 80)
                    firstRound(pw, 12, neutralPlanetsInvading);
            }
        } else {
            for(Planet s : pw.NeutralPlanets()){
                for(Fleet f : myFleets){
                    if(s.PlanetID() == f.DestinationPlanet()){
                        boolean alreadyInvading = false;
                        for(Planet n : neutralPlanetsInvading){
                            if(n.PlanetID() == s.PlanetID()){
                                alreadyInvading = true;
                            }
                        }
                        if(!alreadyInvading){
                            neutralPlanetsInvading.add(s);
                        }
                    }
                }
            }
        }

        List<Planet> myPlanetsShipsAmountsDesc = myPlanets;

        Collections.sort(myPlanetsShipsAmountsDesc, new Comparator<Planet>() {
            @Override
            public int compare(Planet second, Planet first) {
                return second.NumShips() - first.NumShips();
            }
        });


        List<Planet> neutralPlanetsInvadingByEnemy = new ArrayList<>();
        for (Fleet f : enemyFleets){
            if (pw.GetPlanet(f.DestinationPlanet()).Owner()==0)
                for (Planet p : myPlanets) {
                    boolean alreadyAttacking = false;
                    if (p.NumShips()>pw.GetPlanet(f.DestinationPlanet()).GrowthRate()+1 && pw.Distance(p.PlanetID(),f.DestinationPlanet())<pw.Distance(f.DestinationPlanet(),f.SourcePlanet()) && !neutralPlanetsInvadingByEnemy.contains(pw.GetPlanet(f.DestinationPlanet())))
                        for(Fleet f2 : myFleets){
                            if(!(f2.DestinationPlanet() == f.DestinationPlanet())){
                                alreadyAttacking = true;
                            }
                        }
                    if(!alreadyAttacking){
                        neutralPlanetsInvadingByEnemy.add(pw.GetPlanet(f.DestinationPlanet()));
                    }

                }
        }

        for (Planet p : neutralPlanetsInvadingByEnemy){

            for (Planet s : myPlanetsShipsAmountsDesc){
                for (Fleet f : enemyFleets){
                    if (f.DestinationPlanet()== p.PlanetID()){
                        if (pw.Distance(p.PlanetID(),s.PlanetID())-f.TurnsRemaining()==1) {
                            int shipsForDefending=shipsForDefending(pw,s,myPlanets,myFleets,enemyFleets,false);
                            if (s.NumShips()>0 && (s.NumShips()-shipsForDefending)>p.GrowthRate()+1) {
                                pw.IssueOrder(s, p, s.NumShips()-shipsForDefending);
                                s.RemoveShips(s.NumShips()-shipsForDefending);
                            }
                        }
                    }
                }
            }
        }

        for(Planet s : neutralPlanetsInvading){
            boolean enemyEarlier = false;
            int turnsTillArrivalMyself = Integer.MAX_VALUE;
            int turnsTillArrivalEnemy = Integer.MAX_VALUE;

            for(Fleet f : myFleets){
                if (f.DestinationPlanet() == s.PlanetID())
                    if (f.TurnsRemaining()<turnsTillArrivalMyself)
                        turnsTillArrivalMyself = f.TurnsRemaining();
            }

            for(Fleet f : enemyFleets){
                if (f.DestinationPlanet() == s.PlanetID())
                    if (f.TurnsRemaining()<turnsTillArrivalEnemy)
                        turnsTillArrivalEnemy = f.TurnsRemaining();
            }
            enemyEarlier = (turnsTillArrivalEnemy<turnsTillArrivalMyself);


            for(Fleet f : enemyFleets){
                if(f.DestinationPlanet() == s.PlanetID() && !enemyEarlier){
                    int shipsForDefendingNeutralPlanet = shipsForDefendingNeutralPlanet (pw,s);
                    if (shipsForDefendingNeutralPlanet>1)
                        defendInvadingPlanet(pw, s ,myPlanetsShipsAmountsDesc, shipsForDefendingNeutralPlanet);
                }

            }
        }


        //Angriff
        for (Planet s : myPlanets) {
            // wieviele Schiffe brauche ich zur Verteidigung gegen Angriffe
            int shipsForDefending = shipsForDefending(pw, s, myPlanets, myFleets, enemyFleets, true);
            // Suche den nächsten Planeten des Gegners und schicke alle Schiffe hin, falls er nicht von von einer anderen Flotte angegriffen wird
            int nearestDistance = Integer.MAX_VALUE;
            Planet destination = null;

            for (Planet d : enemyPlanets){
                boolean underAttack = false;
                for (Fleet f : myFleets) {
                    if (f.DestinationPlanet() == d.PlanetID() && f.NumShips()>0 && f.SourcePlanet() == s.PlanetID())
                        underAttack = true;
                }
                int distance = pw.Distance(s.PlanetID(), d.PlanetID());
                if ( distance < nearestDistance && !underAttack){
                    nearestDistance = distance;
                    destination = d;
                }
            }
            if (destination != null && round>1) {
                int numShips = s.NumShips();
                if (numShips>shipsForDefending) {
                    sendFleet(pw,s,destination,numShips-shipsForDefending);
                }
            }
        }
        /*
        // troll
        boolean trollmode = false;
        if (trollmode){
            for (Planet s : myPlanets) {
                for (Planet d : pw.NotMyPlanets()){
                    pw.IssueOrder(s, d, 0);
                }

            }
        }*/

    }

    public static int shipsForDefendingNeutralPlanet (PlanetWars pw, Planet target){
        // Werde ich angegriffen so sende nur so viele Schiffe, dass der eigene Planet nicht eingenommen wird
        int invadingEnemyShipWaves = 0;
        int shipsForDefendingMax = 0;
        int shipsForDefending = 0;
        for (Fleet f : pw.EnemyFleets()){
            if (f.DestinationPlanet() == target.PlanetID() && f.NumShips() > 0){
                invadingEnemyShipWaves++;
            }
        }
        if (invadingEnemyShipWaves != 0) {
            int[] invadingEnemyShipAmounts = new int[invadingEnemyShipWaves];
            int[] invadingEnemyShipTurnsRemaining = new int[invadingEnemyShipWaves];
            int i = 0;
            for (Fleet f : pw.EnemyFleets()) {
                if (f.DestinationPlanet() == target.PlanetID() && f.NumShips() > 0) {
                    invadingEnemyShipAmounts[i] = f.NumShips();
                    invadingEnemyShipTurnsRemaining[i] = f.TurnsRemaining();
                    i++;
                }
            }

            int TurnsTillFirstFleetArrives = Integer.MAX_VALUE;

            for (Fleet f : pw.MyFleets()){
                if (f.DestinationPlanet() == target.PlanetID()){
                    if (f.TurnsRemaining() < TurnsTillFirstFleetArrives)
                        TurnsTillFirstFleetArrives = f.TurnsRemaining();
                }

            }

            int maxGrowth = TurnsTillFirstFleetArrives;
            int maxSupport = 0;
            boolean neutralPlanetShipsDestroyed = false;
            for (int j = 0; j<invadingEnemyShipAmounts.length;j++){

                for (Fleet f : pw.MyFleets()){
                    if (f.NumShips()>0 && f.DestinationPlanet()==target.PlanetID() && f.TurnsRemaining() <= invadingEnemyShipTurnsRemaining[j] && maxSupport<f.TurnsRemaining()) {
                        shipsForDefending -= f.NumShips();
                        if (!neutralPlanetShipsDestroyed){
                            shipsForDefending+= f.NumShips()-1;
                            neutralPlanetShipsDestroyed = true;
                        }

                    }
                }
                maxSupport = invadingEnemyShipTurnsRemaining[j];

                if (maxGrowth<invadingEnemyShipTurnsRemaining[j]) {
                    shipsForDefending += invadingEnemyShipAmounts[j] - ((target.GrowthRate() * (invadingEnemyShipTurnsRemaining[j]- maxGrowth)));
                    maxGrowth = invadingEnemyShipTurnsRemaining[j];

                }
                else {
                    shipsForDefending += invadingEnemyShipAmounts[j];
                }
                if (shipsForDefending > shipsForDefendingMax)
                    shipsForDefendingMax = shipsForDefending;
            }
        }
        return shipsForDefendingMax;
    }

    public static void defendInvadingPlanet (PlanetWars pw, Planet target,List <Planet> myPlanetsShipsAmountsDesc, int shipsForDefendingNeutralPlanet){
        if (shipsForDefendingNeutralPlanet>1)
            for (Planet p : myPlanetsShipsAmountsDesc){
                if (p.NumShips() >= shipsForDefendingNeutralPlanet-1) {
                    sendFleet(pw,p,target,shipsForDefendingNeutralPlanet-1);
                    return;
                }
                else {
                    if (p.NumShips()>0) {
                        sendFleet(pw,p,target,p.NumShips());
                        shipsForDefendingNeutralPlanet -= p.NumShips();
                    }
                }
            }
    }



    public static void main(String[] args) {
        String line = "";
        String message = "";
        int c;
        try {
            while ((c = System.in.read()) >= 0) {
                switch (c) {
                    case '\n':
                        if (line.equals("go")) {
                            PlanetWars pw = new PlanetWars(message);
                            DoTurn(pw);
                            pw.FinishTurn();
                            message = "";
                        } else {
                            message += line + "\n";
                        }
                        line = "";
                        break;
                    default:
                        line += (char)c;
                        break;
                }
            }
        } catch (Exception e) {
            // Owned.
        }
    }
}
