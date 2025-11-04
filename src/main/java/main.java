import gui.MainGui;

public class main {
	public static void main(String[] args) {
        // Reverted to always launch the GUI.
        // The bridge will be started as a thread from within MainGui if enabled.
		MainGui.launch(MainGui.class, args);
	}
}

