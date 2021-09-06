package HodNottBot;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.HashMap;

enum HodNottBot_TreeConfig {
    AUTOMATIC_TREE,
    SEPARATED_TREE,
    PERFORMANCE_TREE
}

public class Agent extends AbstractPlayer {
    private HodNottBot_TreeConfig treeConfig = HodNottBot_TreeConfig.SEPARATED_TREE;
    private boolean checkSubAgentsEachTurn = true;
    private boolean verbose = false;

    private boolean hasResources;
    private boolean avatarHasResources;
    private int resourceTypes;
    private int numAvatarResources;
    private int numNPCs;
    private int NPCTypes;
    private int area;
    private int blockSize;
    private int numImmovable;
    private int numMovable;
    private boolean hasPortals;
    private int portalTypes;
    private boolean verticality;
    private boolean useAvailable;
    private boolean healthAvailable;

    private AbstractPlayer subAgent;
    private HodNottBot.asd592.Agent asd592Component;
    private HodNottBot.NovelTS.Agent NovelTSComponent;
    private HodNottBot.Number27.Agent Number27Component;
    private HodNottBot.UrbanSk.Agent UrbanSkComponent;
    private HodNottBot.adrienctx.Agent adrienctxComponent;
    private HodNottBot.RegyN.Agent RegyNComponent;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        this.treeConfig = HodNottBot_TreeConfig.SEPARATED_TREE; // this determines which decision tree will be used
        this.checkSubAgentsEachTurn = true; // toggle for switching controllers during run
        this.verbose = false;

        // these components are only initialised if the sub-agent is relevant to the decision tree being used
        this.asd592Component = new HodNottBot.asd592.Agent(so, elapsedTimer);
        this.NovelTSComponent = new HodNottBot.NovelTS.Agent(so, elapsedTimer);
        this.Number27Component = new HodNottBot.Number27.Agent(so, elapsedTimer);
        if ((this.treeConfig == HodNottBot_TreeConfig.SEPARATED_TREE) || (this.treeConfig == HodNottBot_TreeConfig.PERFORMANCE_TREE)) {
            this.UrbanSkComponent = new HodNottBot.UrbanSk.Agent(so, elapsedTimer);
        }
        if (this.treeConfig == HodNottBot_TreeConfig.PERFORMANCE_TREE) {
            this.adrienctxComponent = new HodNottBot.adrienctx.Agent(so, elapsedTimer);
            this.RegyNComponent = new HodNottBot.RegyN.Agent(so, elapsedTimer);
        }

        // if the controller is configured to not pick new sub-agents each turn, it will pick sub-agents here
        if (!checkSubAgentsEachTurn) {
            pickSubAgent(so);
        }
    }

    @Override
    public Types.ACTIONS act(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        Types.ACTIONS chosenAction = Types.ACTIONS.ACTION_NIL;

        // if the controller is configured to pick sub-agents each turn, it will do so here
        if (checkSubAgentsEachTurn) {
            pickSubAgent(so);
        }

        chosenAction = this.subAgent.act(so, elapsedTimer);
        return chosenAction;
    }

    private void pickSubAgent(StateObservation so) {
        // updates the game features and calls the chosen decision tree to pick the next sub-agent
        this.getFeatures(so);

        if (this.treeConfig == HodNottBot_TreeConfig.AUTOMATIC_TREE) {
            this.pickControllerAutomaticDecisionTree();
        } else if (this.treeConfig == HodNottBot_TreeConfig.SEPARATED_TREE) {
            this.pickControllerSeparatedDecisionTree();
        } else {
            this.pickControllerPerformanceDecisionTree();
        }

        if (this.verbose && (so.getGameTick() % 100 == 0)) {
            printChosenController();
        }
    }

    private void pickControllerAutomaticDecisionTree() {
        int genre;

        // this is a generated decision tree made of conditional statements (VERY MESSY)
        if (this.hasPortals){ // x11 >= 0.5
            if (this.useAvailable){ // x14 >= 0.5
                if (this.hasResources) { // x1 >= 0.5
                    if (this.resourceTypes >= 2.5) { // x3 >= 2.5
                        genre = 4;
                    } else { // x3 < 2.5
                        genre = 3;
                    }
                } else { // x1 < 0.5
                    if (this.numNPCs >= 8.5) { // x5 >= 8.5
                        genre = 4;
                    } else { // x5 < 8.5
                        genre = 2;
                    }
                }
            } else { // x14 < 0.5
                genre = 1;
            }
        } else { // x11 < 0.5
            if (this.useAvailable){ // x14 >= 0.5
                if (this.hasResources) { // x1 >= 0.5
                    genre = 6;
                } else { // x1 < 0.5
                    genre = 5;
                }
            } else { // x14 < 0.5
                if (this.hasResources) { // x1 >= 0.5
                    genre = 6;
                } else { // x1 < 0.5
                    genre = 7;
                }
            }
        }

        // determines which sub-agent is picked based on the genre
        switch (genre) {
            case 1:
            case 6:
            case 7:
                this.subAgent = this.asd592Component;
                break;
            case 2:
            case 3:
                this.subAgent = this.NovelTSComponent;
                break;
            case 4:
            case 5:
            default:
                this.subAgent = this.Number27Component;
                break;
        }
    }

    private void pickControllerSeparatedDecisionTree() {
        int genre;

        // this is a generated decision tree made of conditional statements (VERY MESSY)
        if (this.hasPortals){ // x11 >= 0.5
            if (this.useAvailable){ // x14 >= 0.5
                if (this.hasResources) { // x1 >= 0.5
                    if (this.healthAvailable) { // x15 >= 0.5
                        genre = 3;
                    } else { // x15 < 0.5
                        genre = 4;
                    }
                } else { // x1 < 0.5
                    if (this.numImmovable >= 357) { // x9 >= 357
                        if (this.blockSize >= 35) { // x8 >= 35
                            genre = 2;
                        } else { // x8 < 35
                            if (this.portalTypes >= 1.5) { // x12 >= 1.5
                                genre = 0;
                            } else { // x12 < 1.5
                                genre = 2;
                            }
                        }
                    } else { // x9 < 357
                        if (this.area >= 257194) { // x7 >= 257194
                            genre = 2;
                        } else { // x7 < 257194
                            if (this.numNPCs >= 2.5) { // x5 >= 2.5
                                genre = 2;
                            } else { // x5 < 2.5
                                genre = 0;
                            }
                        }
                    }
                }
            } else { // x14 < 0.5
                if (this.healthAvailable) { // x15 >= 0.5
                    genre = 3;
                } else { // x15 < 0.5
                    if (this.numImmovable >= 420) { // x9 >= 420
                        if (this.blockSize >= 27.5) { // x8 >= 27.5
                            if (this.hasResources) { // x1 >= 0.5
                                genre = 4;
                            } else { // x1 < 0.5
                                genre = 1;
                            }
                        } else { // x8 < 27.5
                            genre = 0;
                        }
                    } else { // x9 < 420
                        if (this.numMovable >= 5.5) { // x10 >= 5.5
                            if (this.numMovable >= 10.5) { // x10 >= 10.5
                                if (this.area >= 306864) { // x7 >= 306864
                                    genre = 1;
                                } else { // x7 < 306864
                                    genre = 0;
                                }
                            } else { // x10 < 10.5
                                if (this.numNPCs >= 1) { // x5 >= 1
                                    genre = 1;
                                } else { // x5 < 1
                                    if (this.area >= 170496) { // x7 >= 170496
                                        genre = 0;
                                    } else { // x7 < 170496
                                        genre = 1;
                                    }
                                }
                            }
                        } else { // x10 < 5.5
                            genre = 1;
                        }
                    }
                }
            }
        } else { // x11 < 0.5
            if (this.useAvailable){ // x14 >= 0.5
                if (this.numNPCs >= 0.5) { // x5 >= 0.5
                    genre = 7;
                } else { // x5 < 0.5
                    if (this.numImmovable >= 172.5) { // x9 >= 172.5
                        genre = 0;
                    } else { // x9 < 172.5
                        genre = 7;
                    }
                }
            } else { // x14 < 0.5
                if (this.healthAvailable) { // x15 >= 0.5
                    if (this.area >= 279596) { // x7 >= 279596
                        genre = 5;
                    } else { // x7 < 279596
                        genre = 0;
                    }
                } else { // x15 < 0.5
                    if (this.area >= 430479) { // x7 >= 430479
                        if (this.numImmovable >= 81.5) { //x9 >= 81.5
                            genre = 6;
                        } else { // x9 < 81.5
                            genre = 0;
                        }
                    } else { // x7 < 430479
                        if (this.blockSize >= 26) { // x8 >= 26
                            if (this.area >= 338873) { // x7 >= 338873
                                if (this.numMovable >= 7.5) { // x10 >= 7.5
                                    genre = 0;
                                } else { // x10 < 7.5
                                    genre = 6;
                                }
                            } else { // x7 < 338873
                                genre = 6;
                            }
                        } else { // x8 < 26
                            genre = 0;
                        }
                    }
                }
            }
        }

        // determines which sub-agent is picked based on the genre
        switch (genre) {
            case 0:
            case 3:
            case 6:
                this.subAgent = this.asd592Component;
                break;
            case 2:
            case 4:
                this.subAgent = this.NovelTSComponent;
                break;
            case 5:
                this.subAgent = this.UrbanSkComponent;
                break;
            case 1:
            case 7:
            default:
                this.subAgent = this.Number27Component;
                break;
        }
    }

    private void pickControllerPerformanceDecisionTree() {
        int genre;

        // this is a generated decision tree made of conditional statements (VERY MESSY)
        if (this.numNPCs >= 0.5) { // x5 >= 0.5
            if (this.numMovable >= 4) { // x10 >= 4
                if (this.useAvailable) { // x14 >= 0.5
                    genre = 2;
                } else { // x14 < 0.5
                    if (this.area >= 307580) { // x7 >= 307580
                        if (this.numImmovable >= 237.5) { // x9 >= 237.5
                            genre = 2;
                        } else { // x9 < 237.5
                            genre = 1;
                        }
                    } else { // x7 < 307580
                        genre = 0;
                    }
                }
            } else { // x10 < 4
                if (this.numImmovable >= 489) { // x9 >= 489
                    genre = 1;
                } else { // x9 < 489
                    if (this.numImmovable >= 104) { // x9 >= 104
                        if (this.useAvailable) { // x14 >= 0.5
                            if (this.NPCTypes >= 2.5) { // x6 >= 2.5
                                genre = 2;
                            } else { // x6 < 2.5
                                if (this.numImmovable >= 287.5) { // x9 >= 287.5
                                    genre = 3;
                                } else { // x9 < 287.5
                                    genre = 4;
                                }
                            }
                        } else { // x14 < 0.5
                            if (this.blockSize >= 30.5) { // x8 >= 30.5
                                genre = 4;
                            } else { // x8 < 30.5
                                genre = 3;
                            }
                        }
                    } else { // x9 < 104
                        if (this.area >= 371579) { // x7 >= 371579
                            genre = 3;
                        } else { // x7 < 371579
                            if (this.blockSize >= 29) { // x8 >= 29
                                genre = 2;
                            } else { // x8 < 29
                                genre = 3;
                            }
                        }
                    }
                }
            }
        } else { // x5 < 0.5
            if (this.area >= 556413) { // x7 >= 556413
                if (this.blockSize >= 46) { // x8 >= 46
                    genre = 4;
                } else { // x8 < 46
                    genre = 2;
                }
            } else { // x7 < 556413
                if (this.numMovable >= 0.5) { // x10 >= 0.5
                    if (this.numMovable >= 1.5) { // x10 >= 1.5
                        if (this.area >= 357950) { // x7 >= 357950
                            if (this.numMovable >= 10.5) { // x10 >= 10.5
                                if (this.numImmovable >= 89) { // x9 >= 89
                                    genre = 6;
                                } else { // x9 < 89
                                    if (this.area >= 379429) { // x7 >= 379429
                                        genre = 5;
                                    } else { // x7 < 379429
                                        genre = 0;
                                    }
                                }
                            } else { // x10 < 10.5
                                if (this.blockSize >= 31) { // x8 >= 31
                                    if (this.blockSize >= 70.5) { // x8 >= 70.5
                                        genre = 4;
                                    } else { // x8 < 70.5
                                        if (this.portalTypes >= 2.5) { // x12 >= 2.5
                                            genre = 4;
                                        } else { // x12 < 2.5
                                            genre = 0;
                                        }
                                    }
                                } else { // x8 < 31
                                    genre = 4;
                                }
                            }
                        } else { // x7 < 357950
                            if (this.hasPortals) { // x11 >= 0.5
                                if (this.area >= 255136) { // x7 >= 255136
                                    genre = 4;
                                } else { // x7 < 255136
                                    if (this.area >= 152171) { // x7 >= 152171
                                        genre = 0;
                                    } else { // x7 < 152171
                                        genre = 3;
                                    }
                                }
                            } else { // x11 < 0.5
                                if (this.blockSize >= 26.5) { // x8 >= 26.5
                                    if (this.hasResources) { // x1 >= 0.5
                                        genre = 6;
                                    } else { // x1 < 0.5
                                        if (this.numImmovable >= 78.5) { // x9 >= 78.5
                                            genre = 3;
                                        } else { // x9 < 78.5
                                            if (this.numMovable >= 17.5) { // x10 >= 17.5
                                                genre = 4;
                                            } else { // x10 < 17.5
                                                genre = 3;
                                            }
                                        }
                                    }
                                } else { // x8 < 26.5
                                    genre = 0;
                                }
                            }
                        }
                    } else { // x10 < 1.5
                        genre = 4;
                    }
                } else { // x10 < 0.5
                    if (this.hasPortals) { // x11 >= 0.5
                        if (this.blockSize >= 23) { // x8 >= 23
                            if (this.area >= 407900) { // x7 >= 407900
                                if (this.portalTypes >= 1.5) { // x12 >= 1.5
                                    genre = 3;
                                } else { // x12 < 1.5
                                    genre = 4;
                                }
                            } else { // x7 < 407900
                                if (this.area >= 228915) { // x7 >= 228915
                                    if (this.numImmovable >= 301.5) { // x9 >= 301.5
                                        if (this.area >= 354882) { // x7 >= 354882
                                            genre = 5;
                                        } else { // x7 < 354882
                                            if (this.area >= 318882) { // x7 >= 318882
                                                genre = 3;
                                            } else { // x7 < 318882
                                                if (this.useAvailable) { // x14 >= 0.5
                                                    genre = 2;
                                                } else { // x14 < 0.5
                                                    genre = 6;
                                                }
                                            }
                                        }
                                    } else { // x9 < 301.5
                                        if (this.blockSize >= 34) { // x8 >= 34
                                            if (this.area >= 364338) { // x7 >= 364338
                                                if (this.numImmovable >= 173) { // x9 >= 173
                                                    genre = 1;
                                                } else { // x9 < 173
                                                    genre = 5;
                                                }
                                            } else { // x7 < 364338
                                                if (this.healthAvailable) { // x15 >= 0.5
                                                    genre = 5;
                                                } else { // x15 < 0.5
                                                    genre = 2;
                                                }
                                            }
                                        } else { // x8 < 34
                                            if (this.area >= 347250) { // x7 >= 347250
                                                genre = 4;
                                            } else { // x7 < 347250
                                                genre = 5;
                                            }
                                        }
                                    }
                                } else { // x7 < 228915
                                    genre = 2;
                                }
                            }
                        } else { // x8 < 23
                            genre = 0;
                        }
                    } else { // x11 < 0.5
                        if (this.numImmovable >= 158) { // x9 >= 158
                            genre = 0;
                        } else { // x9 < 158
                            if (this.area >= 430479) { // x7 >= 430479
                                genre = 0;
                            } else { // x7 < 430479
                                if (this.area >= 224064) { // x7 >= 224064
                                    if (this.area >= 334848) { // x7 >= 334848
                                        genre = 1;
                                    } else { // x7 < 334848
                                        genre = 2;
                                    }
                                } else { // x7 < 224064
                                    genre = 1;
                                }
                            }
                        }
                    }
                }
            }
        }

        // determines which sub-agent is picked based on the genre
        switch (genre) {
            case 1:
                this.subAgent = this.RegyNComponent;
                break;
            case 2:
                this.subAgent = this.NovelTSComponent;
                break;
            case 3:
                this.subAgent = this.asd592Component;
                break;
            case 5:
                this.subAgent = this.UrbanSkComponent;
                break;
            case 6:
                this.subAgent = this.adrienctxComponent;
                break;
            case 0:
            case 4:
            default:
                this.subAgent = this.Number27Component;
                break;
        }
    }

    private void getFeatures(StateObservation so) {
        // retrieves the game features from the given state
        ArrayList<Observation>[] resourcePositions = so.getResourcesPositions();

        this.hasResources = resourcePositions != null;

        if(this.hasResources) {
            this.resourceTypes = resourcePositions.length;
        } else{
            this.resourceTypes = 0;
        }

        HashMap<Integer, Integer> avatarResources = so.getAvatarResources(); // hashmap with (itemID: amount) key pairs
        this.avatarHasResources = !avatarResources.isEmpty();
        this.numAvatarResources = avatarResources.keySet().toArray().length;

        ArrayList<Observation>[] NPCPositions = so.getNPCPositions();
        this.NPCTypes = 0;
        this.numNPCs = 0;
        if (NPCPositions != null) {
            for (ArrayList<Observation> NPCType : NPCPositions) {
                this.NPCTypes += 1;
                this.numNPCs += NPCType.size();
            }
        }

        this.area = (int) Math.round(so.getWorldDimension().getWidth() * so.getWorldDimension().getHeight());
        this.blockSize = so.getBlockSize();

        ArrayList<Observation>[] immovablePositions = so.getImmovablePositions();
        this.numImmovable = 0;
        if (immovablePositions != null) {
            for (ArrayList<Observation> immoveableThings : immovablePositions) {
                this.numImmovable += immoveableThings.size();
            }
        }

        ArrayList<Observation>[] movablePositions = so.getMovablePositions();
        this.numMovable = 0;
        if (movablePositions != null) {
            for (ArrayList<Observation> moveableType : movablePositions) {
                this.numMovable += moveableType.size();
            }
        }

        ArrayList<Observation>[] portalPositions = so.getPortalsPositions();
        this.hasPortals = (portalPositions != null);
        if(this.hasPortals) {
            this.portalTypes = portalPositions.length;
        } else{
            this.portalTypes = 0;
        }

        this.verticality = so.getAvailableActions().contains(Types.ACTIONS.ACTION_UP) || so.getAvailableActions().contains(Types.ACTIONS.ACTION_DOWN);
        this.useAvailable = so.getAvailableActions().contains(Types.ACTIONS.ACTION_USE);
        this.healthAvailable = so.getAvatarHealthPoints() > 0;
    }

    private void printChosenController() {
        if (this.subAgent == this.adrienctxComponent) {
            System.out.println("adrienctx chosen");
        } else if (this.subAgent == this.asd592Component) {
            System.out.println("asd592 chosen");
        } else if (this.subAgent == this.NovelTSComponent) {
            System.out.println("NovelTS chosen");
        } else if (this.subAgent == this.Number27Component) {
            System.out.println("Number27 chosen");
        } else if (this.subAgent == this.RegyNComponent) {
            System.out.println("RegyN chosen");
        } else {
            System.out.println("UrbanSk chosen");
        }
    }
}
