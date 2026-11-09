import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import javax.swing.*;

/**
 * Coordinates warehouse role transitions and the shared application window.
 * <p>
 * WarehouseContext is a singleton that manages the current role (client, clerk,
 * or manager) and the shared JFrame that all roles draw on. It also handles
 * login and logout, and maintains a stack of previous roles for returning after
 * impersonation.
 * </p>
 *
 * @author Aj Johnson, Aaditya Malla. Chidera Izuora
 * @version 1.0
 */
public class WarehouseContext {
    // state codes (also the acting role)
    public static final int CLIENT = 0, CLERK = 1, MANAGER = 2, LOGIN = 3, WISHLIST = 4;

    // transition events (matrix columns)
    public static final int TO_CLIENT = 0, TO_CLERK = 1, TO_MANAGER = 2,
            TO_LOGIN = 3, TERMINATE = 4, TO_WISHLIST = 5;

    private static WarehouseContext instance;

    private final WarehouseState[] states = new WarehouseState[5]; // one per state code
    private final int[][] nextState = new int[5][6]; // [state][transition event]

    private int currentState = LOGIN;
    private String userID;

    private static JFrame frame; // the one shared window, reused by every state

    /**
     * A simple record to hold the state and user ID for returning after
     * impersonation.
     */
    private static final class ReturnFrame {
        final int state;
        final String userID;

        ReturnFrame(int state, String userID) {
            this.state = state;
            this.userID = userID;
        }
    }

    private final Deque<ReturnFrame> returnStack = new ArrayDeque<>();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private WarehouseContext() {
        instance = this; // break the init cycle: states' getInstance() calls us back

        states[CLIENT] = ClientState.getInstance();
        states[CLERK] = ClerkState.getInstance();
        states[MANAGER] = ManagerState.getInstance();
        states[LOGIN] = LoginState.getInstance();
        states[WISHLIST] = WishlistState.getInstance();

        for (int[] row : nextState)
            Arrays.fill(row, -2); // -2 == invalid by default

        nextState[LOGIN][TO_CLIENT] = CLIENT;
        nextState[LOGIN][TO_CLERK] = CLERK;
        nextState[LOGIN][TO_MANAGER] = MANAGER;
        nextState[LOGIN][TERMINATE] = -1;

        nextState[CLIENT][TO_CLERK] = CLERK; // logout back to clerk
        nextState[CLIENT][TO_LOGIN] = LOGIN; // logout out to login
        nextState[CLIENT][TO_WISHLIST] = WISHLIST; // perform wishlist operations

        nextState[WISHLIST][TO_CLIENT] = CLIENT; // back to the client menu

        nextState[CLERK][TO_CLIENT] = CLIENT; // become client
        nextState[CLERK][TO_MANAGER] = MANAGER; // logout back to manager
        nextState[CLERK][TO_LOGIN] = LOGIN; // logout out to login

        nextState[MANAGER][TO_CLERK] = CLERK; // become clerk
        nextState[MANAGER][TO_LOGIN] = LOGIN; // logout out to login

        currentState = LOGIN;

        frame = new JFrame("Warehouse");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setSize(520, 400);
        frame.setLocation(320, 200);
    }

    /**
     * Returns the singleton state-machine context.
     *
     * @return the shared context
     */
    public static WarehouseContext getInstance() {
        if (instance == null)
            instance = new WarehouseContext();
        return instance;
    }

    /** The window every state draws on. */
    /** @return the shared application window */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Applies a transition from the current state.
     *
     * @param transition the transition event to apply
     */
    public void changeState(int transition) {
        int next = nextState[currentState][transition];
        if (next == -1) {
            terminate();
            return;
        }
        if (next == -2) {
            JOptionPane.showMessageDialog(frame, "Invalid transition.");
            return;
        }
        currentState = next;
        states[currentState].run(); // the new state re-skins the shared frame
    }

    /**
     * Enters a role directly from login.
     *
     * @param role the role to enter
     * @param id   the client ID, or {@code null} for staff roles
     */
    public void login(int role, String id) {
        userID = id;
        changeState(roleToEvent(role));
    }

    /**
     * Enters another role while remembering the current role for logout.
     *
     * @param targetRole the role to enter
     * @param id         the ID associated with that role
     */
    public void become(int targetRole, String id) {
        returnStack.push(new ReturnFrame(currentState, userID));
        userID = id;
        changeState(roleToEvent(targetRole));
    }

    /**
     * Returns to the previous role, or to login when no role is being impersonated.
     */
    public void logout() {
        int target;
        String restoreID;
        if (!returnStack.isEmpty()) {
            ReturnFrame f = returnStack.pop();
            target = f.state;
            restoreID = f.userID;
        } else {
            target = LOGIN;
            restoreID = null;
        }

        userID = restoreID;
        changeState(roleToEvent(target));
    }

    /**
     * Terminates the application after printing a goodbye message.
     */
    public void terminate() {
        System.out.println("Goodbye.");
        System.exit(0);
    }

    /**
     * Returns the ID of the currently logged-in client, or {@code null} for staff.
     *
     * @return the ID of the currently logged-in client, or {@code null} for staff
     */
    public String getUser() {
        return userID;
    }

    /**
     * Returns the current role/state code.
     *
     * @return the current role/state code
     */
    public int getActingRole() {
        return currentState;
    }

    private int roleToEvent(int role) {
        switch (role) {
            case CLIENT:
                return TO_CLIENT;
            case CLERK:
                return TO_CLERK;
            case MANAGER:
                return TO_MANAGER;
            case LOGIN:
                return TO_LOGIN;
            case WISHLIST:
                return TO_WISHLIST;
            default:
                return TERMINATE;
        }
    }

    /**
     * Runs the active state.
     */
    public void process() {
        states[currentState].run();
    }

    public static void main(String[] args) {
        WarehouseContext.getInstance().process();
    }
}
