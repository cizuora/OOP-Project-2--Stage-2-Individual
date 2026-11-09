import java.awt.*;
import java.util.Scanner;
import javax.swing.*;

/**
 * Base class for the role-specific warehouse screens.
 * <p>
 * WarehouseState is an abstract class that defines the common behavior and
 * interface for different warehouse roles (client, clerk, manager, login). It
 * provides methods for preparing and displaying the shared application window,
 * handling user input, and managing the output area.
 * </p>
 *
 * @author Aj Johnson, Aaditya Malla. Chidera Izuora
 * @version 1.0
 */
public abstract class WarehouseState {
    /** The one FSM context, shared by all states. */
    protected static final WarehouseContext context = WarehouseContext.getInstance();

    /** Shared console reader, for states not yet converted to GUI. */
    protected static final Scanner in = new Scanner(System.in);

    protected final JTextArea output = new JTextArea(16, 44);

    /** Builds this state's screen or runs its menu loop. */
    public abstract void run();

    /**
     * Clears the shared window and returns it, ready for this state to add buttons.
     *
     * @return the prepared frame
     */
    protected JFrame prepareFrame() {
        clearOutput();
        JFrame frame = context.getFrame();
        frame.getContentPane().removeAll();
        frame.getContentPane().setLayout(new FlowLayout());
        return frame;
    }

    /**
     * Shows the shared window after this state has added its components.
     *
     * @param frame the frame to show
     */
    protected void showFrame(JFrame frame) {
        frame.setVisible(true);
        frame.revalidate();
        frame.repaint();
        frame.toFront();
    }

    /**
     * Prompts the user for a command.
     *
     * @param prompt the prompt to display
     * @return the user's command
     */
    protected int getCommand(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(in.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    /**
     * Prompts the user for a token (string).
     *
     * @param prompt the prompt to display
     * @return the user's input
     */
    protected String getToken(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    /**
     * Prompts the user for a yes/no answer.
     *
     * @param prompt the prompt to display
     * @return true if the user answered yes, false otherwise
     */
    protected boolean yesOrNo(String prompt) {
        System.out.print(prompt + " (y/n): ");
        return in.nextLine().trim().equalsIgnoreCase("y");
    }

    /**
     * Prompt for a required text field: re-asks on blank, returns null if
     * cancelled.
     *
     * @param label the label for the input field
     * @return the user's input, or null if cancelled
     */
    protected String promptRequired(String label) {
        while (true) {
            String value = JOptionPane.showInputDialog(context.getFrame(), label);
            if (value == null)
                return null; // user cancelled
            value = value.trim();
            if (!value.isEmpty())
                return value;
            JOptionPane.showMessageDialog(context.getFrame(), label + " cannot be empty.");
        }
    }

    /**
     * Prompt for a whole number: re-asks on bad input, returns null if cancelled.
     *
     * @param label the label for the input field
     * @return the user's input, or null if cancelled
     */
    protected Integer promptInt(String label) {
        while (true) {
            String value = JOptionPane.showInputDialog(context.getFrame(), label);
            if (value == null)
                return null;
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(context.getFrame(), label + " must be a whole number.");
            }
        }
    }

    /**
     * Prompt for a whole number that must be at least {@code min}: re-asks on
     * bad or too-small input, returns null if cancelled.
     *
     * @param label the label for the input field
     * @param min   the smallest value accepted
     * @return the user's input, or null if cancelled
     */
    protected Integer promptInt(String label, int min) {
        while (true) {
            Integer value = promptInt(label);
            if (value == null)
                return null;
            if (value >= min)
                return value;
            JOptionPane.showMessageDialog(context.getFrame(), label + " must be at least " + min + ".");
        }
    }

    /**
     * Prompt for a decimal number: re-asks on bad input, returns null if cancelled.
     *
     * @param label the label for the input field
     * @return the user's input, or null if cancelled
     */
    protected Double promptDouble(String label) {
        while (true) {
            String value = JOptionPane.showInputDialog(context.getFrame(), label);
            if (value == null)
                return null;
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(context.getFrame(), label + " must be a number.");
            }
        }
    }

    /**
     * Displays text in the output area.
     *
     * @param text the text to display
     */
    protected void display(String text) {
        output.setText(text);
    }

    /**
     * Clears the output area.
     */
    protected void clearOutput() {
        output.setText("");
    }
}
