package org.TrackIt;

import mpi.MPI;
import org.TrackIt.panels.MenuPanel;
import org.TrackIt.trackItSwing.Manager;
import org.TrackIt.videoPlayer.VideoPlayer;

import javax.swing.*;

public class Main extends JFrame {
    public Main() {
        Manager manager = new Manager(this);

        initialization();

        MenuPanel menuPanel = new MenuPanel();
        add(menuPanel);

        manager.refresh(this);
    }

    private void initialization() {
        setTitle("TrackIt");
        setSize(800, 600);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setVisible(true);
    }

    public static void main(String[] args) {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();

        // We want to start the UI only for the Master process.
        if(rank == 0) new Main();

        // All other processes should just do their workerLoops.
        else {
            VideoPlayer.workerLoop();
            MPI.Finalize();
        }
    }
}