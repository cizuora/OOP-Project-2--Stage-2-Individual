import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Screen and actions available to a warehouse manager.
 * 
 * @author Aj Johnson, Aaditya Malla, Chidera Izuora
 * @version 1.0
 */
public class ManagerState extends WarehouseState {
    private static ManagerState instance;
    private final Warehouse warehouse = Warehouse.getInstance();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ManagerState() {
        super();
    }

    /**
     * Returns the singleton instance of ManagerState.
     * 
     * @return the singleton instance of ManagerState
     */
    public static ManagerState getInstance() {
        if (instance == null) {
            instance = new ManagerState();
        }
        return instance;
    }

    /**
     * Prompts for and adds a product to the catalog.
     * 
     * @return the ID of the added product
     */
    public void addProduct() {
        String name = promptRequired("Enter product name:");
        if (name == null) {
            display("Add product cancelled.");
            return;
        }
        Double price = promptDouble("Enter product price:");
        if (price == null) {
            display("Add product cancelled.");
            return;
        }
        Integer stock = promptInt("Enter initial stock:", 0);
        if (stock == null) {
            display("Add product cancelled.");
            return;
        }
        String id = warehouse.addProduct(name, price, stock);
        StringBuilder sb = new StringBuilder();
        sb.append("Product added:\nID: ").append(id)
                .append(String.format("%nName: %s%nPrice: $%.2f%nStock: %d", name, price, stock));
        display(sb.toString());
    }

    /**
     * Prompts for a product and displays its waitlist.
     * 
     * @return the waitlist of the product
     */
    public void displayWaitlist() {
        String productID = promptRequired("Enter product ID:");
        if (productID == null)
            return;
        List<WaitlistView> waitlist = warehouse.getProductWaitlistView(productID);
        if (waitlist == null) {
            display("Product not found.");
            return;
        }
        if (waitlist.isEmpty()) {
            display("Waitlist is empty.");
            return;
        }
        StringBuilder sb = new StringBuilder("Waitlist for product ID ").append(productID).append(":\n");
        for (WaitlistView item : waitlist) {
            sb.append(String.format("Client=%s, Name=%s, Price=$%.2f, WaitQty=%d, Stock=%d%n",
                    item.getClientID(), item.getProductName(), item.getPrice(),
                    item.getWaitingQuantity(), item.getStock()));
        }
        display(sb.toString());
    }

    /**
     * Prompts for shipment lines and submits the shipment.
     * 
     * @return the report of the shipment
     */
    public void receiveShipment() {
        List<ShipmentLine> lines = new ArrayList<>();

        display("Enter shipment lines (product ID and quantity). Press Cancel on the product ID to finish.");
        while (true) {
            String productID = promptRequired("Enter product ID (Cancel to finish):");
            if (productID == null)
                break;

            Integer qty = promptInt("Quantity received:", 1);
            if (qty == null)
                break;
            if (qty <= 0) {
                display("Quantity must be positive.");
                continue;
            }
            lines.add(new ShipmentLine(productID, qty));
        }

        if (lines.isEmpty()) {
            display("No shipment lines entered.");
            return;
        }

        String report = warehouse.receiveShipment(lines);
        display(report);
    }

    /**
     * Switches to the clerk role. Uses become() so that logging out of the
     * clerk returns to this manager screen instead of the login screen.
     */
    public void becomeClerk() {
        context.become(WarehouseContext.CLERK, null);
    }

    /**
     * Logs out of the manager state.
     */
    public void exit() {
        context.logout();
        display("Exiting Manager State.");
    }

    /**
     * Prepares the frame for the manager state.
     */
    @Override
    public void run() {
        JFrame frame = prepareFrame();
        frame.setTitle("Warehouse - Manager");
        frame.getContentPane().setLayout(new BorderLayout()); // buttons on top, output below

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(new AddProductButton());
        buttons.add(new DisplayWaitlistButton());
        buttons.add(new ReceiveShipmentButton());
        buttons.add(new BecomeClerkButton());
        buttons.add(new LogoutButton());

        output.setEditable(false);
        frame.getContentPane().add(buttons, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(output), BorderLayout.CENTER);
        showFrame(frame);
    }

    /**
     * Prepares the frame for the manager state.
     *
     * @return the prepared JFrame
     */
    private class AddProductButton extends JButton implements ActionListener {
        AddProductButton() {
            super("Add Product");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addProduct();
        }
    }

    /**
     * Button to display the waitlist for a product.
     */
    private class DisplayWaitlistButton extends JButton implements ActionListener {
        DisplayWaitlistButton() {
            super("Display Waitlist");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            displayWaitlist();
        }
    }

    /**
     * Button to receive a shipment.
     */
    private class ReceiveShipmentButton extends JButton implements ActionListener {
        ReceiveShipmentButton() {
            super("Receive Shipment");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            receiveShipment();
        }
    }

    /**
     * Button to switch to the clerk role.
     */
    private class BecomeClerkButton extends JButton implements ActionListener {
        BecomeClerkButton() {
            super("Become Clerk");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            becomeClerk();
        }
    }

    /**
     * Button to log out and return to the login state.
     */
    private class LogoutButton extends JButton implements ActionListener {
        LogoutButton() {
            super("Logout");
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            exit();
        }
    }
}
